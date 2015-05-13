/* *********************************************************************** *
 * project: org.matsim.													   *
 * Wp3Accel2dAndSpeedDigiScorer.java
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

/**
 * 
 */
package playground.southafrica.projects.digicore.scoring.wp3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.projects.digicore.grid.DigiGrid2D;
import playground.southafrica.projects.digicore.scoring.Accel2dAndSpeedDigiscorer;
import playground.southafrica.projects.digicore.scoring.DigiScorer;
import playground.southafrica.projects.digicore.scoring.SpeedAndAccelDigiScorer;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Point;

/**
 * Class to score the accelerometer and speed data of individuals. This class
 * is based on the original <i>Work package 1</i>'s {@link SpeedAndAccelDigiScorer}
 * with the difference that for acceleration, only x and y values are used. 
 * It also differs from </i>Work package 2</i>'s {@link Accel2dAndSpeedDigiscorer}
 * in that speed is considered a separate criteria, and not the third dimension. 
 *   
 * @author jwjoubert
 */
public class Wp3Accel2dAndSpeedDigiScorer implements DigiScorer {
	final private static Logger LOG = Logger.getLogger(Wp3Accel2dAndSpeedDigiScorer.class);

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
	
	private DigiGrid2D grid;

	
	public Wp3Accel2dAndSpeedDigiScorer(double scale, List<Double> riskThresholds) {
		this.grid = new DigiGrid2D(scale) {
			
			@Override
			public void writeCellCountsAndRiskClasses(String outputFolder) {
				if(!this.isPopulated()){
					throw new RuntimeException("Grid not populated. Nothing to write.");
				}
				if(!this.isRanked()){
					throw new RuntimeException("Grid not ranked. Nothing to write.");
				}
				String filename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv";
				LOG.info("Writing the cell values and risk classes to " + filename); 
				
				/* Report the risk thresholds for which this output holds. */
				LOG.info("  \\_ Accelerometer risk thresholds:");
				for(int i = 0; i < this.getRiskThresholds().size(); i++){
					LOG.info(String.format("      \\_ Risk %d: %.4f", i, this.getRiskThresholds().get(i)));
				}
				
				/* Write the cell values and their risk classes. */
				BufferedWriter bw = IOUtils.getBufferedWriter(filename);
				try{
					/* Header. */
					bw.write("x,y,count,class");
					bw.newLine();
					
					for(Point p : this.map.keySet()){
						bw.write(String.format("%.4f,%.4f,%.1f,%d\n", p.getX(), p.getY(), this.map.get(p), this.mapRating.get(p)));
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
				LOG.info("Done writing cell values and risk classes.");
			}
		};
		this.grid.setRiskThresholds(riskThresholds); 
	}
	
	
	@Override
	public void buildScoringModel(String filename) {
		LOG.info("Setting up grid...");
		this.grid.setupGrid(filename);

		LOG.info("Populating the risk space with point observations...");
		if(this.maxLines < Integer.MAX_VALUE){
			LOG.warn("A limited number of " + this.maxLines + " is processed (if there are so many)");
		}
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while((line = br.readLine()) != null && counter.getCounter() < this.maxLines){
				/* Parse the record. */
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
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

				/* All records are added to both the risk spaces: speed and
				 * acceleration. */
				/* Put data conditions here. */
				if(
						//						id.equalsIgnoreCase("37ff9d8e04c164ee793e172a561c7b1e") &	/* Specific individual, A. */
						//						id.equalsIgnoreCase("9a01080c086096aaaaff7504a01ea9e3") &	/* Specific individual, B. */
						//						id.equalsIgnoreCase("0ae0c60759b410c2c38fa0ba135a8e16") &	/* Specific individual, C. */
						//						road <= 2 & 												/* Road is a highway */
						//						speed <= 60.0 &												/* Low speed */
						//						speed > 60.0 &												/* High speed */
						true){

					/*-------------------------------------------------------*/
					/* Update the speed risk space.                          */
					/*-------------------------------------------------------*/
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

					/*-------------------------------------------------------*/
					/* Update the acceleration risk space.                   */
					/*-------------------------------------------------------*/
					this.grid.incrementValue(x, y, 1.0);
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
		LOG.info("Done populating the risk space.");
		
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
	
	public void setAccelerationWeight(double weight){
		this.weight_accel = weight;
	}
	
	public double getAccelerationWeight(){
		return this.weight_accel;
	}
	
	public void setSpeedWeight(double weight){
		this.weight_speed = weight;
	}
	
	public double getSpeedWeight(){
		return this.weight_speed;
	}
	
	public void setMaximumLines(int maxLines){
		this.maxLines = maxLines;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Wp3Accel2dAndSpeedDigiScorer.class.toString(), args);
		String filename = args[0];
		String outputFolder = args[1];
		Double scale = Double.parseDouble(args[2]);
		int maxLines = Integer.parseInt(args[3]);
		double aWeight = Double.parseDouble(args[4]);
		double sWeight = Double.parseDouble(args[5]);

		List<Double> riskThresholds = new ArrayList<Double>();
		int argsIndex = 6;
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
		
		Wp3Accel2dAndSpeedDigiScorer wp3AS = new Wp3Accel2dAndSpeedDigiScorer(scale, riskThresholds);
		wp3AS.setAccelerationWeight(aWeight);
		wp3AS.setSpeedWeight(sWeight);
		wp3AS.setMaximumLines(maxLines);
		wp3AS.buildScoringModel(filename);
		wp3AS.grid.writeCellCountsAndRiskClasses(outputFolder);
		wp3AS.rateIndividuals(filename, outputFolder);
		
		Header.printFooter();
	}


	/**
	 * Calculate the {@link RISK_GROUP} based on the weighted risk of both 
	 * speed and acceleration.
	 */
	@Override
	public RISK_GROUP getRiskGroup(String record) {
		/* Check that the 'blob'  has already been created and populated. */
		if(!this.grid.isRanked()){
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
	 * Calculate the weighted risk.
	 * 
	 * @param record
	 * @return
	 */
	private double getRiskGroupValue(String record){
		double result = 0.0;
		
		String[] sa = record.split(",");
		
		//FIXME Remove after debugging
//		String id = sa[1];

		/* Consider speed as first risk component. */
		double speed = Double.parseDouble(sa[8]);
		double speedLimit = Double.parseDouble(sa[11]);
		double speeding = speed / speedLimit;

		/* Because of the data quality, we need to consider zero speed limits.
		 * When building the risk model, zero speed limits is associated with no 
		 * risk speed. */
		double speedRiskValue = 0.0;
		if(speeding <= SPEED_ZERO || speedLimit == 0.0){
			speedRiskValue = 0.0;
		} else if(speeding < SPEED_ONE){
			speedRiskValue = 1.0;
		} else if(speeding < SPEED_TWO){
			speedRiskValue = 2.0;
		} else{
			speedRiskValue = 3.0;
		}

		double accelRiskValue = 0.0;
		/* Return accelerometer risk class. */
		double x = Double.parseDouble(sa[5]);
		double y = Double.parseDouble(sa[6]);

		/* Get the closest cell to this point. */
		int risk = this.grid.getCellRisk(x, y);
		switch (risk) {
		case 0:
			accelRiskValue = 0.0;
			break;
		case 1:
			accelRiskValue = 1.0;
			break;
		case 2:
			accelRiskValue = 2.0;
			break;
		case 3:
			accelRiskValue = 3.0;
			break;
		default:
			throw new RuntimeException("Don't know what risk class " + risk + " is!");
		}

		result = weight_speed*speedRiskValue + weight_accel*accelRiskValue;
		return result;
	}

}
