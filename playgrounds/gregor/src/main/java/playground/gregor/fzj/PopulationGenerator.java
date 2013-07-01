/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.fzj;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class PopulationGenerator {
	
	
	public static void main (String [] args) {
		String config = "/Users/laemmel/devel/fzj/input/config.xml";
		String spawns = "/Users/laemmel/devel/fzj/raw_input/spawns.shp";
		
		Config conf = ConfigUtils.loadConfig(config);
		Scenario sc = ScenarioUtils.loadScenario(conf);
		
		Collection<SimpleFeature> sr = new ShapeFileReader().readFileAndInitialize(spawns);
		int numPers = 4000;
		int frac = numPers/sr.size();
		
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		
		Link eatL = net.getNearestLinkExactly(new CoordImpl(713263.61,6571891.82));
		
		Id eat = eatL.getId();
		
		
		int id = 0;
		for (SimpleFeature ft : sr) {
			Object geo = ft.getDefaultGeometry();
			Coordinate c = ((Geometry)geo).getCoordinate();
			
			Link l = net.getNearestLinkExactly(new CoordImpl(c.x,c.y));
			
			for (int i = 0; i < frac; i++) {
				Person pers = fac.createPerson(new IdImpl(id++));
				pop.addPerson(pers);
				
				Plan plan = fac.createPlan();
				pers.addPlan(plan);
				
				Activity act0 = fac.createActivityFromLinkId("origin", l.getId());
				double offset = MatsimRandom.getRandom().nextGaussian()*1200;
				double time = 12*3600+offset;
				act0.setEndTime(time);
				plan.addActivity(act0);
				
				Leg leg0 = fac.createLeg("car");
				plan.addLeg(leg0);
				
				Activity act1 = fac.createActivityFromLinkId("destination", eat);
				act1.setEndTime(time+30*60);
				
				plan.addActivity(act1);
				
				Leg leg1 = fac.createLeg("car");
				plan.addLeg(leg1);
				
				Activity act2 = fac.createActivityFromLinkId("origin", l.getId());
				plan.addActivity(act2);
			}
			
		}
		
		new PopulationWriter(pop, sc.getNetwork()).write(conf.plans().getInputFile());
	}

}
