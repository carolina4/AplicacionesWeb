package ServidorHttp;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author b16744
 */
public class ServidorHttp {

    public static void main(String[] args) throws Exception {
        ServerSocket socket = new ServerSocket(5217);
        while(true) {
            Socket socketCliente = socket.accept();

            // Se crea un nuevo hilo para atender a la solicitud
            new Thread(new HiloNuevo(socketCliente)).start();
        }   
    }
    
}