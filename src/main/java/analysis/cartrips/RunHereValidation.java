/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package analysis.cartrips;
/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunHereValidation {
	public static void main(String[] args) {
		String folder = "D:/runs-svn/vw_rufbus/vw205.1.0/";
		String run = "vw205.1.0";
				
		RunTraveltimeValidationExample.main(new String[]{folder+run+".output_plans.xml.gz",folder+run+".output_events.xml.gz",folder+run+".output_network.xml.gz","EPSG:25832","nsybvUEVNq66QeKJxHbX","5HqydxSbE8NZAp1ZoS7VAg",folder,"2018-01-17","15000"});
	}
}
