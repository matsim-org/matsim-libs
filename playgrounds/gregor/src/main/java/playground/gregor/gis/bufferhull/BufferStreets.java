/* *********************************************************************** *
 * project: org.matsim.*
 * BufferStreets.java
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
package playground.gregor.gis.bufferhull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

public class BufferStreets {
	static String input = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/link_risk_costs.shp";
	static String output1 = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/link_risk_costs_fl.shp";
	static String output2 = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/link_risk_costs_buff.shp";
	static String output3 = "/home/laemmel/arbeit/svn/shared-svn/projects/LastMile/berichte/abschlussworkshop/gis/link_risk_costs_fl_90.shp";
	
	
	public static void main(String []args) {
		try {
			run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void run() throws IOException, IllegalAttributeException {
		Collection<Feature> fts1 = new ArrayList<Feature>();
		Collection<Feature> fts2 = new ArrayList<Feature>();
		Collection<Feature> fts3 = new ArrayList<Feature>();
		FeatureSource fs = ShapeFileReader.readDataFile(input);
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Double d = (Double) ft.getAttribute("risk");
			if (d >= 90) {
				fts3.add(ft);
			}else if (d >= 50 ) {
				fts1.add(ft);
			} else {
				fts2.add(ft);
			}
		}
		ShapeFileWriter.writeGeometries(fts1, output1);
		ShapeFileWriter.writeGeometries(fts2, output2);
		ShapeFileWriter.writeGeometries(fts3, output3);
		
	}

}
