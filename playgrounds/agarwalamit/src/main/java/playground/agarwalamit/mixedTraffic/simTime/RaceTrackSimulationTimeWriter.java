/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.simTime;

import java.io.PrintStream;

import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.gbl.MatsimRandom;

import playground.agarwalamit.mixedTraffic.FDTestSetUp.GenerateFundamentalDiagramData;
import playground.agarwalamit.mixedTraffic.FDTestSetUp.InputsForFDTestSetUp;

/**
 * @author amit
 */

public class RaceTrackSimulationTimeWriter {

	private final int [] randomNumbers = {4711, 6835, 1847, 4144, 4628, 2632, 5982, 3218, 5736, 7573,4389, 1344} ;
	private static String outputFolder = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run312/carBike/computationalEfficiency/";
	private static PrintStream writer;

	public static void main(String[] args) {

		boolean isUsingCluster = false;
		if (args.length != 0) isUsingCluster = true;

		if ( isUsingCluster ) {
			outputFolder = args[0];
		} 

		RaceTrackSimulationTimeWriter rtstw = new RaceTrackSimulationTimeWriter();

		try {
			writer = new PrintStream(outputFolder+"/simTime.txt");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}

		writer.print("scenario \t simTimeInSec \n");

		for (LinkDynamics ld : LinkDynamics.values() ) {
			for ( TrafficDynamics td : TrafficDynamics.values()){
				writer.print(ld+"_"+td+"\t");
				rtstw.processAndWriteSimulationTime(ld, td);
				writer.println();
			}
		}
		try {
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}
	}

	private void processAndWriteSimulationTime ( QSimConfigGroup.LinkDynamics ld, QSimConfigGroup.TrafficDynamics td ){

		InputsForFDTestSetUp inputs = new InputsForFDTestSetUp();
		inputs.setLinkDynamics(ld);
		inputs.setTrafficDynamics(td);
		inputs.setTravelModes(new String [] {"car","bike"});
		inputs.setModalSplit(new String [] {"1.0","1.0"});
		
		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData(inputs);
		generateFDData.setRunDirectory(outputFolder+"/output_simTime/");
		generateFDData.setIsDumpingInputFiles(false);
		generateFDData.setIsWritingEventsFileForEachIteration(false);

		for (int i = 0; i<randomNumbers.length;i++) {
			MatsimRandom.reset(randomNumbers[i]);
			double startTime = System.currentTimeMillis();
			generateFDData.run();
			double endTime = System.currentTimeMillis();

			if(i>1 ) { // avoid two initial runs
				writer.print( String.valueOf( (endTime - startTime)/1000 ) + "\t");
			}
		}
	}
}


