/* *********************************************************************** *
 * project: org.matsim.*
 * Race.java
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

public class Race {
	private enum Type{Black, Coloured, IndianAsian, White, Other, Unknown};
	
	public static Type getRace(int code){
		switch (code) {
		case 1:
			return Type.Black;
		case 2:
			return Type.Coloured;
		case 3:
			return Type.IndianAsian;
		case 4:
			return Type.White;
		case 5:
			return Type.Other;
		default:
			break;
		}
		return Type.Unknown;
	}

	
	public static int getCode(String description){
		if(description.equalsIgnoreCase("Black")){
			return 1;
		} else if(description.equalsIgnoreCase("Coloured")){
			return 2;
		} else if(description.equalsIgnoreCase("Indian-Asian")){
			return 3;
		}else if(description.equalsIgnoreCase("White")){
			return 4;
		}else if(description.equalsIgnoreCase("Other")){
			return 5;
		} else{
			return 6;			
		}
	}
	
	
	public static String getDescription(Type race){
		switch (race) {
		case Black:
			return "Black";
		case Coloured:
			return "Coloured";
		case IndianAsian:
			return "Indian-Asian";
		case White:
			return "White";
		case Other:
			return "Other";
		default:
			break;
		}
		return "Unknown";
	}
}

