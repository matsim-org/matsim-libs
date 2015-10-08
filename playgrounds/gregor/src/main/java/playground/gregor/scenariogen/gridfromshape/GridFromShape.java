/* *********************************************************************** *
 * project: org.matsim.*
 * GridFromShape.java
 *                                                                         *
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

package playground.gregor.scenariogen.gridfromshape;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class GridFromShape {

	private final ShapeFileReader r;
	private final String csvFile;
	private QuadTree<Cell> qt;

	public GridFromShape(String shapeFile, String csvFile) {
		this.r = new ShapeFileReader();
		this.r.readFileAndInitialize(shapeFile);
		this.csvFile = csvFile;
	}



	private void run() {
		initializeQuadTree();
		classifyCells();
		try {
			dumpResult();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	private void dumpResult() throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(new File(this.csvFile)));
		ReferencedEnvelope e = this.r.getBounds();
		for (double y = e.getMaxY(); y > e.getMinY(); y -= 0.4) {
			for (double x = e.getMinX(); x < e.getMaxX(); x += 0.4) {
				Cell c = this.qt.getClosest(x, y);
				br.append(c.type+",");
			}
			br.append('\n');
		}
		br.close();
	}



	private void classifyCells() {
		for (SimpleFeature ft : this.r.getFeatureSet()) {
			Coordinate prev = null;
			for (Coordinate c : ((Geometry)ft.getDefaultGeometry()).getCoordinates()){
				if (prev != null){
					double dx = c.x-prev.x;
					double dy = c.y-prev.y;
					double l = Math.sqrt(dx*dx+dy*dy);
					dx/=l;
					dy/=l;
					double incr = 0.2;
					for (double range = incr; range < l; range+=incr) {
						double x = prev.x + range*dx;
						double y = prev.y + range*dy;
						Cell cell = this.qt.getClosest(x, y);
						cell.type = -1;
					}
					
				}
				Cell cell = this.qt.getClosest(c.x, c.y);
				cell.type = -1;
				prev = c;
			}
		}
	}

	private void initializeQuadTree() {
		ReferencedEnvelope e = this.r.getBounds();
		this.qt = new QuadTree<>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		int steps = (int) Math.ceil((0-e.getMinX())/0.4);
		double topX = -steps*0.4-0.01+.2;
		int steps2 = (int) Math.ceil((e.getMaxY())/0.4);
		double topY = steps2*0.4-0.01-.2;
		System.out.println("top lef cell:" + topX + " " + topY);
		
		for (double y = topY; y > e.getMinY(); y -= 0.4) {
			for (double x = topX; x < e.getMaxX(); x += 0.4) {
				Cell c = new Cell();
				this.qt.put(x, y, c);
			}
		}

		
		
	}

	private static final class Cell {
		int type = 0;
	}

	public static void main(String [] args) {
//		String shapeFile = "/Users/laemmel/devel/plaueexp/boundaries_closed_transformed.shp";
		String shapeFile = "/Users/laemmel/devel/plaueexp/simpleGeo.shp";
		String csvFile ="/Users/laemmel/devel/plaueexp/grid.csv";
		new GridFromShape(shapeFile,csvFile).run();



	}

}
