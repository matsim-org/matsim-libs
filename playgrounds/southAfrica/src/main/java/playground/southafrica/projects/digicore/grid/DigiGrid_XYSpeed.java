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

package playground.southafrica.projects.digicore.grid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jzy3d.maths.Coord3d;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

/**
 * Class that acts as the container for the three-dimensional grid containing
 * the centroids of the polyhedra that is used for the Digicore accelerometer
 * research. Associated with the grid is the number of observations in each
 * dodecahedron (cell) and also the rating of each cell.
 *
 * @author jwjoubert
 */
public class DigiGrid_XYSpeed  extends DigiGrid {
	final private Logger LOG = Logger.getLogger(DigiGrid_XYSpeed.class);
	private double speed_multiplier = 2.5;

	
	public DigiGrid_XYSpeed(final double scale) {
		super(scale);
	}
	

	public void setupGrid(String filename){
		/* Check that risk zone thresholds have been set. */
		if(riskThresholds == null){
			LOG.error("Cannot build the grid without risk thresholds.");
			throw new RuntimeException("First set thresholds with setRiskThresholds() method.");
		}
		
		LOG.info("Calculating the data extent...");
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minSpeed = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxSpeed = Double.NEGATIVE_INFINITY;
		
		Counter counter = new Counter("   lines # ");
		BufferedReader br = IOUtils.getBufferedReader(filename);
		try{
			String line = null;
			while( (line = br.readLine()) != null){
				String[] sa = line.split(",");
				double x = Double.parseDouble(sa[5]);
				double y = Double.parseDouble(sa[6]);
				double speed = Double.parseDouble(sa[8])*speed_multiplier;
				
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				minSpeed = Math.min(minSpeed, speed);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
				maxSpeed = Math.max(maxSpeed, speed);
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

		/* Establish the centroid grid given the point extent. */
		FCCGrid fccg = new FCCGrid(minX, maxX, minY, maxY, minSpeed, maxSpeed, super.getScale());
		GridPoint[] ga = fccg.getFcGrid();
		for(GridPoint gp : ga){
			minX = Math.min(minX, gp.getX());
			minY = Math.min(minY, gp.getY());
			minSpeed = Math.min(minSpeed, gp.getZ());
			maxX = Math.max(maxX, gp.getX());
			maxY = Math.max(maxY, gp.getY());
			maxSpeed = Math.max(maxSpeed, gp.getZ());
		}
		LOG.info("Done calculating data extent.");

		/* Establish and populate the OcTree given the centroid extent. */
		LOG.info("Building OcTree with dodecahedron centroids... (" + ga.length + " centroids)");
		Counter centroidCounter = new Counter("   centroid # "); 
		super.map = new HashMap<Coord3d, Double>(ga.length);
		super.ot = new OcTree<Coord3d>(minX, minY, minSpeed, maxX, maxY, maxSpeed);
		for(GridPoint gp : ga){
			Coord3d c = new Coord3d(gp.getX(), gp.getY(), gp.getZ());
			ot.put(gp.getX(), gp.getY(), gp.getZ(), c);
			map.put(c, new Double(0.0));
			centroidCounter.incCounter();
		}
		centroidCounter.printCounter();
		LOG.info("Done populating centroid grid: " + ga.length + " points.");
	}


	@Override
	public Coord3d convertToCoord3d(double x, double y, double z) {
		return new Coord3d(x, y, z*speed_multiplier);
	}


	@Override
	public Coord3d getClosest(double x, double y, double z) {
		return this.ot.get(x, y, z*speed_multiplier);
	}
	
	
	@Override
	public void writeCellCountsAndRiskClasses(String outputFolder) {
		if(map.size() == 0 || mapRating == null){
			throw new RuntimeException("Insufficient data to write. Either no grids, or no ranking.");
		}
		String filename = outputFolder + (outputFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv";
		LOG.info("Writing the cell values and risk classes to " + filename); 
		
		/* Report the risk thresholds for which this output holds. */
		LOG.info("  \\_ Accelerometer risk thresholds:");
		for(int i = 0; i < this.riskThresholds.size(); i++){
			LOG.info(String.format("      \\_ Risk %d: %.4f", i, this.riskThresholds.get(i)));
		}
		
		/* Write the cell values and their risk classes. */
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			/* Header. */
			bw.write("x,y,speed,count,class");
			bw.newLine();
			
			for(Coord3d c : this.map.keySet()){
				bw.write(String.format("%.4f, %.4f,%.4f,%.1f,%d\n", c.x, c.y, c.z/speed_multiplier, this.map.get(c), this.mapRating.get(c)));
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

	
	public double getSpeedMultiplier(){
		return this.speed_multiplier;
	}
	
	
	public void setSpeedMultiplier(double speedMultiplier){
		this.speed_multiplier = speedMultiplier;
	}



}
