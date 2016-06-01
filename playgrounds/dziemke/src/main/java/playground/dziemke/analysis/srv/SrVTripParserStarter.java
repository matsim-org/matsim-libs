/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBStops2PlansConverter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dziemke.analysis.srv;

import java.io.IOException;

/**
 * @author dziemke
 * This class is only for starting the SrVTripParser for analysis
 */
public class SrVTripParserStarter {

	public static void main(String[] args) throws IOException {
		String inputFile = "D:/Workspace/container/demand/input/srv/W2008_Berlin2.dat";
		SrV2008TripParser parser = new SrV2008TripParser();
		parser.parse(inputFile);	
	}

}
