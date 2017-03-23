package playground.joel.helpers;

import java.text.DecimalFormat;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by Joel on 18.03.2017.
 */
public class KeyMap {
    NavigableMap<Double, String> keyMap = new TreeMap<>();

    double binSize;

    //equalize length of all key elements
    public DecimalFormat keyForm = new DecimalFormat("#000000");

    public KeyMap(double size) {this.binSize = size;}

    public void initialize() {
        // initialize keyMap
        double binStart = 0;
        while (binStart < 108000) {
            keyMap.put(binStart, writeKey(binStart));
            binStart += binSize;
        }

    }

    public double getBinStart(double time) {
        return keyMap.floorKey(time);
    }

    String writeKey(double binStart) {
        String key = String.valueOf(keyForm.format(binStart)) + " - " + String.valueOf(keyForm.format(binStart + binSize));
        return key;
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
