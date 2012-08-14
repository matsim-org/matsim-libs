/* *********************************************************************** *
 * project: org.matsim.*
 * DgOldCbModel2Ks2010
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
package playground.dgrether.signalsystems.cottbus.scripts;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgOldCbModel2Ks2010 {

	public static void main(String[] args) throws IOException {
		int cellsX = 5;
		int cellsY = 5;
		double boundingBoxOffset = 2000.0;
		double startTime = 6.0 * 3600.0;
		double endTime = 9.0 * 3600.0;
	//private static double startTime = 13.5 * 3600.0;
	//private static double endTime = 18.5 * 3600.0;
		double matsimPopSampleSize = 1.0;
		double ksModelCommoditySampleSize = 1.0;
		final String outputDirectory = DgPaths.REPOS + "shared-svn/projects/cottbus/cb2ks2010/cottbus_originaldaten_0600_0900/";

		String studiesdg = DgPaths.REPOS + "shared-svn/studies/dgrether/";
		String networkFilename = studiesdg + "cottbus/originaldaten/network.xml";
		String lanesFilename = studiesdg + "cottbus/originaldaten/lanedefinitionsCottbus_v2.0.xml";
		String signalSystemsFilename = studiesdg +  "cottbus/originaldaten/signalSystemsCottbus_v2.0.xml";
		String signalGroupsFilename = studiesdg + "cottbus/originaldaten/signalGroupsCottbus_v2.0.xml";
		String signalControlFilename = studiesdg + "cottbus/originaldaten/signalControlCottbusT90_v2.0.xml";
		//TODO change to run1712 when finished
		String populationFilename = studiesdg + "cottbus/originaldaten/plans.xml";
//		String populationFile = DgPaths.REPOS + "runs-svn/run1292/1292.output_plans.xml.gz";
		Scenario fullScenario = DgMATSimScenario2KoehlerStrehler2010.loadScenario(networkFilename, populationFilename, lanesFilename, signalSystemsFilename, signalGroupsFilename, signalControlFilename);
		
		String name = "cottbus originaldaten plans between 06:00 and 09:00";

		DgMATSimScenario2KoehlerStrehler2010 matsim2KsConverter = new DgMATSimScenario2KoehlerStrehler2010(fullScenario);
		matsim2KsConverter.setMatsimPopSampleSize(matsimPopSampleSize);
		matsim2KsConverter.setKsModelCommoditySampleSize(ksModelCommoditySampleSize);
		matsim2KsConverter.convert(outputDirectory, name, boundingBoxOffset, cellsX, cellsY, startTime, endTime);

	}

}
