/* *********************************************************************** *
 * project: org.matsim.*
 * ZrhFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.mz2005;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.EsriShapeIO;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import playground.johannes.coopsim.util.MatsimCoordUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ZrhFilter {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FactoryException 
	 */
	public static void main(String[] args) throws IOException, FactoryException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimPopulationReader reader = new MatsimPopulationReader(scenario);
		reader.readFile("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/09-12-2011/plans.xml");

		Set<SimpleFeature> features = EsriShapeIO.readFeatures("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/Kanton.shp");
		Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
		
		Set<Person> remove = new HashSet<Person>();
		for(Person p : scenario.getPopulation().getPersons().values()) {
			Point c = MatsimCoordUtils.coordToPoint(((Activity)p.getSelectedPlan().getPlanElements().get(0)).getCoord());
			c = CRSUtils.transformPoint(c, CRS.findMathTransform(CRSUtils.getCRS(4326), CRSUtils.getCRS(21781)));
			
			if(!geometry.contains(c)) {
				remove.add(p);
			}
		}
		
		System.out.println("Removing " + remove.size() + " persons.");
		for(Person p : remove) {
			scenario.getPopulation().getPersons().remove(p.getId());
		}
		
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), null);
		writer.write("/Users/jillenberger/Work/socialnets/data/schweiz/mz2005/rawdata/09-12-2011/zrh/plans.xml");
	}

}
