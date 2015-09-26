/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorHttp;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kalam
 */
public class Bitacora {
    FileWriter bitacora = null;
    PrintWriter pw = null;
    
    Bitacora(){
        try {
            this.bitacora = new FileWriter("www/bitacora.txt",true);
            pw = new PrintWriter(bitacora);
         
        } catch (IOException ex) {
            Logger.getLogger(Bitacora.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.pw = new PrintWriter(this.bitacora);
    }
    
    public void actualizarBitacora(String accion, String nombreArchivo, String datos, String servidor, String refiere) {
        DateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
        Date date = new Date();
        String estampillaTiempo = dateFormat.format(date);
        try{
            this.pw.println("Metodo: " + accion + " Hora: " + estampillaTiempo + " Servidor: " + servidor + " Refiere: " + refiere + " URL: " + nombreArchivo + " Datos: " + datos);
       
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
           try {
                if (null != this.bitacora)
                   this.bitacora.close();
                } catch (Exception e2) {
                   e2.printStackTrace();
                }
        }

    }
    
}
