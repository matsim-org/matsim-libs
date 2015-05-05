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

package playground.southafrica.projects.digicore.scoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jzy3d.maths.Coord3d;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.grid.DigiGrid;
import playground.southafrica.projects.digicore.grid.DigiGrid.Visual;
import playground.southafrica.utilities.Header;

public class SpeedAndAccelDigiScorer implements DigiScorer{
	private final static Logger LOG = Logger.getLogger(SpeedAndAccelDigiScorer.class);
	private final SpeedDigiScorer delegate;
	
	/* Other variables. */
	private int maxLines = Integer.MAX_VALUE;
	private int noSpeedLimitWarningCount = 0;
	private int noRiskWarningCount = 0;
	private double[] speedObservations = {0, 0, 0, 0};
	private final double SPEED_ZERO = 1.0;
	private final double SPEED_ONE = 1.1;
	private final double SPEED_TWO = 1.2;
	private double weight_speed = 0.5;
	private double weight_accel = 0.5;


	public SpeedAndAccelDigiScorer(final double scale, String filename, final List<Double> riskThresholds, Visual visual) {
		this.delegate = new SpeedDigiScorer(scale, filename, riskThresholds, visual);
	}

	@Override
	public void buildScoringModel(String filename) {
		LOG.info("Populating the dodecahedra with point observations...");
		if(this.maxLines < Integer.MAX_VALUE){
			LOG.warn("A limited number of " + this.maxLines + " is processed (if there are so many)");
		}

		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double z = Double.parseDouble(sa[7]);
				double speed = Double.parseDouble(sa[8]);
				double speedLimit = Double.parseDouble(sa[11]);

				/* Warn if no speed limit exists. */
				if(speedLimit == 0 && noSpeedLimitWarningCount < 10){
					if(noSpeedLimitWarningCount < 10){
						LOG.warn("No speed limit for the record: " + line);
						noSpeedLimitWarningCount++;
					}
					if(noSpeedLimitWarningCount == 10){
						LOG.warn("Future occurences of this warning will be suppressed");
					}
				}

				/* Major priority rule is speed. But, contrary to SpeedDigiScorer,
				 * NOT only non-speeding records are added to the blob. The 
				 * weights are equally shared between the speed risk and the
				 * accelerometer risk. 
				 * 
				 * (5 Nov 2014) It turns out to be that whatever the weight, it
				 * does not influence the weighting, because the overall weighting
				 * just goes down. Changed it back to 1.  
				 */
				/* Put data conditions here. */
				if(
						//						id.equalsIgnoreCase("37ff9d8e04c164ee793e172a561c7b1e") &	/* Specific individual, A. */
						//						id.equalsIgnoreCase("9a01080c086096aaaaff7504a01ea9e3") &	/* Specific individual, B. */
						//						id.equalsIgnoreCase("0ae0c60759b410c2c38fa0ba135a8e16") &	/* Specific individual, C. */
						//						road <= 2 & 												/* Road is a highway */
						//						speed <= 60.0 &												/* Low speed */
						//						speed > 60.0 &												/* High speed */
						true){

					/* Apply the speed rules according to which records are scored. */
					double speeding = speed / speedLimit;
					int speedIndex;
					if(speeding <= SPEED_ZERO || speedLimit == 0.0){ 
						/* The latter condition will result in a NaN value for 
						 * speeding, which messes up points addition to the grid,
						 * as such points are omitted. Rather we add them to the
						 * grid as we cannot actually consider them as 
						 * speeding per se. */
						speedIndex = 0;
					} else if(speeding < SPEED_ONE){
						speedIndex = 1;
					} else if(speeding < SPEED_TWO){
						speedIndex = 2;
					} else{
						speedIndex = 3;
					}
					double oldCount = speedObservations[speedIndex];
					speedObservations[speedIndex] = oldCount + 1.0;

					Coord3d c = delegate.getGrid().getClosest(x, y, z);
					delegate.getGrid().incrementCount(c, 1.0);
				}
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
		LOG.info("All " + counter.getCounter() + " points processed.");

		delegate.getGrid().rankGridCells();
	}

	
	public double getRiskGroupValue(String record){
		double result = 0.0;
		
		String[] sa = record.split(",");
		
		//FIXME Remove after debugging
		String id = sa[1];

		/* Consider speed as first risk component. */
		double speed = Double.parseDouble(sa[8]);
		double speedLimit = Double.parseDouble(sa[11]);
		double speeding = speed / speedLimit;

		/* Because of the data quality, we need to consider zero speed limits.
		 * When building the risk model, zero speed limits is associated with no 
		 * risk speed. */
		RISK_GROUP speedRisk = null;
		double speedRiskValue = 0.0;
		if(speeding <= SPEED_ZERO || speedLimit == 0.0){
			speedRisk = RISK_GROUP.NONE;
			speedRiskValue = 0.0;
		} else if(speeding < SPEED_ONE){
			speedRisk = RISK_GROUP.LOW;	
			speedRiskValue = 1.0;
		} else if(speeding < SPEED_TWO){
			speedRisk = RISK_GROUP.MEDIUM;
			speedRiskValue = 2.0;
		} else{
			speedRisk = RISK_GROUP.HIGH;
			speedRiskValue = 3.0;
		}

		RISK_GROUP accelRisk = null;
		double accelRiskValue = 0.0;
		/* Return accelerometer risk class. */
		double x = Double.parseDouble(sa[5]);
		double y = Double.parseDouble(sa[6]);
		double z = Double.parseDouble(sa[7]);

		/* Get the closest cell to this point. */
		Coord3d cell = delegate.getGrid().getClosest(x, y, z);
		int risk = delegate.getGrid().getCellRisk(cell);
		switch (risk) {
		case 0:
			accelRisk = RISK_GROUP.NONE;
			accelRiskValue = 0.0;
			break;
		case 1:
			accelRisk = RISK_GROUP.LOW;
			accelRiskValue = 1.0;
			break;
		case 2:
			accelRisk = RISK_GROUP.MEDIUM;
			accelRiskValue = 2.0;
			break;
		case 3:
			accelRisk = RISK_GROUP.HIGH;
			accelRiskValue = 3.0;
			break;
		default:
			throw new RuntimeException("Don't know what risk class " + risk + " is!");
		}

//		/* Now return the lower (riskier) of the two components. */ 
//		RISK_GROUP result = null;
//		switch (speedRisk) {
//		case NONE:
//			result = accelRisk;
//			break;
//		case LOW:
//			switch (accelRisk) {
//			case NONE:
//			case LOW:
//				result = speedRisk;
//				break;
//			case MEDIUM:			
//			case HIGH:			
//			default:
//				result = accelRisk;
//			}
//			break;
//		case MEDIUM:			
//			switch (accelRisk) {
//			case NONE:
//			case LOW:
//			case MEDIUM:			
//				result = speedRisk;
//				break;
//			case HIGH:			
//			default:
//				result = accelRisk;
//			}
//			break;
//		case HIGH:			
//			switch (accelRisk) {
//			case NONE:
//			case LOW:
//			case MEDIUM:			
//			case HIGH:			
//				result = speedRisk;
//				break;
//			default:
//				result = accelRisk;
//			}
//		}
//		
		result = weight_speed*speedRiskValue + weight_accel*accelRiskValue;
		return result;
	}
	
	
	/**
	 * An accelerometer record is given two risk groups: one based on speed and
	 * another based on the accelerometer data. The overall risk group is the
	 * lower (more risky) of the two components. 
	 */
	@Override
	public RISK_GROUP getRiskGroup(String record) {
		/* Check that the 'blob'  has already been created and populated. */
		if(!delegate.getGrid().isRanked()){
			LOG.error("You cannot get a risk group unless the risk evaluation has been done.");
			LOG.error("First call the method 'buildScoringModel(...)");
			throw new RuntimeException();
		}
		
		/* Quantify the risk. */ 
		double riskValue = getRiskGroupValue(record);
		
		if(riskValue <= 0.75){
			return RISK_GROUP.NONE;
		} else if(riskValue <= 1.5){
			return RISK_GROUP.LOW;
		} else if (riskValue <= 2.25){
			return RISK_GROUP.MEDIUM;
		} else{
			return RISK_GROUP.HIGH;
		}
	}

