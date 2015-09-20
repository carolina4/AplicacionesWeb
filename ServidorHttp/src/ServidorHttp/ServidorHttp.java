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
public class ServidorHttp {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args){
        try {
            ServerSocket socket = new ServerSocket(5217);
            while(true) {
                
                Socket socketCliente = socket.accept();
                
                
                new Thread(
                    new HiloNuevo(socketCliente)
                ).start();
            }
        } catch(Exception ref){}       
    }
}