package ServidorHttp;

import java.net.*;
import java.io.*;
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
         

        String[] solicitud = archivoAArreglo(in); 
        // Para que el programa no prouzca errores con solicitudes vacias
        if (solicitud.length == 0) {
            return;
        }
        
        StringTokenizer tokenizer = new StringTokenizer(solicitud[0]);
        System.out.println(solicitud[0]);
        
        String accion = tokenizer.nextToken();
        String archivo = tokenizer.nextToken().substring(1);
        String paramVariables = archivo.substring(archivo.indexOf("?")+1,archivo.length());
        String [] variables = paramVariables.split("&");

        switch(archivo){
            
            // Si no se declaro un archivo, se usa el archivo por defecto
            case "":
            archivo = "index.html";
            break;
            case "registro.html":
            archivo = "registro.html";
            break;          
        }
        

        String contenidoSolicitud = "";
        
        FileInputStream fin;
        boolean archivoExiste = true;

        try {
            fin = new FileInputStream(archivo);
            contenidoSolicitud = archivoAString(fin);
            fin.close();
        } catch(Exception ex) {
            archivoExiste = false;
        }
        

        if(archivoExiste) {
            DiccionarioMimeTypes diccionario = new DiccionarioMimeTypes();    
            String mimeType = diccionario.obtenerMimeType(archivo);
            
            if (revisarMimeType(mimeType, solicitud)) {
                // El mimetype del archivo corresponde a un mimetype aceptado
                switch (accion) {
                    case "GET":
                        System.out.println("Realizando un GET");
                        out.write(respuestaHttp(200, "OK", mimeType, contenidoSolicitud, true));
                        break;

                    case "HEAD":
                        System.out.println("Realizando un HEAD");
                        out.write(respuestaHttp(200, "OK", mimeType, contenidoSolicitud, false));
                        break;

                    case "POST":
                        System.out.println("Realizando un POST");
                        POST(out,contenidoSolicitud,variables);
                        break;
                    default:
                        // El servidor no entiende la solicitud, ocurre un error 400
                        contenidoSolicitud = "<html><body>Error 400: Bad Request.</body></html>";
                        out.write(respuestaHttp(400, "Bad Request", "text/html", contenidoSolicitud, true));
                }
            } else {
                // El mimetype del archivo no corresponde a un mimetype aceptado, ocurre un error 406
                contenidoSolicitud = "<html><body>Error 406: Not Acceptable.</body></html>";
                out.write(respuestaHttp(406, "Not Acceptable", "text/html", contenidoSolicitud, true));
            }
        } else {
            // El archivo no existe, ocurre un error 404
            contenidoSolicitud = "<html><body>Error 404: Not Found.</body></html>";
            out.write(respuestaHttp(404, "Not Found", "text/html", contenidoSolicitud, true));
        }           
        
        out.close();
        in.close();
        clienteVisitante.close();
        
    }
    
    // Devuelve un String con una respuesta HTTP
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
    
    // Convierte el contenido de un archivo a un String
    private String archivoAString(FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();     
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
        }
        
        return contenido.toString();
    }

    private void POST (BufferedWriter out, String contenido, String [] variables) throws IOException {
      
        for (String variable : variables){
        
            
            System.out.println("Variable post:"+ variable);
        
        }
        
        out.write("HTTP/1.0 200 OK\r\n");
        out.write("Date: Fri, 31 Dec 2000 23:59:59 GMT\r\n");
        out.write("Server: Apache/0.8.4\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("Content-Length: " + contenido.length() + "\r\n");
        out.write("Expires: Sat, 01 Jan 2001 00:59:59 GMT\r\n");
        out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
        out.write("\r\n");
        out.write(contenido.toString());  
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
    private boolean revisarMimeType(String mimeType, String[] solicitud){
        boolean resultado = false;
        for (int i = 0; i < solicitud.length; ++i) {
            if (!solicitud[i].equals("")) {
                String[] partesSolicitud = solicitud[i].split(" |,");
                
                // Se busca el encabezado Accept en la solicitud
                if (partesSolicitud[0].equals("Accept:")) {
                    
                    // Se revisan los mimetypes aceptados para ver si aceptan al mimetype
                    for (int j = 1; j < partesSolicitud.length; ++j) {
                        // Se eliminan caracteres no necesarios
                        int fin = partesSolicitud[j].indexOf(";");
                        if (fin != -1) {
                            partesSolicitud[j] = partesSolicitud[j].substring(0,fin);
                        }
                        
                        if (partesSolicitud[j].equals("*/*") || partesSolicitud[j].equals(mimeType)) {
                            resultado = true;
                            break;
                        } else {
                            String mimeTypeGeneral = mimeType.substring(0, mimeType.indexOf("/"));
                            mimeTypeGeneral += "/*";
                            if (partesSolicitud[j].equals(mimeTypeGeneral)) {
                                resultado = true;
                                break;
                            }
                        }                 
                    }
                    break;
                }
            }
        }
        return resultado;
    }
    

}
