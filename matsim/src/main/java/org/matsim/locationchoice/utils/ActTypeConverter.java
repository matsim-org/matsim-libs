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
}
