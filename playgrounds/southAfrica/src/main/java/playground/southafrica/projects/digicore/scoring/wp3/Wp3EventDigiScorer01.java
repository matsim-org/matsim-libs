/* *********************************************************************** *
 * project: org.matsim.*
 * Wp3EventDigiscorer01.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import playground.southafrica.projects.digicore.grid.DigicoreEventGrid;
import playground.southafrica.projects.digicore.scoring.DIGICORE_EVENT;
import playground.southafrica.projects.digicore.scoring.DigiScorer;
import playground.southafrica.utilities.Header;

/**
 * Class to generate a risk space from merely events provided by Digicore.
 * 
 * @author jwjoubert
 */
public class Wp3EventDigiScorer01 implements DigiScorer {
	final private static Logger LOG = Logger.getLogger(Wp3EventDigiScorer01.class);

	/* Other variables. */
	private int maxLines = Integer.MAX_VALUE;
	private int noRiskWarningCount = 0;
	
	private DigicoreEventGrid grid;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(Wp3EventDigiScorer01.class.toString(), args);
		String filename = args[0];
		String outputFolder = args[1];
		int maxLines = Integer.parseInt(args[2]);
		
		List<Double> riskThresholds = new ArrayList<Double>();
		int argsIndex = 3;
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
		
		Wp3EventDigiScorer01 wp3E = new Wp3EventDigiScorer01(riskThresholds);
		wp3E.setMaximumLines(maxLines);
		wp3E.buildScoringModel(filename);
		wp3E.rateIndividuals(filename, outputFolder);
		
		Header.printFooter();
	}

	public void setMaximumLines(int maxLines){
		this.maxLines = maxLines;
	}


	public Wp3EventDigiScorer01(List<Double> riskThresholds) {
		this.grid = new DigicoreEventGrid();
		this.grid.setRiskThresholds(riskThresholds);
	}

	@Override
	public void buildScoringModel(String filename) {
		LOG.info("Building scoring model...");
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
				
				/*TODO Actually I think there is no reason to build the risk
				 * space at all when using the events. The challenge is in the 
				 * a priori way in which events are "rated" as being no-risk,
				 * low, medium or high-risk. */
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
		counter.printCounter();
		LOG.info("Done populating the risk space.");

		this.grid.rankGridCells();
		LOG.info("Done building scoring model.");
		this.grid.reportEventCounts();
	}

	@Override
	public void rateIndividuals(String filename, String outputFolder) {
		LOG.info("Rating individuals...");
		Map<String, Integer[]> personMap = new TreeMap<String, Integer[]>();

		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null && counter.getCounter() < maxLines){
				String[] sa = line.split(",");

				String id = sa[0];
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
		LOG.info("Done writing the per-person risk class counts.");
	}

	@Override
	public RISK_GROUP getRiskGroup(String record) {
		if(!this.grid.isRanked()){
			throw new RuntimeException("Cannot rank individuals if grid has not been ranked.");
		}
		
		String[] sa = record.split(",");
		int eventId = Integer.parseInt(sa[2]);
		return DIGICORE_EVENT.getEvent(eventId).getRiskGroup();
	}
	
}
