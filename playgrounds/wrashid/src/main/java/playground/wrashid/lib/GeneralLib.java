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

package playground.wrashid.lib;

import java.util.ArrayList;

/**
 * 
 * this class has been moved, just keeping one method here, which is being used by a different playground
 * 
 * @author rashid_waraich
 *
 */
public class GeneralLib {

	public static void writeList(ArrayList<String> list, String fileName) {
		org.matsim.contrib.parking.lib.GeneralLib.writeList(list, fileName);
	}
	
}
