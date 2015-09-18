/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreActivityDensityRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.freight.digicore.algorithms.kernelDensity;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.southafrica.freight.digicore.containers.DigicoreActivity;
import playground.southafrica.freight.digicore.containers.DigicoreChain;
import playground.southafrica.freight.digicore.containers.DigicoreVehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class DigicoreActivityDensityCallable implements Callable<String> {
	private List<Coord> list = new ArrayList<Coord>();
	private Polygon polygon;
	private DigicoreVehicle vehicle;
	private Counter counter;
	private Raster raster;
	private final File tempFolder;
	
	public DigicoreActivityDensityCallable(MultiPolygon polygon, DigicoreVehicle vehicle, Counter counter, 
			double resolution, double radius, int kdeType, Color color, String tempFolder) throws IOException {
		this.polygon = (Polygon) polygon.getEnvelope();
		this.vehicle = vehicle;
		this.counter = counter;
		raster = new Raster(this.polygon, resolution, radius, kdeType, color);
		this.tempFolder = new File(tempFolder);
		if(!this.tempFolder.exists() || !this.tempFolder.isDirectory() || !this.tempFolder.canWrite()){
			throw new IOException("Temporary outputFolder is not writable.");
		}
	}

	@Override
	public String call() throws Exception {
		GeometryFactory gf = new GeometryFactory();
		Polygon envelope = (Polygon)polygon.getEnvelope();
		for(DigicoreChain chain : vehicle.getChains()){
			for(DigicoreActivity activity : chain.getAllActivities()){
				Point p = gf.createPoint(new Coordinate(activity.getCoord().getX(), activity.getCoord().getY()));
				if(envelope.contains(p)){
					if(polygon.contains(p)){
						list.add(new Coord(p.getX(), p.getY()));
					}
				}
			}
		}

		raster.processPoints(list);
		String filename = this.tempFolder.getAbsolutePath() + "/" + vehicle.getId().toString() + "txt.gz";
		File f = new File(filename);
		f.deleteOnExit();
		BufferedWriter bw = IOUtils.getBufferedWriter(f.getAbsolutePath());
		try{
			for(int row = 0; row < raster.rows(); row++){
				for(int col = 0; col < raster.columns(); col++){
					if(raster.getImageMatrixValue(row, col) > 0.0){
						bw.write(String.valueOf(row));
						bw.write(",");
						bw.write(String.valueOf(col));
						bw.write(",");
						bw.write(String.valueOf(raster.getImageMatrixValue(row, col)));
						bw.newLine();
					}
				}
			}
		} finally{
			bw.close();
		}
		/* Just clear the memory-intensive stuff. */
		list = null;
		polygon = null;
		vehicle = null;
		raster = null;
		
		counter.incCounter();
		return filename;
	}

}

