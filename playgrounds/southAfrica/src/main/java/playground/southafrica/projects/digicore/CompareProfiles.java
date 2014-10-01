/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.projects.digicore;

import playground.southafrica.utilities.Header;

/**
 * Class to read a base profile, and compare another (individual) profile
 * against it.
 * 
 * @author jwjoubert
 */
public class CompareProfiles {

	public static void main(String[] args) {
		Header.printHeader(CompareProfiles.class.toString(), args);
		String base = args[0];
		String compare = args[1];
		
		
		Header.printFooter();
	}

}
