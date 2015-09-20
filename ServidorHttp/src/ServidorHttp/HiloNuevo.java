/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ServidorHttp;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author b16744
 */
public class HiloNuevo implements Runnable{

    protected Socket socketCliente = null;
    protected String serverText   = null;

    public HiloNuevo(Socket clientSocket) {
        this.socketCliente = clientSocket;
    }

    public void run() {
        try {
            SolicitudHttp request = new SolicitudHttp(socketCliente);
            request.process();
        } catch (Exception ex) {
            Logger.getLogger(HiloNuevo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}