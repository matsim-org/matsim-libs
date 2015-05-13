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

import playground.southafrica.projects.digicore.grid.DigiGrid3D.Visual;
import playground.southafrica.projects.digicore.grid.DigiGrid_XYZ;
import playground.southafrica.utilities.Header;

public class AccelOnlyDigiScorer implements DigiScorer_XYZ{
	private final static Logger LOG = Logger.getLogger(AccelOnlyDigiScorer.class);

	private DigiGrid_XYZ grid;
	private int maxLines = Integer.MAX_VALUE;


	public AccelOnlyDigiScorer(final double scale, String filename, final List<Double> riskThresholds, Visual visual) {
		this.grid = new DigiGrid_XYZ(scale);
		this.grid.setVisual(visual);
		this.grid.setRiskThresholds(riskThresholds);
		this.grid.setupGrid(filename);
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
				String id = sa[1];
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double z = Double.parseDouble(sa[7]);

				Double speed = Double.parseDouble(sa[8]);
				Integer road = Integer.parseInt(sa[9]);

				/* Put data conditions here. */
				if(
						//							id.equalsIgnoreCase("16d38cf2304c6ee23c702f4a65e5f5e0") &	/* Specific individual, A: Good */
						//							id.equalsIgnoreCase("5b71b050d8e03e618728cce5ca6a3471") &	/* Specific individual, B: Average */
						//							id.equalsIgnoreCase("d655fcbb9910c5580e030a591d6ac66d") &	/* Specific individual, C: Bad */
						//							road <= 2 & 												/* Road is a highway */
						//							speed <= 60.0 &												/* Low speed */
						//							speed > 90.0 &												/* High speed */
						true){

					/* In this implementation, ONLY look at acceleration, so add
					 * all the records to the 'blob'. */
					Coord3d c = grid.getClosest(x, y, z);
					grid.incrementCount(c, 1.0);
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

		this.grid.rankGridCells();
	}

	/**
	 * An accelerometer record is only classified based on the
	 * accelerometer data.
	 */
	@Override
	public RISK_GROUP getRiskGroup(String record) {
		/* Check that the 'blob'  has already been created and populated. */
		if(!this.grid.isRanked()){
			LOG.error("You cannot get a risk group unless the risk evaluation has been done.");
			LOG.error("First call the method 'buildScoringModel(...)");
			throw new RuntimeException();
		}

		String[] sa = record.split(",");

		/* Return accelerometer risk class. */
		double x = Double.parseDouble(sa[5]);
		double y = Double.parseDouble(sa[6]);
		double z = Double.parseDouble(sa[7]);

		/* Get the closest cell to this point. */
		Coord3d cell = grid.getClosest(x, y, z);
		int risk = grid.getCellRisk(cell);
		switch (risk) {
		case 0:
			return RISK_GROUP.NONE;
		case 1:
			return RISK_GROUP.LOW;
		case 2:
			return RISK_GROUP.MEDIUM;
		case 3:
			return RISK_GROUP.HIGH;
		default:
			throw new RuntimeException("Don't know what risk class " + risk + " is!");
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
		LOG.info("Processing records for person-specific scoring.");
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
	public DigiGrid_XYZ getGrid(){
		return this.grid;
	}

	@Override
	public void setGrid(DigiGrid_XYZ grid) {
		this.grid = grid;
	}

	public void setMaximumLines(int maxLines){
		this.maxLines = maxLines;
	}


	public static void main(String[] args) {
		Header.printHeader(AccelOnlyDigiScorer.class.toString(), args);

		/* Parse the input arguments. */
		String filename = args[0];
		String outputFolder = args[1];
		Double scale = Double.parseDouble(args[2]);
		int maxLines = Integer.parseInt(args[3]);
		Visual visual = Visual.valueOf(args[4]);

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

		AccelOnlyDigiScorer aos = new AccelOnlyDigiScorer(scale, filename, riskThresholds, visual);
		aos.setMaximumLines(maxLines);
		aos.buildScoringModel(filename);
		aos.getGrid().writeCellCountsAndRiskClasses(outputFolder);
		aos.rateIndividuals(filename, outputFolder);

		Header.printFooter();
		aos.getGrid().visualiseGrid();
	}

}
