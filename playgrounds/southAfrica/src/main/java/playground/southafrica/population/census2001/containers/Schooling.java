/* *********************************************************************** *
 * project: org.matsim.*
 * Schooling.java
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

public class Schooling {
	private enum Type{None, PreSchool, School, Tertiary, AdultEducation, Unknown};
	
	public static Type getSchool(int code){
		switch (code) {
		case 1:
			return Type.None;
		case 2:
			return Type.PreSchool;
		case 3:
			return Type.School;
		case 6:
			return Type.Tertiary;
		case 7:
			return Type.AdultEducation;
		case 8:
			return Type.Unknown;
		default:
			break;
		}
		return null;
	}
	
	
	public static int getCode(String description){
		if(description.equalsIgnoreCase("None")){
			return 1;
		} else if(description.equalsIgnoreCase("PreSchool")){
			return 2;
		} else if(description.equalsIgnoreCase("School")){
			return 3;
		} else if(description.equalsIgnoreCase("Tertiary")){
			return 6;
		} else if(description.equalsIgnoreCase("AdultEducation")){
			return 7;
		}
		return 8;
	}
	
	
	public static String getDescription(Type schoolType){
		switch (schoolType) {
		case None:
			return "None";
		case PreSchool:
			return "PreSchool";
		case School:
			return "School";
		case Tertiary:
			return "Tertiary";
		case AdultEducation:
			return "AdultEducation";
		case Unknown:
			return "Unknown";
		default:
			break;
		}
		return "";
	}


}

