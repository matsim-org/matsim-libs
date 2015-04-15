/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryCleaner.java
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class TrajectoryCleaner {

	private final ShapeFileReader r;
	private final String trIn;
	private final String trOut;
	public TrajectoryCleaner(String shape, String trIn, String trOut) {
		this.r = new ShapeFileReader();
		this.r.readFileAndInitialize(shape);
		this.trIn = trIn;
		this.trOut = trOut;
	}

	private void run() throws IOException {
		Geometry g = null;
		for (SimpleFeature f : this.r.getFeatureSet()){
			Geometry tmp = (Geometry) f.getDefaultGeometry();
			if (g == null) {
				g = tmp;
			} else {
				g = g.union(tmp);
			}
		}
		g = g.convexHull();
		BufferedReader tR = new BufferedReader(new FileReader(new File(this.trIn)));
		BufferedWriter tW = new BufferedWriter(new FileWriter(new File(this.trOut)));
		
		String l = tR.readLine();
		while (l != null) {
			String[] expl = StringUtils.explode(l, ' ');
			if (expl.length != 5) {
				tW.append(l);
			} else {
				double x = Double.parseDouble(expl[2]);
				double y = Double.parseDouble(expl[3]);
				Point p = MGC.xy2Point(x, y);
				if (g.contains(p)){
					tW.append(l);	
				}
			}
			tW.append('\n');
			l = tR.readLine();
		}
		tR.close();
		tW.close();
		
	}
	public static void main(String [] args) {
		String shape = "/Users/laemmel/devel/plaueexp/simpleGeo.shp";
		String trIn = "/Users/laemmel/devel/plaueexp/LNdW/br180/jpsTrajectories.txt";
		String trOut = "/Users/laemmel/devel/plaueexp/LNdW/br180/jpsTrajectoriesCleaned.txt";
		try {
			new TrajectoryCleaner(shape,trIn,trOut).run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
