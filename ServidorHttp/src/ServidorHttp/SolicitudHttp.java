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
            return;
        }
        
        StringTokenizer tokenizer = new StringTokenizer(solicitud[0]);
        System.out.println(solicitud[0]);
        
        String accion = tokenizer.nextToken();
        String nombreArchivo = tokenizer.nextToken().substring(1);
        
        // Si no se declaro un archivo, se usa el archivo por defecto
        if (nombreArchivo.equals("")) {
            nombreArchivo = "index.html";
        }
        
        File archivo = new File(nombreArchivo);
        int tamanoArchivo = (int)archivo.length();
        
        if(archivo.exists()) {
            DiccionarioMimeTypes diccionario = new DiccionarioMimeTypes();    
            String mimeType = diccionario.obtenerMimeType(nombreArchivo);
            
            if (revisarMimeType(mimeType, solicitud)) {
                // El mimetype del archivo corresponde a un mimetype aceptado
                switch (accion) {
                    case "GET":
                        System.out.println("Realizando un GET");
                        GET(archivo, dout, mimeType);
                        break;

                    case "HEAD":
                        System.out.println("Realizando un HEAD");
                        out.write(headerHttp(200, "OK", mimeType, tamanoArchivo));
                        break;

                    case "POST":
                        System.out.println("Realizando un POST");
                        // FALTA
                        break;
                        
                    default:
                        // El servidor no entiende la solicitud, ocurre un error 400
                        String contenidoSolicitud = "<html><body>Error 400: Bad Request.</body></html>";
                        out.write(respuestaHttp(400, "Bad Request", "text/html", contenidoSolicitud, true));
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
                            String mimeTypeGeneral = obtenerMimeTypeGeneral(mimeType);
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
    
    // Obtiene el mime type genereal a partir de un mimeType, por ejemplo: text/html => text/*
    private String obtenerMimeTypeGeneral(String mimeType) {
        String mimeTypeGeneral = mimeType.substring(0, mimeType.indexOf("/"));
        mimeTypeGeneral += "/*";
        return mimeTypeGeneral;
    }
    
}
