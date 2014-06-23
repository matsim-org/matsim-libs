/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.analysis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opengis.feature.simple.SimpleFeature;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.io.DoubleSerializer;
import playground.johannes.gsv.synPop.io.IntegerSerializer;
import playground.johannes.gsv.synPop.io.PointSerializer;
import playground.johannes.gsv.synPop.io.XMLParser;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author johannes
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		XMLParser parser = new XMLParser();
		parser.addSerializer(MIDKeys.PERSON_MUNICIPALITY_CLASS, IntegerSerializer.instance());
		parser.addSerializer(CommonKeys.PERSON_WEIGHT, DoubleSerializer.instance());
		parser.addSerializer(CommonKeys.PERSON_HOME_POINT, new PointSerializer());
		parser.setValidating(false);
		
		parser.parse("/home/johannes/Schreibtisch/10000000.pop.xml");
		
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/synpop/data/gis/marktzellen/plz8.gk3.shp");
		Set<Geometry> geometries = new HashSet<Geometry>();
 		for(SimpleFeature feature : features) {
			geometries.add((Geometry) feature.getDefaultGeometry());
		}
		PopulationDensityTask task = new PopulationDensityTask(geometries, "/home/johannes/gsv/synpop/output/popDen.shp");
		
		task.analyze(parser.getPersons());

	}

}
