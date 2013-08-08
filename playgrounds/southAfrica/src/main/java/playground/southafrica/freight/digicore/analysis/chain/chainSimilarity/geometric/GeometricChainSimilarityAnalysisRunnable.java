/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package playground.southafrica.freight.digicore.analysis.chain.chainSimilarity.geometric;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.io.DigicoreVehicleReader_v1;

public class GeometricChainSimilarityAnalysisRunnable implements Runnable {
	private final File vehicleFile;
	private final File outputFolder;
	private final Counter counter;
	
	public GeometricChainSimilarityAnalysisRunnable(File vehicleFile, File outputFolder, Counter counter) {
		this.vehicleFile = vehicleFile;
		this.outputFolder = outputFolder;
		this.counter = counter;
	}
	
	@Override
	public void run() {
		/* Read the vehicle. */
		DigicoreVehicleReader_v1 dvr = new DigicoreVehicleReader_v1();
		dvr.parse(this.vehicleFile.getAbsolutePath());
		List<DigicoreChain> chains = dvr.getVehicle().getChains();
		
		String outputFile = this.outputFolder.getAbsolutePath() + 
				"/" + dvr.getVehicle().getId().toString() + ".csv";
		BufferedWriter bw = IOUtils.getAppendingBufferedWriter(outputFile);
		try{
			bw.write("Chain,Chain,Overlap");
			bw.newLine();
			
			/* Analyse each chain combination. */
			for(int i = 0; i < chains.size()-1; i++){
				for(int j = i+1; j < chains.size(); j++){
					DigicoreChain chain1 = chains.get(i);
					DigicoreChain chain2 = chains.get(j);
					
					/*TODO The buffer is hard coded as 100m. */
					double overlap = GeometricChainSimilarityAnalyser.getPercentageOverlap(chain1, chain2, 100);
					
					/* Write output. */
					bw.write(String.format("%d,%d,%.6f\n", i, j, overlap));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFile);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFile);
			}
		}
		
		counter.incCounter();
	}

}
