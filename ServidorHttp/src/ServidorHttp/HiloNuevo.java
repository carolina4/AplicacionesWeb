package ServidorHttp;

import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Carolina y Kalam
 */
public class HiloNuevo implements Runnable{

    protected Socket socketCliente = null;

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