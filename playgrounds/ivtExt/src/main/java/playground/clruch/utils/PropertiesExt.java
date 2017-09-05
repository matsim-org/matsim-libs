package playground.clruch.utils;

import java.util.Properties;

import playground.clruch.data.LocationSpec;
import playground.clruch.data.ReferenceFrame;

/** @author Claudio Ruch */
// TODO make sure only PropertiesExt visible in Server, Viewer, Preparer.
public class PropertiesExt {

    private final Properties properties;

    public static PropertiesExt wrap(Properties properties) {
        return new PropertiesExt(properties);

    }

    private PropertiesExt(Properties properties) {
        this.properties = properties;
    }

    public ReferenceFrame getReferenceFrame() {
        return ReferenceFrame.fromString(properties.getProperty("ReferenceFrame"));
    }

    public LocationSpec getLocationSpec() {
        return LocationSpec.fromString(properties.getProperty("LocationSpec"));

    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(properties.getProperty(key));
    }

    public int getInt(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

}
