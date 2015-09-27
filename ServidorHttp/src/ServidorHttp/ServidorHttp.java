package ServidorHttp;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Carolina y Kalam
 */
public class ServidorHttp {

    public static void main(String[] args) throws Exception {
        ServerSocket socket = new ServerSocket(5217);
        
        while(true) {
            // El socket espera a solicitudes de clientes
            Socket socketCliente = socket.accept();

            // Se crea un nuevo hilo para atender la solicitud
            new Thread(new HiloNuevo(socketCliente)).start();
        }   
    }
    
}