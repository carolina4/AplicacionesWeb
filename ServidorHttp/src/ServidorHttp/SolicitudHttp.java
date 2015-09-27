package ServidorHttp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Carolina y Kalam
 */
public class SolicitudHttp {
    private final Socket clienteVisitante;
    private final Bitacora bitacora;
    private final DiccionarioMimeTypes diccionario;
    
    public SolicitudHttp(Socket clienteVisitante) throws Exception {
        this.clienteVisitante = clienteVisitante;  
        bitacora = new Bitacora();
        diccionario = new DiccionarioMimeTypes();
    }
    
    public void process() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(clienteVisitante.getInputStream()));
        DataOutputStream out = new DataOutputStream(clienteVisitante.getOutputStream());
        
        String[] solicitud = leerHeaderSolicitud(in);
        
        // Para que el programa no produzca errores con solicitudes vacias
        if (solicitud.length == 0) {
            out.close();
            in.close();
            clienteVisitante.close();
            return;
        }
        
        // Se analiza la primera parte de la solicitud
        StringTokenizer tokenizer = new StringTokenizer(solicitud[0]);     
        String accion = tokenizer.nextToken();
        String nombreArchivo = tokenizer.nextToken();
        String datosGET = "";
        
        int fin = nombreArchivo.indexOf("?");
        if (fin != -1) {
            datosGET = nombreArchivo.substring(fin+1);
            nombreArchivo = nombreArchivo.substring(0,fin);
        }
        
        // Se analizan las siguientes partes de la solicitud
        String servidor = "";
        String refiere = "";
        String acepta = "";
        
        for (int i = 0; i < solicitud.length; ++i) {
            if (!solicitud[i].equals("")) {
                String[] partesSolicitud = solicitud[i].split(" ");
                
                if (partesSolicitud[0].equals("Host:")) {
                    servidor = partesSolicitud[1];
                }
                if (partesSolicitud[0].equals("Referer:")) {
                    refiere = partesSolicitud[1];
                }
                if (partesSolicitud[0].equals("Accept:")) {
                    acepta = partesSolicitud[1];
                }
            }
        }
        
        // Se obtiene el contenido de la solicitud si es un POST
        String datosPOST = "";
        if (accion.equals("POST")) {
            int i;
            while(in.ready()) {
                i = in.read();
                datosPOST += (char)i;
            }
        }
        
        // Se inserta la solicitud a la bitacora      
        if (accion.equals("POST")) {        
            bitacora.actualizarBitacora(accion, nombreArchivo, datosPOST, servidor, refiere);
        } else {        
            bitacora.actualizarBitacora(accion, nombreArchivo, datosGET, servidor, refiere);
        }     
        
        // Se revisa por error 400
        if (servidor.equals("")) {
            // No se incluyo el cabezado host, ocurre un error 400
            String contenidoSolicitud = "<html><body>Error 400: Bad Request.</body></html>";
            out.write(respuestaHttp(400, "Bad Request", "text/html", contenidoSolicitud).getBytes(Charset.forName("UTF-8")));
            
            out.close();
            in.close();
            clienteVisitante.close();
            return;
        }
        
        // Si no se declaro un archivo se usa el archivo por defecto
        if (nombreArchivo.equals("/")) {
            nombreArchivo = "/index.html";
        }
        
        // La carpeta www es donde se guardan los archivos que el servidor devuelve al cliente
        File archivo = new File("www" + nombreArchivo);
        
        if(archivo.exists()) {
            String mimeType = diccionario.obtenerMimeType(nombreArchivo);
            
            if (revisarMimeType(mimeType, acepta)) {
                // El mimetype del archivo corresponde a un mimetype aceptado
                switch (accion) {
                    case "GET":
                        GET(archivo, out, mimeType, datosGET);
                        break;
                        
                    case "HEAD":
                        HEAD(archivo, out, mimeType);
                        break;

                    case "POST":
                        POST(archivo, out, mimeType, datosPOST);
                        break;
                        
                    default:
                        // El servidor no entiende la solicitud, ocurre un error 501
                        String contenidoSolicitud = "<html><body>Error 501: Not Implemented.</body></html>";
                        out.write(respuestaHttp(501, "Not Implemented", "text/html", contenidoSolicitud).getBytes(Charset.forName("UTF-8")));
                }
            } else {
                // El mimetype del archivo no corresponde a un mimetype aceptado, ocurre un error 406
                String contenidoSolicitud = "<html><body>Error 406: Not Acceptable.</body></html>";
                out.write(respuestaHttp(406, "Not Acceptable", "text/html", contenidoSolicitud).getBytes(Charset.forName("UTF-8")));
            }
        } else {
            // El archivo no existe, ocurre un error 404
            String contenidoSolicitud = "<html><body>Error 404: Not Found.</body></html>";
            out.write(respuestaHttp(404, "Not Found", "text/html", contenidoSolicitud).getBytes(Charset.forName("UTF-8")));
        }           
        
        out.close();
        in.close();
        clienteVisitante.close();    
    }
    
    private void GET (File archivo, DataOutputStream dout, String mimeType, String datosGET) throws FileNotFoundException, IOException, InterruptedException {
        int punto = archivo.getName().lastIndexOf(".");
        
        // Se revisa la extension del archivo
        if (archivo.getName().substring(punto + 1).equals("php")) {
            // Si el archivo es PHP hay que preprocesarlo
            
            String respuesta = respuestaHttp(200, "OK", mimeType, procesarPHP(archivo, datosGET));
            dout.write(respuesta.getBytes(Charset.forName("UTF-8")));
        } else {
            // Si el archivo no es PHP se lee y se envia como bytes
            
            byte contenidoArchivo[] = new byte[(int)archivo.length()];

            FileInputStream fin = new FileInputStream(archivo);
            fin.read(contenidoArchivo);
            fin.close();

            String header = headerHttp(200, "OK", mimeType, (int)archivo.length());
            dout.write(header.getBytes(Charset.forName("UTF-8")));
            dout.write(contenidoArchivo);
           
        }
    }
    
    private void HEAD(File archivo, DataOutputStream dout, String mimeType) throws IOException{
        String header = headerHttp(200, "OK", mimeType, (int)archivo.length());
        dout.write(header.getBytes(Charset.forName("UTF-8")));
    }
    
    private void POST (File archivo, DataOutputStream dout, String mimeType, String datosPOST) throws FileNotFoundException, IOException, InterruptedException {        
        int dot = archivo.getName().lastIndexOf(".");
        
        // Se revisa la extension del archivo
        if (archivo.getName().substring(dot + 1).equals("php")) {
            // Si el archivo es PHP hay que preprocesarlo
            
            // Se crea un archivo temporal para el procesado
            File temp = File.createTempFile("tmp", ".php", null);
            BufferedWriter output = new BufferedWriter(new FileWriter(temp));
            output.write("<?php $_POST = $_GET; ?>\n");
            output.write(archivoAString(archivo));
            output.close();
            
            String respuesta = respuestaHttp(200, "OK", mimeType, procesarPHP(temp, datosPOST));
            dout.write(respuesta.getBytes(Charset.forName("UTF-8")));
              
            temp.deleteOnExit();
        } else {
            // Si no es un archivo PHP se devuelve una pagina con los datos del contenido del request
            
            String [] variables = datosPOST.split("&");
            String contenidoArchivo = "<html><body>";
            for (int i = 0; i < variables.length; ++i) {
                contenidoArchivo += "<p>";
                contenidoArchivo += variables[i];
                contenidoArchivo += "</p>";
            }
            contenidoArchivo += "</body></html>";

            String respuesta = respuestaHttp(200, "OK", mimeType, contenidoArchivo);
            dout.write(respuesta.getBytes(Charset.forName("UTF-8")));
        }
    }
    
    // Devuelve un string con una respuesta HTTP completa con un contenido que es texto
    private String respuestaHttp (int codigo, String nombreCodigo, String tipoContenido, String contenido) {
        String respuesta = headerHttp (codigo, nombreCodigo, tipoContenido, contenido.length());
        respuesta += contenido;
        return respuesta;
    }
    
    // Devuelve un string con un header para una respuesta HTTP con los valores indicados
    private String headerHttp (int codigo, String nombreCodigo, String tipoContenido, int tamanoContenido) {
        String respuesta = "HTTP/1.0 " + codigo + " " + nombreCodigo + "\r\n";
        
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        Date date = new Date();
        respuesta += "Date: " + dateFormat.format(date) + "\r\n";
        
        respuesta += "Server: My Server/0.1\r\n";    
        respuesta += "Content-Type: " + tipoContenido + "\r\n";   
        respuesta += "Content-Length: " + tamanoContenido + "\r\n\r\n";
        
        return respuesta;
    }
    
    // Convierte el contenido de un archivo a un String
    private String archivoAString (File archivo) throws FileNotFoundException, IOException {
        FileInputStream fin = new FileInputStream(archivo);
        String contenido = streamAString(fin);
        fin.close();
        return contenido;
    }
    
    // Convierte el contenido de un stream a un String
    private String streamAString(FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();     
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
        }
        
        return contenido.toString();
    }
    
    // Lee y convierte el header de una solicitud a un arreglo de Strings
    public String[] leerHeaderSolicitud(BufferedReader in) throws IOException {
        List<String> lineas = new ArrayList<>();
        String linea;
        while ((linea = in.readLine()) != null) {
            lineas.add(linea.trim());
            if (linea.isEmpty()) {
                break;
            }
        }
        return lineas.toArray(new String[lineas.size()]);
    }
    
    // Revisa si el mimetype de un archivo corresponde a algun mimetype aceptado
    private boolean revisarMimeType(String mimeType, String acepta){
        boolean resultado = false;
        if (!acepta.equals("")) {
            String[] mimeTypesAceptados = acepta.split(" |,");
                
            // Se revisan los mimetypes aceptados para ver si aceptan al mimetype
            for (int i = 0; i < mimeTypesAceptados.length; ++i) {
                // Se eliminan caracteres no necesarios
                int fin = mimeTypesAceptados[i].indexOf(";");
                if (fin != -1) {
                    mimeTypesAceptados[i] = mimeTypesAceptados[i].substring(0,fin);
                }
                        
                if (mimeTypesAceptados[i].equals("*/*") || mimeTypesAceptados[i].equals(mimeType)) {
                    resultado = true;
                    break;
                } else {
                    String mimeTypeGeneral = obtenerMimeTypeGeneral(mimeType);
                    if (mimeTypesAceptados[i].equals(mimeTypeGeneral)) {
                        resultado = true;
                        break;
                    }
                }                         
            }
        }
        return resultado;
    }
    
    // Obtiene el Mime Type genereal a partir de un Mime Type, por ejemplo: text/html => text/*
    private String obtenerMimeTypeGeneral(String mimeType) {
        String mimeTypeGeneral = mimeType.substring(0, mimeType.indexOf("/"));
        mimeTypeGeneral += "/*";
        return mimeTypeGeneral;
    }
    
    // Procesa un archivo PHP de forma que regrese el contenido de la pagina preprocesado
    private String procesarPHP (File archivo, String datos) throws IOException, InterruptedException {
        // Se crea una instruccion php-cgi con sus parametros
        String [] variables = datos.split("&");  
        String instruccion = "php-cgi -f " + archivo.getPath();     
        for (int i = 0; i<variables.length; ++i) {
            instruccion += " ";
            instruccion += variables[i];
        }
        
        // Se ejecuta la instruccion php-cgi
        Process p = Runtime.getRuntime().exec(instruccion);
        p.waitFor();
        
        // Se leen los posibles errores
        String linea;
        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while((linea = error.readLine()) != null){
            System.out.println(linea);
        }
        error.close();

        // Se crea un resultado con la respuesta de php
        String resultado = "";
        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while((linea=input.readLine()) != null){
            resultado += linea;
        }

        error.close();
        input.close();
        return resultado;
    }

}
