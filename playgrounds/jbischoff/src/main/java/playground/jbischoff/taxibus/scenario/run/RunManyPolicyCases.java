/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.run;



/**
 * @author  jbischoff
 *
 */
public class RunManyPolicyCases {

	public static void main(String[] args) {

		String[] runs = {"configPC85","configPC86"};
		for (int i = 0; i<runs.length;i++){
			String[] file = {"C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/configs/"+runs[i]+".xml"};
//			String[] file = {"D:/runs-svn/vw_rufbus/delievery/20160121/runs/"+runs[i]+"/"+runs[i]+".output_events.xml.gz"};
			RunTaxibusPolicyCase.main(file);
		}
		
	}

}
