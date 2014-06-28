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

package playground.southafrica.projects.treasury2014;

import playground.southafrica.utilities.Header;

/** 
 * Class to automate the merging of the persons and commercial vehicle
 * subpopulations. 
 *
 * @author jwjoubert
 */
public class MergePeopleAndFreight {

	public static void main(String[] args) {
		Header.printHeader(MergePeopleAndFreight.class.toString(), args);
		run(args);
		Header.printFooter();
	}
	
	public static void run(String[] args){
		/* TODO Must still be implemented. I think this is only useful once 
		 * full activity chains has been assigned to the persons as well. */
	}

}
