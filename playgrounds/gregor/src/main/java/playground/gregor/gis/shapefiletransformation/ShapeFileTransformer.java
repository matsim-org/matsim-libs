/* *********************************************************************** *
 * project: org.matsim.*
 * ShapeFileTransformer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.shapefiletransformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ShapeFileTransformer {
	List<TP> transformationPoint = new ArrayList<TP>();
	private final String inFile;
	private final String outFile;

	public ShapeFileTransformer(String inFile, String outFile) {
		this.transformationPoint.add(new TP(650983.216200,9899892.899700,650976.859978,9899897.975674));
		this.transformationPoint.add(new TP(651863.890200,9906193.308800,651857.327300,9906198.661757));
		this.transformationPoint.add(new TP(655038.672000,9902242.503700,655033.121934,9902247.243937));
		this.transformationPoint.add(new TP(655669.991100,9896712.201500,655664.955244,9896716.533197));
		this.transformationPoint.add(new TP(655669.991100,9896712.201500,655664.955244,9896716.533197));
		this.transformationPoint.add(new TP(656430.405300,9892289.742300,656425.841225,9892293.717175));
		this.transformationPoint.add(new TP(653968.379700,9891185.322900,653963.304976,9891189.521052));
		this.transformationPoint.add(new TP(650568.164200,9893651.850100,650562.121086,9893656.597831));
		this.transformationPoint.add(new TP(650400.149900,9895822.881100,650393.923831,9895827.779820));
		this.transformationPoint.add(new TP(649670.880700,9899766.272400,649664.221829,9899771.495319));
		this.transformationPoint.add(new TP(649182.494600,9903415.126400,649175.725982,9903420.603632));
		this.transformationPoint.add(new TP(647860.942500,9907223.802000,647853.363038,9907229.688753));
		this.transformationPoint.add(new TP(648930.972900,9907887.844300,648923.603237,9907893.645140));
		this.inFile = inFile;
		this.outFile = outFile;


	}

	private void run() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(this.inFile);
		Iterator it = fts.getFeatures().iterator();
		List<Feature> outFts = new ArrayList<Feature>();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Geometry geo = ft.getDefaultGeometry();
			Coordinate [] coords = geo.getCoordinates();
			for (Coordinate c : coords) {
				transformCoord(c);

			}
			outFts.add(ft);
		}

		ShapeFileWriter.writeGeometries(outFts, this.outFile);

	}


	private void transformCoord(Coordinate c) {

		double d_x = 0;
		double d_y = 0;
		double distSum = 0;

		for (TP tp : this.transformationPoint) {
			double infl = 1/tp.getDist(c);
			d_x += tp.d_x * infl;
			d_y += tp.d_y * infl;
			distSum += infl;


		}
		d_x /= distSum;
		d_y /= distSum;

		c.x = c.x + d_x;
		c.y = c.y + d_y;


	}

	public static void main(String [] args) {
		String inFile = "/home/laemmel/arbeit/tmp/Dyn_Exp_3Tageszeiten.shp";
		String outFile = "/home/laemmel/arbeit/tmp/tmp/Dyn_Exp_3Tageszeiten.shp";
		try {
			new ShapeFileTransformer(inFile,outFile).run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static class TP {
		private final double d_x;
		private final double d_y;
		private final Coordinate c;


		public TP(double x_o, double y_o, double x_n, double y_n){
			this.d_x = x_n - x_o;
			this.d_y = y_n - y_o;
			this.c = new Coordinate(x_o,y_o);
		}

		public double getDist(Coordinate c) {
			return this.c.distance(c);
		}
	}
}

