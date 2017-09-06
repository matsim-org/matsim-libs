package playground.joel.helpers;

import java.util.NavigableMap;

/**
 * Created by Joel on 18.03.2017.
 */
public class BinnedHelper {

    public static void checkFull(NavigableMap<String, Double> map, double binSize, KeyMap keyMap) {
        double binStart = 0;
        boolean full = true;
        while (binStart < 108000) {
            if (!map.containsKey(keyMap.getKey(binStart))) full = false;
            binStart += binSize;
        }
        if (!full) System.out.println("not all time bins contain values\n" +
                "\t- created bins are not continuous\n" +
                "\t- increasing binSize could resolve this");
    }

    public static void putAdd(NavigableMap<String, Double> map, String key, double value) {
        double currValue = 0;
        if (map.containsKey(key)) currValue = map.get(key);
        map.put(key, currValue + value);
    }

}
