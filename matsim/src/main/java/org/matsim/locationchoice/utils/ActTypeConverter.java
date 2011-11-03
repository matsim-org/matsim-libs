package org.matsim.locationchoice.utils;

public class ActTypeConverter {
	
	public static String convert2FullType(String type) {
		String fullType = "tta";
		if (type.startsWith("h")) {
			fullType = "home";
		}
		else if (type.startsWith("w")) {
			fullType = "work";
		}
		else if (type.startsWith("e")) {
			fullType = "education";
		}
		else if (type.startsWith("s")) {
			fullType = "shop";
		}
		else if (type.startsWith("l")) { 
			fullType = "leisure";
		}
		return fullType;
	}
	
	public static String convert2MinimalType(String type) {
		String minimalType = "tta";
		if (type.startsWith("h")) {
			minimalType = "h";
		}
		else if (type.startsWith("w")) {
			minimalType = "w";
		}
		else if (type.startsWith("e")) {
			minimalType = "e";
		}
		else if (type.startsWith("s")) {
			minimalType = "s";
		}
		else if (type.startsWith("l")) { 
			minimalType = "l";
		}
		return minimalType;
	}
}
