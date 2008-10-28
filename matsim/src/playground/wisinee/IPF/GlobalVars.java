/* *********************************************************************** *
 * project: org.matsim.*
 * ReadEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
 * *********************************************************************** */package playground.wisinee.IPF;
import java.util.ArrayList;

public class GlobalVars {
//	input and output variables for calculation matrix
	public static double[] fixedR; 				// fixed row	
	public static double[] fixedC; 				// fixed column	
	public static double[][] initialRij;		//initial matrix	
	public static double[][] finalRij; 			//final matrix
	
	public static ArrayList<OriginalData> orgn; //to keep information from original data
	public static String[] finalData; 			//final data to write to new file
	
//	for random people to each decided group
	public static double[] totalPP;					//to keep total number of people, 
	public static double[] usedPP;						//to keep total number of people that have already assigned
	public static double[][] countUsedPP;				//
}
