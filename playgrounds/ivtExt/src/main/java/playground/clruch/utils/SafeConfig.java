// code by jph
package playground.clruch.utils;

import org.matsim.core.config.ReflectiveConfigGroup;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public class SafeConfig {
    final ReflectiveConfigGroup reflectiveConfigGroup;

    private SafeConfig(ReflectiveConfigGroup reflectiveConfigGroup) {
        this.reflectiveConfigGroup = reflectiveConfigGroup;
    }

    public int getInteger(String key, int alt) {
        try {
            String string = reflectiveConfigGroup.getParams().get(key);
            if (string != null)
                return Integer.parseInt(string);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return alt;
    }

    public double getDouble(String key, double alt) {
        try {
            String string = reflectiveConfigGroup.getParams().get(key);
            if (string != null)
                return Double.parseDouble(string);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return alt;
    }

    public int getIntegerStrict(String key) {
        String string = reflectiveConfigGroup.getParams().get(key);
        GlobalAssert.that(string != null);
        return Integer.parseInt(string);
    }

    public double getDoubleStrict(String key) {
        String string = reflectiveConfigGroup.getParams().get(key);
        GlobalAssert.that(string != null);
        return Double.parseDouble(string);
    }
    
    public boolean getBoolStrict(String key) {
        String string = reflectiveConfigGroup.getParams().get(key);
        GlobalAssert.that(string != null);
        return Boolean.parseBoolean(string);
    }
    
    public String getStringStrict(String key) {
        String string = reflectiveConfigGroup.getParams().get(key);
        GlobalAssert.that(string != null);
        return string;
    }

    public static SafeConfig wrap(ReflectiveConfigGroup reflectiveConfigGroup) {
        return new SafeConfig(reflectiveConfigGroup);
    }
}
