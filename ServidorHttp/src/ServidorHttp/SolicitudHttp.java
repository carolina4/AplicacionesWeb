package ServidorHttp;

import java.net.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author b16744
 */
public class SolicitudHttp {
    private Socket clienteVisitante;
    
    public SolicitudHttp(Socket clienteVisitante) throws Exception {
        this.clienteVisitante = clienteVisitante;       
    }
    
    public void process() throws Exception {
        BufferedReader in = new BufferedReader(new InputStreamReader(clienteVisitante.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clienteVisitante.getOutputStream()));
        DataOutputStream dout = new DataOutputStream(clienteVisitante.getOutputStream());
         
        String[] solicitud = leerHeaderSolicitud(in); 
        // Para que el programa no prouzca errores con solicitudes vacias
        if (solicitud.length == 0) {
            dout.close();
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
        String datosPOST = "";
        
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
        if (accion.equals("POST")) {
            int i = 0;
            while(in.ready())
            {
                i = in.read();
                datosPOST += (char)i;
            }
        }
        
        // Se inserta la solicitud a la bitacora
        if (accion.equals("POST")) {
            actualizarBitacora(accion, nombreArchivo, datosPOST, servidor, refiere);
        } else {
            actualizarBitacora(accion, nombreArchivo, datosGET, servidor, refiere);
        }     
        
        // Se revisa por error 400
        if (servidor.equals("")) {
            // No se incluyo el cabezado host, ocurre un error 400
            String contenidoSolicitud = "<html><body>Error 400: Bad Request.</body></html>";
            out.write(respuestaHttp(400, "Bad Request", "text/html", contenidoSolicitud, true));
            
            dout.close();
            out.close();
            in.close();
            clienteVisitante.close();
            return;
        }
        
        // Si no se declaro un archivo se usa el archivo por defecto
        if (nombreArchivo.equals("/")) {
            nombreArchivo = "/index.html";
        }
        
        // La carpeta www es donde se guardan los archivos que el servidor devuelve al cliete
        File archivo = new File("www" + nombreArchivo);
        int tamanoArchivo = (int)archivo.length();
        
        if(archivo.exists()) {
            DiccionarioMimeTypes diccionario = new DiccionarioMimeTypes();    
            String mimeType = diccionario.obtenerMimeType(nombreArchivo);
            
            if (revisarMimeType(mimeType, acepta)) {
                // El mimetype del archivo corresponde a un mimetype aceptado
                switch (accion) {
                    case "GET":
                        GET(archivo, dout, mimeType, datosGET);
                        break;

                    case "HEAD":
                        out.write(headerHttp(200, "OK", mimeType, tamanoArchivo));
                        break;

                    case "POST":
                        POST(archivo, dout, mimeType, datosPOST);
                        break;
                        
                    default:
                        // El servidor no entiende la solicitud, ocurre un error 501
                        String contenidoSolicitud = "<html><body>Error 501: Not Implemented.</body></html>";
                        out.write(respuestaHttp(501, "Not Implemented", "text/html", contenidoSolicitud, true));
                }
            } else {
                // El mimetype del archivo no corresponde a un mimetype aceptado, ocurre un error 406
                String contenidoSolicitud = "<html><body>Error 406: Not Acceptable.</body></html>";
                out.write(respuestaHttp(406, "Not Acceptable", "text/html", contenidoSolicitud, true));
            }
        } else {
            // El archivo no existe, ocurre un error 404
            String contenidoSolicitud = "<html><body>Error 404: Not Found.</body></html>";
            out.write(respuestaHttp(404, "Not Found", "text/html", contenidoSolicitud, true));
        }           
        
        dout.close();
        out.close();
        in.close();
        clienteVisitante.close();
        
    }
    
    private void GET (File archivo, DataOutputStream dout, String mimeType, String datosGET) throws FileNotFoundException, IOException, InterruptedException {
        int dot = archivo.getName().lastIndexOf(".");
        
        if (archivo.getName().substring(dot + 1).equals("php")) {
            // Si el archivo es PHP hay que preprocesarlo
            String contenido = procesarPHP(archivo, datosGET);
            
            String header = headerHttp(200, "OK", mimeType, contenido.length());
            dout.write(header.getBytes(Charset.forName("UTF-8")));
            dout.write(contenido.getBytes(Charset.forName("UTF-8")));
        } else {
            byte contenidoArchivo[] = new byte[(int)archivo.length()];

            FileInputStream fin = new FileInputStream(archivo);
            fin.read(contenidoArchivo);
            fin.close();

            String header = headerHttp(200, "OK", mimeType, (int)archivo.length());
            dout.write(header.getBytes(Charset.forName("UTF-8")));
            dout.write(contenidoArchivo);
        }
    }
	
    private void POST (File archivo, DataOutputStream dout, String mimeType, String datosPOST) throws FileNotFoundException, IOException, InterruptedException {        
        int dot = archivo.getName().lastIndexOf(".");
        
        if (archivo.getName().substring(dot + 1).equals("php")) {
            // Si el archivo es PHP hay que preprocesarlo
            String contenido = procesarPHP(archivo, datosPOST);
            
            String header = headerHttp(200, "OK", mimeType, contenido.length());
            dout.write(header.getBytes(Charset.forName("UTF-8")));
            dout.write(contenido.getBytes(Charset.forName("UTF-8")));
        } else {
            String [] variables = datosPOST.split("&");
            String contenidoArchivo = "<html><body>";
            for (int i = 0; i < variables.length; ++i) {
                contenidoArchivo += "<p>";
                contenidoArchivo += variables[i];
                contenidoArchivo += "</p>";
            }
            contenidoArchivo += "</body></html>";

            String header = headerHttp(200, "OK", mimeType, (int)archivo.length());
            dout.write(header.getBytes(Charset.forName("UTF-8")));
            dout.write(contenidoArchivo.getBytes(Charset.forName("UTF-8")));
        }
  
    }
    
    // Devuelve un string con una respuesta HTTP completa con un contenido que es texto
    private String respuestaHttp (int codigo, String nombreCodigo, String tipoContenido, String contenido, boolean incluyeContenido) {
        String respuesta = "HTTP/1.0 " + codigo + " " + nombreCodigo + "\r\n";
        
        DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        Date date = new Date();
        respuesta += "Date: " + dateFormat.format(date) + "\r\n";
        
        respuesta += "Server: My Server/0.1\r\n";    
        respuesta += "Content-Type: " + tipoContenido + "\r\n";   
        respuesta += "Content-Length: " + contenido.length() + "\r\n\r\n";
        
        if (incluyeContenido) {
            respuesta += contenido;
        }
        
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
    private String archivoAString(FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();     
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
        }
        
        return contenido.toString();
    }
    
    // Lee y convierte el header de la solicitud a un arreglo de Strings
    public String[] leerHeaderSolicitud(BufferedReader in) throws IOException {
        List<String> lineas = new ArrayList<String>();
        String linea = null;
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
    
    // Obtiene el mime type genereal a partir de un mimeType, por ejemplo: text/html => text/*
    private String obtenerMimeTypeGeneral(String mimeType) {
        String mimeTypeGeneral = mimeType.substring(0, mimeType.indexOf("/"));
        mimeTypeGeneral += "/*";
        return mimeTypeGeneral;
    }
    
    // Actualiza el archivo de la bitacora de solicitudes
    private void actualizarBitacora(String accion, String nombreArchivo, String datos, String servidor, String refiere) {
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        Date date = new Date();
        String estampillaTiempo = dateFormat.format(date);

        System.out.println("Metodo: " + accion + " Hora: " + estampillaTiempo + " Servidor: " + servidor + " Refiere: " + refiere + " URL: " + nombreArchivo + " Datos: " + datos);
        
        // FALTA
    }
    
    // Procesa un archivo PHP de forma que regrese el contenido de la pagina preprocesado
    private String procesarPHP (File archivo, String datos) throws IOException, InterruptedException {
        // Se hace una equivalencia entre las variables de get y post
        BufferedReader br = new BufferedReader(new FileReader(archivo));
        String primeraLinea = br.readLine().trim();
        if (!primeraLinea.equals("<?php $_POST = $_GET; ?>") ) {
            agregarLineaAlInicioArchivo(archivo, "<?php $_POST = $_GET; ?>\n");
        }
        br.close();
        
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

        input.close();
        
        return resultado;
    }
    
    // Agrega el contenido de un string al inicio de un archivo
    private void agregarLineaAlInicioArchivo(File archivo, String linea) throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(archivo, "rw");
        byte[] text = new byte[(int) raf.length()];
        raf.readFully(text);
        raf.seek(0);
        raf.writeBytes(linea);
        raf.write(text);
        raf.close();
    }
}
