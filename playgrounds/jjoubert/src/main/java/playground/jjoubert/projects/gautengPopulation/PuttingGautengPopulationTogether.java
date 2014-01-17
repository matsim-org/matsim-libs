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

/**
 * 
 */
package playground.jjoubert.projects.gautengPopulation;

import playground.southafrica.utilities.Header;

/**
 * 
 * @author jwjoubert
 */
public class PuttingGautengPopulationTogether {

	/**
	 * Make sure that there are two files for the commercial vehicles in the 
	 * output folder specified in the arguments:<br><br>
	 * <code>com.xml.gz</code> containing the desired population of commercial
	 * 		vehicles; and <br>
	 * <code>comAttr.xml.gz</code> containing the attributes of the commercial
	 * 		vehicle population.<br><br>
	 * 
	 * This means you have to first run the {@link AddGautengIntraAttribute} 
	 * class and move the output to the relevant folder.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(PuttingGautengPopulationTogether.class.toString(), args);
		
		String outputFolder = args[0];

		/* First convert all the old Sanral population components. */
		SanralPopulationConverter.Run("/Users/jwjoubert/Documents/workspace/data-sanral2010/plans/car_plans_2009_10pctV0.xml.gz",
				"car", "car", 0.1, 
				outputFolder + "car.xml.gz",
				outputFolder + "carAttr.xml.gz");
		SanralPopulationConverter.Run("/Users/jwjoubert/Documents/workspace/data-sanral2010/plans/bus_plans_2009_10pctV0.xml.gz",
				"bus", "bus", 0.1, 
				outputFolder + "bus.xml.gz",
				outputFolder + "busAttr.xml.gz");
		SanralPopulationConverter.Run("/Users/jwjoubert/Documents/workspace/data-sanral2010/plans/taxi_plans_2009_10pctV0.xml.gz",
				"taxi", "taxi", 0.1, 
				outputFolder + "taxi.xml.gz",
				outputFolder + "taxiAttr.xml.gz");
		SanralPopulationConverter.Run("/Users/jwjoubert/Documents/workspace/data-sanral2010/plans/ext_plans_2011_10pctV0.xml.gz",
				"ext", "ext", 0.1, 
				outputFolder + "ext.xml.gz",
				outputFolder + "extAttr.xml.gz");
		
		/* Now combine the subpopulations. */
		JoinSubpopulations.Run( 
				/* Car and commercial vehicles... */
				outputFolder + "car.xml.gz", outputFolder + "carAttr.xml.gz", 
				outputFolder + "com.xml.gz", outputFolder + "comAttr.xml.gz", 
				outputFolder + "tmp1.xml.gz", outputFolder + "tmp1Attr.xml.gz");
		JoinSubpopulations.Run(
				/* ... add bus... */
				outputFolder + "tmp1.xml.gz", outputFolder + "tmp1Attr.xml.gz", 
				outputFolder + "bus.xml.gz", outputFolder + "busAttr.xml.gz", 
				outputFolder + "tmp2.xml.gz", outputFolder + "tmp2Attr.xml.gz");
		JoinSubpopulations.Run(
				/* ... add taxi... */
				outputFolder + "tmp2.xml.gz", outputFolder + "tmp2Attr.xml.gz", 
				outputFolder + "taxi.xml.gz", outputFolder + "taxiAttr.xml.gz", 
				outputFolder + "tmp3.xml.gz", outputFolder + "tmp3Attr.xml.gz");
		JoinSubpopulations.Run(
				/* ... add external traffic... */
				outputFolder + "tmp3.xml.gz", outputFolder + "tmp3Attr.xml.gz", 
				outputFolder + "ext.xml.gz", outputFolder + "extAttr.xml.gz", 
				outputFolder + "gauteng.xml.gz", outputFolder + "gautengAttr.xml.gz");
		
		/* Finally, add the vehicle class and eTag penetration. */
		AssignTollAttributes.Run(
				outputFolder + "gauteng.xml.gz", 
				outputFolder + "gautengAttr.xml.gz",
				outputFolder + "gautengAttr2.xml.gz");
		
		Header.printFooter();
	}

}
