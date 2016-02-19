/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gctpeds.demand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos.CALinkInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DemandGenerator {

	//	private static final int NUM_PERS = 100000;
	private static final double SIGMA = 3600;
	private static final double MU = 16*3600+30*60;
	private static final double F = 15*3600+45*60;
	private static final double T = 17*3600;
	
	private Scenario sc;

	private final List<Location> origins = new ArrayList<>();
	private final List<Location> destinations = new ArrayList<>();


	public DemandGenerator(Scenario sc) {
		this.sc = sc;
	}

	private void run() {
		enrichConfig();
		findTargets();
		Population pop = sc.getPopulation();
		pop.getPersons().clear();
		PopulationFactory fac = pop.getFactory();
		int i = 0;
		for (Location o : origins) {

			for (int cnt = 0; cnt < o.dep; cnt++) {

				Person pers = fac.createPerson(Id.createPersonId(i++));
				pop.addPerson(pers);

				Plan pl = fac.createPlan();
				pers.addPlan(pl);
				double dep = Double.NEGATIVE_INFINITY;
				while (dep < F || dep > T) {
					dep = MatsimRandom.getRandom().nextGaussian()*SIGMA + MU;
				}
				Id<Link> homeLink = o.l.getId();
				Activity h = fac.createActivityFromLinkId("home", homeLink);
				pl.addActivity(h);
				h.setEndTime(dep);

				Leg leg = fac.createLeg("car");
				pl.addLeg(leg);

				Id<Link> workLink = getDest(o);
				Activity w = fac.createActivityFromLinkId("work", workLink);
				pl.addActivity(w);
			}
			for (int cnt = 0; cnt < o.dep/4; cnt++) {

				Person pers = fac.createPerson(Id.createPersonId(i++));
				pop.addPerson(pers);

				Plan pl = fac.createPlan();
				pers.addPlan(pl);
				double dep = Double.NEGATIVE_INFINITY;
				while (dep < F || dep > T) {
					dep = MatsimRandom.getRandom().nextGaussian()*SIGMA + MU;
				}
				Id<Link> homeLink = o.l.getId();
				Activity h = fac.createActivityFromLinkId("home", homeLink);
				pl.addActivity(h);
				h.setEndTime(dep);

				Leg leg = fac.createLeg("car");
				pl.addLeg(leg);

				Id<Link> workLink = getDest(o);
				Activity w = fac.createActivityFromLinkId("work", workLink);
				pl.addActivity(w);
			}
			
		}

	}

	private Id<Link> getDest(Location o) {
		double weightSum = 0;
		List<Location> cands = new ArrayList<>();
		for (Location d : this.destinations) {
			double dist = CoordUtils.calcEuclideanDistance(o.l.getCoord(), d.l.getCoord());
			if (dist > 50) {
				double w = o.dep*d.arr*(dist*dist);
				d.w = w;
				weightSum += w;
				cands.add(d);
			}
		}
		double goal = weightSum*MatsimRandom.getRandom().nextDouble();
		double incr = 0;
		for (Location cand : cands) {
			incr += cand.w;
			if (incr >= goal) {
				return cand.l.getId();
			}
		}
		return null;
	}

	private void enrichConfig() {
		Config c = this.sc.getConfig();
		c.strategy().addParam("Module_1", "ReRoute");
		c.strategy().addParam("ModuleProbability_1", ".1");
		c.strategy().addParam("ModuleDisableAfterIteration_1", "50");
		c.strategy().addParam("Module_2", "ChangeExpBeta");
		c.strategy().addParam("ModuleProbability_2", ".9");
		c.controler().setLastIteration(100);

		ActivityParams pre = new ActivityParams("home");
		pre.setTypicalDuration(49); // needs to be geq 49, otherwise when
		// running a simulation one gets
		// "java.lang.RuntimeException: zeroUtilityDuration of type pre-evac must be greater than 0.0. Did you forget to specify the typicalDuration?"
		// the reason is the double precision. see also comment in
		// ActivityUtilityParameters.java (gl)
		pre.setMinimalDuration(49);
		pre.setClosingTime(49);
		pre.setEarliestEndTime(49);
		pre.setLatestStartTime(49);
		pre.setOpeningTime(49);

		ActivityParams post = new ActivityParams("work");
		post.setTypicalDuration(49); // dito
		post.setMinimalDuration(49);
		post.setClosingTime(49);
		post.setEarliestEndTime(49);
		post.setLatestStartTime(49);
		post.setOpeningTime(49);
		sc.getConfig().planCalcScore().addActivityParams(pre);
		sc.getConfig().planCalcScore().addActivityParams(post);
		sc.getConfig().planCalcScore().setLateArrival_utils_hr(0.);
		sc.getConfig().planCalcScore().setPerforming_utils_hr(0.);


	}

	private void findTargets() {
		CALinInfos infos = null;
		try {
			FileInputStream str = new FileInputStream("/Users/laemmel/devel/nyc/gct_vicinity/ca_link_infos");
			infos = CALinInfos.parseFrom(str);
			str.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (CALinkInfo inf : infos.getCaLinkInfoList()) {
			Link l = this.sc.getNetwork().getLinks().get(Id.createLinkId(inf.getId()));
			Link depL = null;
			Link arrL = null;
			if (l.getFromNode().getInLinks().size() == 1) {
				depL = l;
			} else {
				arrL = l;
			}
			int deps = inf.getDepartures();
			if (depL != null && deps > 0) { 
				Location loc = new Location();
				loc.l = l;
				loc.dep = deps;
				this.origins.add(loc);
			}
			int arrs = inf.getArrivals();
			if (arrL != null && arrs > 0) {
				Location loc = new Location();
				loc.l = l;
				loc.arr = arrs;
				this.destinations.add(loc);
			}
		}

	}

	private static final class Location {
		Link l;
		int arr;
		int dep;
		double w;
	}


	public static void main(String [] args) {
		String configFile = "/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz";
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, configFile);
		Scenario sc = ScenarioUtils.loadScenario(c);

		c.controler().setOutputDirectory("/Users/laemmel/devel/nyc/output");

		new DemandGenerator(sc).run();
		c.plans().setInputFile("/Users/laemmel/devel/nyc/gct_vicinity/plans.xml.gz");
		new PopulationWriter(sc.getPopulation()).write("/Users/laemmel/devel/nyc/gct_vicinity/plans.xml.gz");
		new ConfigWriter(c).write("/Users/laemmel/devel/nyc/gct_vicinity/config.xml.gz");

	}



}
