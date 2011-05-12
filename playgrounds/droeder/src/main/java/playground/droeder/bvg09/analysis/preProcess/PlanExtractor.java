/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.bvg09.analysis.preProcess;

import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.droeder.DaPaths;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class PlanExtractor {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Set<Feature> features = null;
		features = new ShapeFileReader().readFileAndInitialize(DaPaths.VSP + "BVG09_Auswertung/BerlinSHP/Berlin.shp");
		
		Geometry g =  (Geometry) features.iterator().next().getAttribute(0);
		
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(DaPaths.VSP + "BVG09_Auswertung/input/network.final.xml.gz");
		
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		PersonLocationFilter algo = new PersonLocationFilter(g, 1000, sc.getNetwork());
		((PopulationImpl) sc.getPopulation()).addAlgorithm(algo);
		new MatsimPopulationReader(sc).parse(DaPaths.VSP + "BVG09_Auswertung/input/bvg.run128.25pct.100.plans.selected.xml.gz");
		
	}

}
