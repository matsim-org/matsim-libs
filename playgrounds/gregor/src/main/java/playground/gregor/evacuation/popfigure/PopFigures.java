/* *********************************************************************** *
 * project: org.matsim.*
 * PopFigures.java
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
package playground.gregor.evacuation.popfigure;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

public class PopFigures {
	
	public static void main(String [] args) throws IOException {
		
		String popFile = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/buildings_v20100315/evac_zone_buildings_v20100315.shp";
		FeatureSource fs = ShapeFileReader.readDataFile(popFile);
		Iterator it = fs.getFeatures().iterator();
		int night = 0;
		int morning = 0;
		int afternoon = 0;
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			night += (Integer)ft.getAttribute("popNight");
			morning += (Integer)ft.getAttribute("popDay");
			afternoon += (Integer)ft.getAttribute("popAf");
		}
		System.out.println("night:" + night + " morn:" + morning + " af:" + afternoon);
	}

}
