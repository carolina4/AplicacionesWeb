/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorHttp;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

/**
 *
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
         
        String solicitud = in.readLine();
        
        /*String solicitudCompleta;
        while ((solicitudCompleta = in.readLine()) != null) {
            System.out.println(solicitudCompleta);
            if (solicitudCompleta.isEmpty()) {
                break;
            }
        }*/
        
        solicitud = solicitud.trim();
        
        System.out.println(solicitud);
        
        StringTokenizer st = new StringTokenizer(solicitud);
        
        String accion = st.nextToken();
        String archivo = st.nextToken();
        
        if (archivo.equals("/")) {
            archivo = "index.html";
        }
        
        FileInputStream fin = null;
        boolean fileExist = true;
        try {
            fin = new FileInputStream(archivo);
        }
        catch(Exception ex) {
            fileExist = false;
            fin = new FileInputStream("archivo404.html");
        }
        
        if(fileExist) {
            switch (accion) {
                case "GET":
                    System.out.println("Realizando un GET");
                    GET(out, fin);
                    break;

                case "POST":
                    System.out.println("Realizando un POST");
                    break;

                case "HEAD":
                    System.out.println("Realizando un HEAD");
                    break;

                default:
                    System.out.println("406");
                    break;
            }               
        } else {
            error404(out, fin);
        }           
        
        fin.close();
        out.close();
        in.close();
        clienteVisitante.close();
        
    }
    
    private void GET (BufferedWriter out, FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();
        
        byte[] buffer = new byte[1024] ;
        int length = 0;
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
            ++length;
        }
        
        
        out.write("HTTP/1.0 200 OK\r\n");
        out.write("Date: Fri, 31 Dec 2000 23:59:59 GMT\r\n");
        out.write("Server: Apache/0.8.4\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("Content-Length: " + length + "\r\n");
        out.write("Expires: Sat, 01 Jan 2001 00:59:59 GMT\r\n");
        out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
        out.write("\r\n");
        out.write(contenido.toString());  
    }
    
    private void error404 (BufferedWriter out, FileInputStream fin) throws IOException {
        StringBuilder contenido = new StringBuilder();
        
        byte[] buffer = new byte[1024] ;
        int length = 0;
        int ch;
        while((ch = fin.read()) != -1){
            contenido.append((char)ch);
            ++length;
        }
        
        out.write("HTTP/1.0 404 NOT FOUND\r\n");
        out.write("Date: Fri, 31 Dec 2000 23:59:59 GMT\r\n");
        out.write("Server: Apache/0.8.4\r\n");
        out.write("Content-Type: text/html\r\n");
        out.write("Content-Length: " + length + "\r\n");
        out.write("Expires: Sat, 01 Jan 2001 00:59:59 GMT\r\n");
        out.write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n");
        out.write("\r\n");
        out.write(contenido.toString());
    }
}