	/**
	 * Consider each record, and process them per individual so that the total
	 * number of occurrences in each risk group can be calculated. The output 
	 * file with name <code>riskClassCountsPerPerson.csv</code> will be created 
	 * in the output folder.
	 * 
	 * @param outputFolder
	 */
	@Override
	public void rateIndividuals(String filename, String outputFolder){
		Map<String, Integer[]> personMap = new TreeMap<String, Integer[]>();

		/* Process all records. */
		LOG.info("Processing records for person-specific scoring...");
		LOG.info("Weights used:");
		LOG.info("  \\_ acceleration: " + this.getAccelWeight());
		LOG.info("  \\_ speed: " + this.getSpeedWeight());
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");

				/*TODO Give this some more thought (in future). I think we 
				 * need not, in this case, where we look at both speed and 
				 * acceleration, just call the single RISK_GROUP. Rather, 
				 * calculate it 50/50 as it was done with building the profile. */
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
	public DigiGrid getGrid(){
		return delegate.getGrid();
	}

	@Override
	public void setGrid(DigiGrid grid) {
		delegate.setGrid(grid);
	}

	public void setMaximumLines(int maxLines){
		this.maxLines = maxLines;
	}
	
	public double getSpeedWeight(){
		return this.weight_speed;
	}
	
	public void setSpeedWeight(double weight){
		this.weight_speed = weight;
	}
	
	public double getAccelWeight(){
		return this.weight_accel;
	}
	
	public void setAccelWeight(double weight){
		this.weight_accel = weight;
	}
	
	/**
	 * Writes the speed observation results: the number of records in each risk
	 * class. The output file with name <code>speedClassCounts.csv</code> will
	 * be created in the output folder.
	 * 
	 * @param outputFolder
	 */
	public void writeSpeedCounts(String outputFolder){
		String filename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "speedClassCounts.csv";
		LOG.info("Writing the speed class counts to " + filename); 

		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			/* Header. */
			bw.write("speedClass,count");
			bw.newLine();

			for(int i = 0; i < this.speedObservations.length; i++){
				bw.write(String.format("%d,%.1f\n", i, this.speedObservations[i]));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing the speed class counts."); 
	}



	public static void main(String[] args) {
		Header.printHeader(SpeedAndAccelDigiScorer.class.toString(), args);

		/* Parse the input arguments. */
		String filename = args[0];
		String outputFolder = args[1];
		Double scale = Double.parseDouble(args[2]);
		int maxLines = Integer.parseInt(args[3]);
		Visual visual = Visual.valueOf(args[4]);
		double aWeight = Double.parseDouble(args[5]);
		double sWeight = Double.parseDouble(args[6]);

		List<Double> riskThresholds = new ArrayList<Double>();
		int argsIndex = 7;
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

		SpeedAndAccelDigiScorer sds = new SpeedAndAccelDigiScorer(scale, filename, riskThresholds, visual);
		sds.setMaximumLines(maxLines);
		sds.setAccelWeight(aWeight);
		sds.setSpeedWeight(sWeight);
		sds.buildScoringModel(filename);
		sds.getGrid().writeCellCountsAndRiskClasses(outputFolder);
		sds.writeSpeedCounts(outputFolder);
		sds.rateIndividuals(filename, outputFolder);

		Header.printFooter();
		sds.getGrid().visualiseGrid();
	}
}
