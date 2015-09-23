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
         
        String[] solicitud = archivoAArreglo(in); 
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
                        GET(archivo, dout, mimeType);
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
    
    private void GET (File archivo, DataOutputStream dout, String mimeType) throws FileNotFoundException, IOException {
        byte contenidoArchivo[] = new byte[(int)archivo.length()];
        
        FileInputStream fin;
        fin = new FileInputStream(archivo);
        fin.read(contenidoArchivo);
        fin.close();
        
        String header = headerHttp(200, "OK", mimeType, (int)archivo.length());
        dout.write(header.getBytes(Charset.forName("UTF-8")));
        dout.write(contenidoArchivo);
    }
	
    private void POST (File archivo, DataOutputStream dout, String mimeType, String datosPOST) throws FileNotFoundException, IOException {    
        String [] variables = datosPOST.split("&");
        /*
        byte contenidoArchivo[] = new byte[(int)archivo.length()];
        
        FileInputStream fin;
        fin = new FileInputStream(archivo);
        fin.read(contenidoArchivo);
        fin.close();
        if (variables != null) {
            for (String variable : variables){  

                System.out.println("Variable post:"+ variable);

            }
        }
        */
        String contenidoArchivo = "<html><body>";
        for (int i = 0; i < variables.length; ++i) {
            contenidoArchivo += "<p>";
            contenidoArchivo += variables[i];
            contenidoArchivo += "</p>";
        }
        contenidoArchivo += "</body></html>";
        
        String header = headerHttp(200, "OK", mimeType, contenidoArchivo.length());
        dout.write(header.getBytes(Charset.forName("UTF-8")));
        
        dout.write(contenidoArchivo.getBytes(Charset.forName("UTF-8")));
    }
    
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
    
    // Convierte el contenido de un archivo a un arreglo de Strings por lineas
    public String[] archivoAArreglo(BufferedReader in) throws IOException {
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
    
    private void actualizarBitacora(String accion, String nombreArchivo, String datos, String servidor, String refiere) {
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        Date date = new Date();
        String estampillaTiempo = dateFormat.format(date);

        System.out.println("Metodo: " + accion + " Hora: " + estampillaTiempo + " Servidor: " + servidor + " Refiere: " + refiere + " URL: " + nombreArchivo + " Datos: " + datos);
        
        // FALTA
    }
}
