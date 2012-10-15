/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.singapore.fcl.parkopedia;

public class Main {

	public static void main(String[] args) {
		
		ConvertToCsv convertToCsv=new ConvertToCsv();
		convertToCsv.parse(args[0]);
		System.out.println(convertToCsv.numberOfParkingWithUnknownCapacity);
		System.out.println(convertToCsv.totalNumberOfParking);
		System.out.println(convertToCsv.maxPrice);
		
		
		//ParkopediaReaderV1 parkopediaReader=new ParkopediaReaderV1();
		//parkopediaReader.parse(args[0]);
		
		
	}
	
}
