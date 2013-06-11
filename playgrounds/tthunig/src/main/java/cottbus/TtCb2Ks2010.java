/* *********************************************************************** *
 * project: org.matsim.*
 * DgCb2Ks2010
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
package cottbus;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.dgrether.koehlerstrehlersignal.Scenario2KoehlerStrehler2010;


/**
 * @author dgrether
 * @author tthunig
 *
 */
public class TtCb2Ks2010 {

	private static String pathToSVNDirectory = "C:/Users/Atany/Desktop/SHK/SVN/";
	
	public static void main(String[] args) throws Exception {
		int cellsX = 5;
		int cellsY = 5;
		double boundingBoxOffset = 50.0;
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
	//private static double startTime = 13.5 * 3600.0;
	//private static double endTime = 18.5 * 3600.0;
		double matsimPopSampleSize = 1.0;
		double ksModelCommoditySampleSize = 0.01;
		final String outputDirectory = pathToSVNDirectory + "projects_cottbus/cb2ks2010/testlauf/";

		String networkFilename = pathToSVNDirectory + "studies_cottbus/cottbus_feb_fix/network_wgs84_utm33n.xml.gz";
		String lanesFilename = pathToSVNDirectory + "studies_cottbus/cottbus_feb_fix/lanes.xml";
		String signalSystemsFilename = pathToSVNDirectory + "studies_cottbus/cottbus_feb_fix/signal_systems.xml";
		String signalGroupsFilename = pathToSVNDirectory + "studies_cottbus/cottbus_feb_fix/signal_groups.xml";
		String signalControlFilename = pathToSVNDirectory + "studies_cottbus/cottbus_feb_fix/signal_control.xml";
		//TODO change to run1712 when finished
		String populationFilename = pathToSVNDirectory + "run1292/1292.output_plans_sample.xml";
//		String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans.xml.gz";
		Scenario fullScenario = Scenario2KoehlerStrehler2010.loadScenario(networkFilename, populationFilename, lanesFilename, signalSystemsFilename, signalGroupsFilename, signalControlFilename);
		
		String name = "run 1292 output plans between 05:30 and 09:30";

		Scenario2KoehlerStrehler2010 matsim2KsConverter = new Scenario2KoehlerStrehler2010(fullScenario,  MGC.getCRS(TransformationFactory.WGS84_UTM33N));
		matsim2KsConverter.setMatsimPopSampleSize(matsimPopSampleSize);
		matsim2KsConverter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		matsim2KsConverter.convert(outputDirectory, name, boundingBoxOffset, cellsX, cellsY, startTime, endTime);
	}

}
