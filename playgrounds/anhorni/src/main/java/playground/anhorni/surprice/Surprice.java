package playground.anhorni.surprice;

import java.util.ArrayList;
import java.util.Arrays;

public class Surprice {
	
	public static ArrayList<String> days = new ArrayList<String>(Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk"));
	public static final String SURPRICE_RUN = "surprice_run";
	public static final String SURPRICE_PREPROCESS = "surprice_preprocess";
	
	// income params
	public static double mean = 0.0;
	public static double stdDev = 1.0;
	
}
