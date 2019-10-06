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
		
		
		String folder = "D:\\Thiel\\Programme\\WVModell\\01_MatSimInput\\vw274_cadytsOn_NoMultiWork_Mutat36000.1\\";
		String run = "vw274_cadytsOn_NoMultiWork_Mutat36000.1";
				
		RunTraveltimeValidationExample.main(new String[]{folder+run+".output_plans.xml.gz",folder+run+".output_events.xml.gz",folder+run+".output_network.xml.gz","EPSG:25832","MTIHglK7a71g2jG6GopJ","5dBURxwsfwPPcxgNhSAGbQ",folder,"2019-05-14","5000"});
	}
}
