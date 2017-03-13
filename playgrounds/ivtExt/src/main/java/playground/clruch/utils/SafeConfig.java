package playground.clruch.utils;

import org.matsim.core.config.ReflectiveConfigGroup;

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

    public static SafeConfig wrap(ReflectiveConfigGroup reflectiveConfigGroup) {
        return new SafeConfig(reflectiveConfigGroup);
    }
}
