/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreRiskCubeGrid.java
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
package playground.southafrica.projects.digicore.grid;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.projects.digicore.scoring.wp3.Wp3RiskCube;

/**
 * Building a custom grid based on the Digicore RiskCube.
 * 
 * @author jwjoubert
 */
public class DigicoreRiskCubeGrid extends DigiGrid {
	final private Logger log = Logger.getLogger(DigicoreRiskCubeGrid.class);
	
	double pointsConsidered = 0.0;

	/* Specific risk space objects. */
	private Map<String, Integer> countMap = new TreeMap<>();
	private Map<String, Integer> riskMap;
	
	
	public DigicoreRiskCubeGrid(List<Double> riskThresholds) {
		this.setRiskThresholds(riskThresholds);
	}
	
	@Override
	public void setupGrid(String filename) {
		/* In this implementation I work with Digicore's discretization, so no
		 * need to set up anything specific in the grid. The constructor 
		 * creates the necessary structures/maps. */
	}
	
	public void incrementCell(String record){
		String speed = String.format("%02d", Wp3RiskCube.getRiskCubeSpeedBin(record));
		String accelX = String.format("%02d", Wp3RiskCube.getRiskcubeAccelXBin(record));
		String accelY = String.format("%02d", Wp3RiskCube.getRiskcubeAccelYBin(record));
		String cell = speed + "_" + accelX + "_" + accelY;
		
		if(!countMap.containsKey(cell)){
			countMap.put(cell, 1);
		} else{
			int oldValue = countMap.get(cell);
			countMap.put(cell, oldValue+1);
		}
		pointsConsidered++;
	}
	
	
	public void setPopulated(boolean populated){
		super.setPopulated(populated);
	}
	
	
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
			bw.write("x,y,speed,count,class");
			bw.newLine();
			
			for(String s : countMap.keySet()){
				String[] sa = s.split("_");
				int speed = Integer.parseInt(sa[0]);
				int x = Integer.parseInt(sa[1]);
				int y = Integer.parseInt(sa[2]);
				bw.write(String.format("%d,%d,%d,%d,%d\n", x, y, speed, countMap.get(s), riskMap.get(s)));
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
	
	public void setRanked(boolean ranked){
		super.setRanked(ranked);
	}


	@Override
	public void rankGridCells() {
		Comparator<String> comparator = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return countMap.get(o2).compareTo(countMap.get(o1));
			}
		};
		List<String> sortedCells = new ArrayList<>(countMap.keySet());
		Collections.sort(sortedCells, comparator);
		
		/* Report the top 20 cell values. */
		log.info("   20 cells with largest number of observations:");
		for(int i = 0; i < 20; i++){
			log.info(String.format("      %d: %d observations", i+1, countMap.get(sortedCells.get(i))));
		}

		double totalAdded = 0.0;
		double cumulative = 0.0;
		this.riskMap = new TreeMap<>(comparator);
		
		List<String> cellsToRemove = new ArrayList<String>();

		double maxValue = 0.0;
		for(int i = 0; i < sortedCells.size(); i++){
			String s = sortedCells.get(i);
			double obs = countMap.get(s);
			if(obs > 0){
				maxValue = Math.max(maxValue, (double)obs);
				totalAdded += (double)obs;
				cumulative = totalAdded / pointsConsidered;
				
				/* Get the rating class for this value. */
				Integer ratingZone = null;
				int zoneIndex = 0;
				while(ratingZone == null && zoneIndex < getRiskThresholds().size()){
					if(cumulative <= getRiskThresholds().get(zoneIndex)){
						ratingZone = new Integer(zoneIndex);
					} else{
						zoneIndex++;
					}
				}
				riskMap.put(s, ratingZone);
			} else{
				cellsToRemove.add(s);
			}
		}

		this.setRanked(true);
		log.info("Done ranking grid cells.");
		log.info("A total of " + countMap.size() + " cells contain points (max value: " + maxValue + ")");
	}
	
	public int getRiskGroup(String key){
		if(!riskMap.containsKey(key)){
			throw new RuntimeException("Cannot get a risk class for record with key " + key);
		}
		
		return riskMap.get(key);
	}

}
