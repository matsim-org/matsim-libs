/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeAttributeOrder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.gis.featurechanger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.ShapeFileWriter;



public class ChangeAttributeOrder {



	public static void main(String [] args) throws IOException, ArrayIndexOutOfBoundsException, IllegalAttributeException {
		String file = "./padang/referencing/referenced10.shp";
		int from = 2;
		int to = 3;
		FeatureSource fs = ShapeFileReader.readDataFile(file);
		Collection<Feature> fc = processFeatures(fs,from,to);
		ShapeFileWriter.writeGeometries(fc, file);


	}

	private static Collection<Feature> processFeatures(FeatureSource fs, int from, int to) throws IOException, ArrayIndexOutOfBoundsException, IllegalAttributeException {
		Iterator it = fs.getFeatures().iterator();
		Collection<Feature> fc = new ArrayList<Feature>();
		while (it.hasNext()){
			Feature ft = (Feature)it.next();
			Object tmp = ft.getAttribute(from);
			ft.setAttribute(from, ft.getAttribute(to));
			ft.setAttribute(to, tmp);
			fc.add(ft);
		}


		return fc;
	}

}
