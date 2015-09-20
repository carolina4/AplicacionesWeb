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
        
        // Si no se declaro un archivo, se usa el archivo por defecto
        if (archivo.equals("")) {
            archivo = "index.html";
        }
        
        // Contenido de la respuesta a la solicitud
        String contenido;
        
        FileInputStream fin;
        boolean archivoExiste = true;
        try {
            fin = new FileInputStream(archivo);
            contenido = archivoAString(fin);
            fin.close();
        }
        catch(Exception ex) {
            // Si el archivo no existe ocurre un error 404
            archivoExiste = false;
            
            archivo = "archivo404.html";
            try {
                fin = new FileInputStream(archivo);
                contenido = archivoAString(fin);
                fin.close();
            } catch(Exception ex2) {
                // Si no exite un archivo para el error 404 se usa un contenido por defecto
                contenido = "<html><body>404 Not Found.</body></html>";
            }
        }
        
        if(archivoExiste) {
            DiccionarioMimeTypes diccionario = new DiccionarioMimeTypes();    
            String mimeType = diccionario.obtenerMimeType(archivo);
            
            switch (accion) {
                case "GET":
                    System.out.println("Realizando un GET");
                    GET(out, contenido, mimeType);
                    break;

                case "HEAD":
                    System.out.println("Realizando un HEAD");
                    HEAD(out, contenido, mimeType);
                    break;
                    
                case "POST":
                    System.out.println("Realizando un POST");
                    break;
            }               
        } else {
            // El archivo no existe, ocurre un error 404
            out.write(respuestaHttp(404, "Not Found", "text/html", contenido, true));
        }           
        
        out.close();
        in.close();
        clienteVisitante.close();
        
    }
    
    private void GET (BufferedWriter out, String contenido, String mimeType) throws IOException {
        String respuesta = respuestaHttp(200, "OK", mimeType, contenido, true);      
        out.write(respuesta);
    }
    
    private void HEAD (BufferedWriter out, String contenido, String mimeType) throws IOException {
        String respuesta = respuestaHttp(200, "OK", mimeType, contenido, false);    
        out.write(respuesta);
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
    
    private String archivoAString(FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();     
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
        }
        
        return contenido.toString();
    }
    
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
    
}
