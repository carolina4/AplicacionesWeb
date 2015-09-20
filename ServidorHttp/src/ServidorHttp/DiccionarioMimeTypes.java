package ServidorHttp;

import javax.activation.MimetypesFileTypeMap;

/**
 * @author Caro
 */
public class DiccionarioMimeTypes {
    
    MimetypesFileTypeMap mapa;
    
    public DiccionarioMimeTypes() {
        mapa = new MimetypesFileTypeMap();
        
        mapa.addMimeTypes("application/xhtml+xml xhtml");
        mapa.addMimeTypes("application/x-php php PHP");
        mapa.addMimeTypes("application/pdf pdf PDF");
        mapa.addMimeTypes("image/x-ico ico ICO");
        mapa.addMimeTypes("application/javascript js JS");
        mapa.addMimeTypes("application/json json JSON");
        mapa.addMimeTypes("application/xml xml XML");
        mapa.addMimeTypes("application/zip zip ZIP");
        mapa.addMimeTypes("image/png png PNG");
    }
    
    public String obtenerMimeType(String nombreArchivo) {
        return mapa.getContentType(nombreArchivo);
    }
}
