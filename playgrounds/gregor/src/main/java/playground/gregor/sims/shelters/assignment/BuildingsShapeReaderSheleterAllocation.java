/* *********************************************************************** *
 * project: org.matsim.*
 * BuildingsShapeReaderSheleterAllocation.java
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
package playground.gregor.sims.shelters.assignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;

public class BuildingsShapeReaderSheleterAllocation extends BuildingsShapeReader {

	public static List<Building> readDataFile(String shelterFile,
			String buildingsFile, double sampleSize) {
		List<Building> list = BuildingsShapeReader.readDataFile(buildingsFile, sampleSize);
		for (Building b : list) {
			b.setShelterSpace(0);
			b.setIsQuakeProof(0);
		}
		List<Building> list2;
		try {
			list2 = getShelters(shelterFile,sampleSize);
			list.addAll(list2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	private static List<Building> getShelters(String shelterFile,
			double sampleSize) throws IOException {
		List<Building> bs = new ArrayList<Building>();
		FeatureSource fts = ShapeFileReader.readDataFile(shelterFile);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Long cap = (Long) ft.getAttribute("capacity");
			String id = (String) ft.getAttribute("ID");
			Building b = new Building(new IdImpl(id), 0, 0, 0, 1, (int) (cap * sampleSize), 3, 1, ft.getDefaultGeometry());
			bs.add(b);
		}
		
		return bs;
	}

}
