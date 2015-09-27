package ServidorHttp;

import javax.activation.MimetypesFileTypeMap;

/**
 * @author Carolina y Kalam
 */
public class DiccionarioMimeTypes {
    
    MimetypesFileTypeMap mapa;
    
    public DiccionarioMimeTypes() {
        mapa = new MimetypesFileTypeMap();
        
        mapa.addMimeTypes("application/xhtml+xml xhtml");
        mapa.addMimeTypes("text/html php PHP");
        mapa.addMimeTypes("application/pdf pdf PDF");
        mapa.addMimeTypes("image/x-ico ico ICO");
        mapa.addMimeTypes("text/javascript js JS");
        mapa.addMimeTypes("application/json json JSON");
        mapa.addMimeTypes("application/xml xml XML");
        mapa.addMimeTypes("application/zip zip ZIP");
        mapa.addMimeTypes("image/png png PNG");
        mapa.addMimeTypes("image/svg svg SVG");
        mapa.addMimeTypes("text/css css CSS");
        mapa.addMimeTypes("text/html asp ASP");
        mapa.addMimeTypes("text/html html htm HTML");
    }
    
    // Devuelve el Mime Type de un archivo segun su extension
    public String obtenerMimeType(String nombreArchivo) {
        return mapa.getContentType(nombreArchivo);
    }
}
