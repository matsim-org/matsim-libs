package playground.clruch.utils;

import java.util.Properties;

/**
 * @author Claudio Ruch
 *
 */
// TODO make sure only PropertiesExt visible in Server, Viewer, Preparer. 
public class PropertiesExt {
    
    private final Properties properties;
    
    
    
    public static PropertiesExt wrap(Properties properties){
        return new PropertiesExt(properties);
        
        
    }
    
    private PropertiesExt(Properties properties){
        this.properties = properties;
    }
    
    // TODO fail if key undefined... 
    // 
    public boolean getBoolean(String key){
       return Boolean.valueOf(properties.getProperty(key));
    }
    
}
