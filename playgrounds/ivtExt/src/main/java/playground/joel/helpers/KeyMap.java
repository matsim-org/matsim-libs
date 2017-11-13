package playground.joel.helpers;

import java.text.DecimalFormat;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Created by Joel on 18.03.2017. */
// TODO possibly move to queuey and write tests
public class KeyMap {
    // equalize length of all key elements
    public static final DecimalFormat KEYFORM = new DecimalFormat("#000000");
    // ---
    private final NavigableMap<Double, String> keyMap = new TreeMap<>();
    private final double binSize;

    public KeyMap(double size) {
        this.binSize = size;
    }

    public void initialize() {
        // initialize keyMap
        double binStart = 0;
        while (binStart < 108000) { // TODO magic const
            keyMap.put(binStart, writeKey(binStart));
            binStart += binSize;
        }

    }

    public double getBinStart(double time) {
        return keyMap.floorKey(time);
    }

    private String writeKey(double binStart) {
        // TODO try this without String::valueOf's, should be obsolete
        return String.valueOf(KEYFORM.format(binStart)) + " - " + String.valueOf(KEYFORM.format(binStart + binSize));
    }

    public String getKey(double time) {
        return keyMap.get(keyMap.floorKey(time));
    }

    public static double keyToTime(String key) {
        // extracts the first number/bin start from a key of the form "xxxxxx - xxxxxx"
        String asd[] = key.split(" ");
        return Double.parseDouble(asd[0]);
    }

}
