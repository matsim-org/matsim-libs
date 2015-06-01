/* *********************************************************************** *
 * project: org.matsim.*
 * Wp3RiskCubeDigiScorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.southafrica.projects.digicore.scoring.wp3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.grid.DigicoreRiskCubeGrid;
import playground.southafrica.projects.digicore.scoring.DigiScorer;
import playground.southafrica.utilities.Header;

/**
 * For the third work package, this class uses the discretization provided in
 * the proposal. Instead of using the risk categories associated with each of
 * the discrete speed and accelerometer classes, we use a <i>data envelope</i>
 * approach similar to the rhombic dodecahedra, i.e. the 3D blob.
 *
 * @author jwjoubert
 */
public class Wp3RiskCubeDigiScorer02 implements DigiScorer {
	final private static Logger LOG = Logger.getLogger(Wp3RiskCubeDigiScorer02.class);
	
	/* Other variables. */
	private int maxLines = Integer.MAX_VALUE;
	private int noRiskWarningCount = 0;
	private double weight_speed = 0.5;
	private double weight_accel = 0.5;
	
	/* The risk space. */
	DigicoreRiskCubeGrid grid;
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Wp3RiskCubeDigiScorer02.class.toString(), args);
		String filename = args[0];
		String outputFolder = args[1];
		int maxLines = Integer.parseInt(args[2]);
		double aWeight = Double.parseDouble(args[3]);
		double sWeight = Double.parseDouble(args[4]);

		List<Double> riskThresholds = new ArrayList<Double>();
		int argsIndex = 5;
		while(args.length > argsIndex){
			riskThresholds.add(Double.parseDouble(args[argsIndex++]));
		}

		/* Check that the output folder is empty. */
		File folder = new File(outputFolder);
		if(folder.exists() && folder.isDirectory() && folder.listFiles().length > 0){
			LOG.error("The output folder " + outputFolder + " is not empty.");
			throw new RuntimeException("Output directory will not be overwritten!!");
		}
		folder.mkdirs();
		
		Wp3RiskCubeDigiScorer02 wp3rc2 = new Wp3RiskCubeDigiScorer02(riskThresholds);
		wp3rc2.setAccelerationWeight(aWeight);
		wp3rc2.setSpeedWeight(sWeight);
		wp3rc2.setMaximumLines(maxLines);
		wp3rc2.buildScoringModel(filename);
		wp3rc2.grid.writeCellCountsAndRiskClasses(outputFolder);
		wp3rc2.rateIndividuals(filename, outputFolder);
		
		Header.printFooter();
	}

	public Wp3RiskCubeDigiScorer02(List<Double> riskThresholds) {
		this.grid = new DigicoreRiskCubeGrid(riskThresholds);
	}
	
	@Override
	public void buildScoringModel(String filename) {
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				this.grid.incrementCell(line);
				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		this.grid.setPopulated(true);
		this.grid.rankGridCells();
	}

	@Override
	public void rateIndividuals(String filename, String outputFolder) {
		Map<String, Integer[]> personMap = new TreeMap<String, Integer[]>();

		/* Process all records. */
		LOG.info("Processing records for person-specific scoring...");
		LOG.info("Weights used:");
		LOG.info("  \\_ acceleration: " + this.getAccelerationWeight());
		LOG.info("  \\_ speed: " + this.getSpeedWeight());
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");

				String id = sa[1];
				if(!personMap.containsKey(id)){
					Integer[] ia = {0, 0, 0, 0};
					personMap.put(id, ia);
				}
				Integer[] thisArray = personMap.get(id);

				RISK_GROUP risk = getRiskGroup(line);
				int index;
				switch (risk) {
				case NONE:
					index = 0;
					break;
				case LOW:
					index = 1;
					break;
				case MEDIUM:
					index = 2;
					break;
				case HIGH:
					index = 3;
					break;
				default:
					throw new RuntimeException("Don't know where to get risk values for " + risk.toString());
				}
				int oldCount = thisArray[index];
				thisArray[index] = oldCount+1;

				counter.incCounter();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + filename);
		} finally{
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		counter.printCounter();
		LOG.info("Done processing records. Unique persons identified: " + personMap.size());

		/* Write the output to file. */
		String outputFilename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "riskClassCountsPerPerson.csv";
		LOG.info("Writing the per-person risk classes counts to " + outputFilename); 

		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(outputFilename);
		try{
			/* Header. */
			bw.write("id,none,low,medium,high");
			bw.newLine();

			for(String id : personMap.keySet()){
				Integer[] thisArray = personMap.get(id);
				bw.write(String.format("%s,%d,%d,%d,%d\n", id, thisArray[0], thisArray[1], thisArray[2], thisArray[3]));

				if(thisArray[1] + thisArray[2] + thisArray[3] == 0 && noRiskWarningCount < 10){
					LOG.warn("No risk customer: " + id);
					noRiskWarningCount++;
					if(noRiskWarningCount == 10){
						LOG.warn("Future occurences of this warning will be surpressed.");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + outputFilename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + outputFilename);
			}
		}
		LOG.info("Done writing the per-person risk classes counts.");
	}

	@Override
	public RISK_GROUP getRiskGroup(String record) {
		if(!this.grid.isRanked()){
			throw new RuntimeException("Cannot rank individuals if grid has not been ranked.");
		}
		
		String speed = String.format("%02d", Wp3RiskCube.getRiskCubeSpeedBin(record));
		String accelX = String.format("%02d", Wp3RiskCube.getRiskcubeAccelXBin(record));
		String accelY = String.format("%02d", Wp3RiskCube.getRiskcubeAccelYBin(record));
		String cell = speed + "_" + accelX + "_" + accelY;
		
		int riskClass = this.grid.getRiskGroup(cell);
		switch (riskClass) {
		case 0:
			return RISK_GROUP.NONE;
		case 1:
			return RISK_GROUP.LOW;
		case 2:
			return RISK_GROUP.MEDIUM;
		case 3:
			return RISK_GROUP.HIGH;
		default:
			throw new RuntimeException("Cannot find a risk class with code " + riskClass);
		}
	}
	
	
	private void setAccelerationWeight(double weight){
		this.weight_accel = weight;
	}
	
	private double getAccelerationWeight(){
		return this.weight_accel;
	}
	
	private void setSpeedWeight(double weight){
		this.weight_speed = weight;
	}
	
	private double getSpeedWeight(){
		return this.weight_speed;
	}
	
	private void setMaximumLines(int number){
		this.maxLines = number;
	}
	


}
