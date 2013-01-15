/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationGeneratorSim2DV4.java
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

package playground.gregor.multidestpeds.helper;

import java.io.IOException;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.gregor.multidestpeds.io.Importer;
import playground.gregor.multidestpeds.io.Ped;
import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DConfigUtils;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.scenario.Sim2DScenarioUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class PopulationGeneratorSim2DV4 {

	
	public static void main(String [] args) {
		
		String inputMat = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/experimental_data/Dez2010/joined/gr90.mat";
		String scDir = "/Users/laemmel/devel/gr90_sim2d_v4/";
		String inputDir = scDir + "/input";
		
		Sim2DConfig s2dConfig = Sim2DConfigUtils.loadConfig(inputDir + "/s2d_config.xml");
		Sim2DScenario s2dScenario = Sim2DScenarioUtils.loadSim2DScenario(s2dConfig);
		
		Config config = ConfigUtils.loadConfig(inputDir + "/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(config);
		sc.addScenarioElement(s2dScenario);
//		s2dScenario.connect(sc);
		createPop(sc,inputMat);
	}

	private static void createPop(Scenario sc, String inputMat) {
		Importer imp = new Importer();
		try {
			imp.read(inputMat, sc);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		List<Ped> peds = imp.getPeds();
		
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		
		sc.getPopulation().getPersons().clear();
		PopulationFactory fac = sc.getPopulation().getFactory();
		for (Ped ped : peds) {
			double dep = ped.depart;
			Coordinate depCoord = ped.coords.get(dep);
			double arr = ped.arrived;
			Coordinate arrCoord = ped.coords.get(arr);
			Person pers = fac.createPerson(ped.id);
			Plan plan = fac.createPlan();
//			Link ol = net.getNearestLinkExactly(MGC.coordinate2Coord(depCoord));
			Id oid;
			if (pers.getId().toString().contains("g")) {
				oid = new IdImpl("sim2d_0_-1611");
			} else {
				oid = new IdImpl("sim2d_0_-1616");
			}
			Activity act0 = fac.createActivityFromLinkId("origin", oid);
			act0.setEndTime(dep);
			plan.addActivity(act0);
			Leg leg = fac.createLeg("car");
			plan.addLeg(leg);
			Id did;
			if (pers.getId().toString().contains("r")) {
				did = new IdImpl("sim2d_0_rev_-1592");
			} else {
				did = new IdImpl("sim2d_0_rev_-1624");
			}
			Activity act1 = fac.createActivityFromLinkId("destination", did);
			plan.addActivity(act1);
			pers.addPlan(plan);
			sc.getPopulation().addPerson(pers);
		}
		new PopulationWriter(sc.getPopulation(), net).write(sc.getConfig().plans().getInputFile());
	}
}
