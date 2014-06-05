/* *********************************************************************** *
 * project: org.matsim.*
 * LivingQuarterType.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.southafrica.population.census2001.containers;

public class LivingQuarterType2001 {
	private enum Type{House, Hotel, StudentResidence, OldAgeHome, Hostel, Other};
//	HOUSE("House", 1), 
//	HOTEL("Hotel", 2), 
//	STUDENT_RESIDENCE("StrudentResidence", 3),
//	OLD_AGE_HOME("OldAgeHome", 4), 
//	HOSTEL("Hostel", 5),
//	OTHER("Other", 6);
	
	private final String stringDescription;
	private final int code;
	
	private LivingQuarterType2001(String description, int code) {
		this.stringDescription = description;
		this.code = code;
	}

	public static Type getLivingQuarterType(int code){
		switch (code) {
		case 1:
			return Type.House;
		case 2:
			return Type.Hotel;
		case 3:
			return Type.StudentResidence;
		case 4: 
			return Type.OldAgeHome;
		case 5:
			return Type.Hostel;
		default:
			break;
		}
		return Type.Other;
	}
	
	public static int getCode(String description){
		if(description.equalsIgnoreCase("House")){
			return 1;
		} else if(description.equalsIgnoreCase("Hotel")){
			return 2;
		} else if(description.equalsIgnoreCase("StudentResidence")){
			return 3;
		} else if(description.equalsIgnoreCase("OldAgeHome")){
			return 4;
		} else if(description.equalsIgnoreCase("Hostel")){
			return 5;
		} else{
			return 6;
		}
	}
	
	public static String getDescription(Type type){
		switch (type) {
		case House:
			return "House";
		case Hotel:
			return "Hotel";
		case StudentResidence:
			return "StudentResidence";
		case OldAgeHome:
			return "OldAgeHome";
		case Hostel:
			return "Hostel";
		case Other:
			return "Other";
		default:
			return null;
		}
	}
	
}

