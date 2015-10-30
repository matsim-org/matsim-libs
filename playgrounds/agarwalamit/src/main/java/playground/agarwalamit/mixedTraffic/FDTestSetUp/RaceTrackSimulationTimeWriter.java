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

package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.io.PrintStream;

import org.matsim.core.gbl.MatsimRandom;

/**
 * @author amit
 */

public class RaceTrackSimulationTimeWriter {

	private final int [] randomNumbers = {4711/*, 6835, 1847, 4144, 4628, 2632, 5982, 3218, 5736, 7573,4389, 1344*/} ;
	private final String outputFolder = "../../../../repos/shared-svn/projects/mixedTraffic/triangularNetwork/run312";

	public static void main(String[] args) {


		RaceTrackSimulationTimeWriter rtstw = new RaceTrackSimulationTimeWriter();

		PrintStream writer;
		try {
			writer = new PrintStream(rtstw.outputFolder+"/simTime.txt");
			writer.print("scenario \t simTime \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason : "+e);
		}

		{
			//fifo without holes
			String data2Write = rtstw.run(false, false, false);
			try {
				writer.print("fifo_withoutHoles"+"\t"+data2Write);
				writer.println();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}

		{
			//fifo with holes
			String data2Write = rtstw.run(false, false, true);
			try {
				writer.print("fifo_withHoles"+"\t"+data2Write);
				writer.println();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}
		
		{
			//passing without holes
			String data2Write = rtstw.run(true, false, false);
			try {
				writer.print("passing_withoutHoles"+"\t"+data2Write);
				writer.println();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}
		
		{
			//passing with holes
			String data2Write = rtstw.run(true, false, true);
			try {
				writer.print("passing_withHoles"+"\t"+data2Write);
				writer.println();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}
		
		{
			//seepage without holes
			String data2Write = rtstw.run(true, true, false);
			try {
				writer.print("seepage_withoutHoles"+"\t"+data2Write);
				writer.println();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}
		
		{
			//seepage with holes
			String data2Write = rtstw.run(true, true, true);
			try {
				writer.print("seepage_withHoles"+"\t"+data2Write);
				writer.println();
				writer.close();
			} catch (Exception e) {
				throw new RuntimeException("Data is not written. Reason : "+e);
			}
		}
	}

	private String run(boolean isPassing, boolean isSeeping, boolean isUsingHoles){
		GenerateFundamentalDiagramData generateFDData = new GenerateFundamentalDiagramData();
		generateFDData.setRunDirectory(outputFolder+"/output_simTime/");
		generateFDData.setTravelModes(new String [] {"car","bike"});
		generateFDData.setModalSplit(new String [] {"1.0","1.0"}); //in pcu
		generateFDData.setIsDumpingInputFiles(false);
		generateFDData.setIsWritingEventsFileForEachIteration(false);

		generateFDData.setIsPassingAllowed(isPassing);
		generateFDData.setIsSeepageAllowed(isSeeping);
		generateFDData.setIsUsingHoles(isUsingHoles); 

		String simulationTime = "";
		for (int i = 0; i<randomNumbers.length;i++) {
			MatsimRandom.reset(randomNumbers[i]);
			double startTime = System.currentTimeMillis();
			generateFDData.run();
			double endTime = System.currentTimeMillis();

			if(i>0 ) { // avoid two initial runs
				simulationTime = simulationTime.concat( String.valueOf(endTime - startTime) + "\t");
			}
		}
		return simulationTime;
	}
}


