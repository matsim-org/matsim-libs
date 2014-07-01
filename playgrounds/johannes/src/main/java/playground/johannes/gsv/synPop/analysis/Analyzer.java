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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;
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
		
		parser.parse("/home/johannes/gsv/synpop/output/1400000000.pop.xml.gz");
//		parser.parse("/home/johannes/gsv/mid2008/pop.xml");
		
		Set<SimpleFeature> features = FeatureSHP.readFeatures("/home/johannes/gsv/synpop/data/gis/nuts/Gemeinden.gk3.shp");
		Set<Geometry> geometries = new HashSet<Geometry>();
 		for(SimpleFeature feature : features) {
			geometries.add((Geometry) feature.getDefaultGeometry());
		}
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		FacilitiesReaderMatsimV1 facReader = new FacilitiesReaderMatsimV1(scenario);
		facReader.readFile("/home/johannes/gsv/synpop/data/facilities/facilities.ger.all.xml");
		ActivityFacilities facilities = scenario.getActivityFacilities();
	
	
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
//		task.addComponent(new PopulationDensityTask(geometries, "/home/johannes/gsv/synpop/output/popDen.kmz"));
		task.addComponent(new ActivityDistanceTask(facilities, "/home/johannes/gsv/synpop/output/"));
//		task.addComponent(new ActivityChainTask("/home/johannes/gsv/synpop/output/"));

		task.analyze(parser.getPersons());
	}

}
