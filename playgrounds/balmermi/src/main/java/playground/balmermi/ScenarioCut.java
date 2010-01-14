/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioCut.java
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

package playground.balmermi;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class ScenarioCut {

	//////////////////////////////////////////////////////////////////////

	private static void calcExtent(Scenario scenario) {
		Coord min = new CoordImpl(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		Coord max = new CoordImpl(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
		for (ActivityFacilityImpl f : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			if (f.getCoord().getX() < min.getX()) { min.setX(f.getCoord().getX()); }
			if (f.getCoord().getY() < min.getY()) { min.setY(f.getCoord().getY()); }
			if (f.getCoord().getX() > max.getX()) { max.setX(f.getCoord().getX()); }
			if (f.getCoord().getY() > max.getY()) { max.setY(f.getCoord().getY()); }
		}
		System.out.println("Spatial extent facilities: min:"+min+"; max:"+max);

		min = new CoordImpl(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		max = new CoordImpl(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
		for (Node n : scenario.getNetwork().getNodes().values()) {
			if (n.getCoord().getX() < min.getX()) { min.setX(n.getCoord().getX()); }
			if (n.getCoord().getY() < min.getY()) { min.setY(n.getCoord().getY()); }
			if (n.getCoord().getX() > max.getX()) { max.setX(n.getCoord().getX()); }
			if (n.getCoord().getY() > max.getY()) { max.setY(n.getCoord().getY()); }
		}
		System.out.println("Spatial extent network: min:"+min+"; max:"+max);

		min = new CoordImpl(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY);
		max = new CoordImpl(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY);
		for (Person p : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (PlanElement e : plan.getPlanElements()) {
					if (e instanceof ActivityImpl) {
						ActivityImpl a = (ActivityImpl)e;
						if (a.getCoord() != null) {
							if (a.getCoord().getX() < min.getX()) { min.setX(a.getCoord().getX()); }
							if (a.getCoord().getY() < min.getY()) { min.setY(a.getCoord().getY()); }
							if (a.getCoord().getX() > max.getX()) { max.setX(a.getCoord().getX()); }
							if (a.getCoord().getY() > max.getY()) { max.setY(a.getCoord().getY()); }
						}
						if (a.getLinkId() != null) {
							Node n = a.getLink().getFromNode();
							if (n.getCoord().getX() < min.getX()) { min.setX(n.getCoord().getX()); }
							if (n.getCoord().getY() < min.getY()) { min.setY(n.getCoord().getY()); }
							if (n.getCoord().getX() > max.getX()) { max.setX(n.getCoord().getX()); }
							if (n.getCoord().getY() > max.getY()) { max.setY(n.getCoord().getY()); }
							n = a.getLink().getToNode();
							if (n.getCoord().getX() < min.getX()) { min.setX(n.getCoord().getX()); }
							if (n.getCoord().getY() < min.getY()) { min.setY(n.getCoord().getY()); }
							if (n.getCoord().getX() > max.getX()) { max.setX(n.getCoord().getX()); }
							if (n.getCoord().getY() > max.getY()) { max.setY(n.getCoord().getY()); }
						}
						if (a.getFacilityId() != null) {
							ActivityFacility f = a.getFacility();
							if (f.getCoord().getX() < min.getX()) { min.setX(f.getCoord().getX()); }
							if (f.getCoord().getY() < min.getY()) { min.setY(f.getCoord().getY()); }
							if (f.getCoord().getX() > max.getX()) { max.setX(f.getCoord().getX()); }
							if (f.getCoord().getY() > max.getY()) { max.setY(f.getCoord().getY()); }
						}
					}
				}
			}
			if (((ScenarioImpl) scenario).getKnowledges().getKnowledgesByPersonId().get(p.getId()) != null) {
				for (ActivityOptionImpl ao : ((ScenarioImpl) scenario).getKnowledges().getKnowledgesByPersonId().get(p.getId()).getActivities()) {
					ActivityFacilityImpl f = ao.getFacility();
					if (f.getCoord().getX() < min.getX()) { min.setX(f.getCoord().getX()); }
					if (f.getCoord().getY() < min.getY()) { min.setY(f.getCoord().getY()); }
					if (f.getCoord().getX() > max.getX()) { max.setX(f.getCoord().getX()); }
					if (f.getCoord().getY() > max.getY()) { max.setY(f.getCoord().getY()); }
				}
			}
		}
		System.out.println("Spatial extent population: min:"+min+"; max:"+max);
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceFacilities(ActivityFacilitiesImpl facilities, Network network) {
		System.out.println("removing facilities that refer to removed links of the network... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			if (network.getLinks().get(f.getLink().getId()) == null) { toRemove.add(f.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" facilities to remove.");
		for (Id id : toRemove) { facilities.getFacilities().remove(id); }
		System.out.println("=> "+facilities.getFacilities().size()+" facilities left.");
		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceFacilities(ActivityFacilitiesImpl facilities, Coord center, double radius) {
		System.out.println("removing facilities outside of circle ("+center.toString()+";"+radius+""+")... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			if (f.calcDistance(center) > radius) { toRemove.add(f.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" facilities to remove.");
		for (Id id : toRemove) { facilities.getFacilities().remove(id); }
		System.out.println("=> "+facilities.getFacilities().size()+" facilities left.");
		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceFacilities(ActivityFacilitiesImpl facilities, Coord min, Coord max) {
		System.out.println("removing facilities outside of rectangle ("+min.toString()+";"+max.toString()+""+")... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
			Coord c = f.getCoord();
			if (c.getX() < min.getX()) { toRemove.add(f.getId()); continue; }
			if (c.getX() > max.getX()) { toRemove.add(f.getId()); continue; }
			if (c.getY() < min.getY()) { toRemove.add(f.getId()); continue; }
			if (c.getY() > max.getY()) { toRemove.add(f.getId()); continue; }
		}
		System.out.println("=> "+toRemove.size()+" facilities to remove.");
		for (Id id : toRemove) { facilities.getFacilities().remove(id); }
		System.out.println("=> "+facilities.getFacilities().size()+" facilities left.");
		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceNetwork(NetworkImpl network, Coord center, double radius) {
		System.out.println("removing links outside of circle ("+center.toString()+";"+radius+""+")... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (Link l : network.getLinks().values()) {
			CoordImpl fc = (CoordImpl)l.getFromNode().getCoord();
			CoordImpl tc = (CoordImpl)l.getToNode().getCoord();
			if (fc.calcDistance(center) > radius) { toRemove.add(l.getId()); }
			else if (tc.calcDistance(center) > radius) { toRemove.add(l.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" links to remove.");
		for (Id id : toRemove) { network.removeLink(network.getLinks().get(id)); }
		System.out.println("=> "+network.getLinks().size()+" links left.");
		System.out.println("done. " + (new Date()));

		System.out.println("cleaning network... " + (new Date()));
		new NetworkCleaner().run(network);
		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceNetwork(NetworkImpl network, Coord min, Coord max) {
		System.out.println("removing links outside of rectangle ("+min.toString()+";"+max.toString()+""+")... " + (new Date()));
		Set<Id> toRemove = new HashSet<Id>();
		for (LinkImpl l : network.getLinks().values()) {
			Coord fc = l.getFromNode().getCoord();
			if (fc.getX() < min.getX()) { toRemove.add(l.getId()); continue; }
			if (fc.getX() > max.getX()) { toRemove.add(l.getId()); continue; }
			if (fc.getY() < min.getY()) { toRemove.add(l.getId()); continue; }
			if (fc.getY() > max.getY()) { toRemove.add(l.getId()); continue; }
			Coord tc = l.getToNode().getCoord();
			if (tc.getX() < min.getX()) { toRemove.add(l.getId()); continue; }
			if (tc.getX() > max.getX()) { toRemove.add(l.getId()); continue; }
			if (tc.getY() < min.getY()) { toRemove.add(l.getId()); continue; }
			if (tc.getY() > max.getY()) { toRemove.add(l.getId()); continue; }
		}
		System.out.println("=> "+toRemove.size()+" links to remove.");
		for (Id id : toRemove) { network.removeLink(network.getLinks().get(id)); }
		System.out.println("=> "+network.getLinks().size()+" links left.");
		System.out.println("done. " + (new Date()));

		System.out.println("cleaning network... " + (new Date()));
		new NetworkCleaner().run(network);
		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////

	private static void reducePopulation(ScenarioImpl scenario) {
		System.out.println("removing persons containing links and/or facilities that are removed..." + (new Date()));
		Set<Id> linkIds = scenario.getNetwork().getLinks().keySet();
		Set<Id> facIds = scenario.getActivityFacilities().getFacilities().keySet();
		Set<Id> toRemove = new HashSet<Id>();
		for (Person p : scenario.getPopulation().getPersons().values()) {
			boolean removeIt = false;
			for (Plan plan : p.getPlans()) {
				for (PlanElement e : plan.getPlanElements()) {
					if (e instanceof ActivityImpl) {
						ActivityImpl a = (ActivityImpl)e;
						if ((a.getLinkId() != null) && (!linkIds.contains(a.getLinkId()))) { removeIt = true; }
						if ((a.getFacilityId() != null) && (!facIds.contains(a.getFacilityId()))) { removeIt = true; }
					}
					else if (e instanceof LegImpl) {
						LegImpl l = (LegImpl)e;
						l.setRoute(null);
					}
				}
			}
			if (scenario.getKnowledges().getKnowledgesByPersonId().get(p.getId()) != null) {
				for (ActivityOptionImpl ao : scenario.getKnowledges().getKnowledgesByPersonId().get(p.getId()).getActivities()) {
					if (!facIds.contains(ao.getFacility().getId())) { removeIt = true; }
				}
			}
			if (removeIt) { toRemove.add(p.getId()); }
		}
		System.out.println("=> "+toRemove.size()+" persons to remove.");
		for (Id id : toRemove) { scenario.getPopulation().getPersons().remove(id); }
		System.out.println("=> "+scenario.getPopulation().getPersons().size()+" persons left.");
		System.out.println("done. " + (new Date()));

		System.out.println("re-initializing initial routes...");
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		PreProcessLandmarks preProcessLandmarks = new PreProcessLandmarks(timeCostCalc);
		preProcessLandmarks.run(scenario.getNetwork());
		ReRouteLandmarks router = new ReRouteLandmarks(scenario.getConfig(), scenario.getNetwork(),timeCostCalc,timeCostCalc,preProcessLandmarks);
		router.prepareReplanning();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				router.handlePlan(plan);
			}
		}
		router.finishReplanning();
		Gbl.printMemoryUsage();
		System.out.println("done. (re-initializing initial routes)");
	}

	//////////////////////////////////////////////////////////////////////

	private static void reduceScenario(String[] args) {
		ScenarioImpl scenario = (ScenarioImpl) new ScenarioLoaderImpl(args[0]).loadScenario();
		calcExtent(scenario);
		if (args.length == 4) {
			Coord center = new CoordImpl(args[1],args[2]);
			double radius = Double.parseDouble(args[3]);

			reduceNetwork(scenario.getNetwork(),center,radius);
			reduceFacilities(scenario.getActivityFacilities(),scenario.getNetwork());
			reduceFacilities(scenario.getActivityFacilities(),center,radius);
			reducePopulation(scenario);
		}
		else { // args.length == 5
			Coord min = new CoordImpl(args[1],args[2]);
			Coord max = new CoordImpl(args[3],args[4]);

			reduceNetwork(scenario.getNetwork(),min,max);
			reduceFacilities(scenario.getActivityFacilities(),scenario.getNetwork());
			reduceFacilities(scenario.getActivityFacilities(),min,max);
			reducePopulation(scenario);
		}
		calcExtent(scenario);
		new NetworkWriter(scenario.getNetwork()).writeFile(scenario.getConfig().network().getOutputFile());
		new FacilitiesWriter(scenario.getActivityFacilities()).writeFile(scenario.getConfig().facilities().getOutputFile());
		new PopulationWriter(scenario.getPopulation(),scenario.getNetwork(), scenario.getKnowledges()).writeFile(scenario.getConfig().plans().getOutputFile());
	}

	//////////////////////////////////////////////////////////////////////

	private static void printUsage() {
		System.out.println();
		System.out.println("ScenarioCut");
		System.out.println();
		System.out.println("Usage1: ScenarioCut configfile centerX centerY radius");
		System.out.println("        Reduces a given Scenario according to a given circular area.");
		System.out.println("Usage2: ScenarioCut configfile minX minY maxX maxY");
		System.out.println("        Reduces a given Scenario according to a given rectangular area.");
		System.out.println("Usage3: ScenarioCut configfile [zrhCutC|zrhCutR]");
		System.out.println("        Reduces a given Scenario according to predefined cuts:");
		System.out.println("        zrhCutC := ScenarioCut configfile 683518 246836 30000 (30km radius around Bellevue)");
		System.out.println("        zrhCutR := ScenarioCut configfile 640000 200000 740000 310000 (Zurich greater region)");
		System.out.println();
		System.out.println("Note: config file should contain the following parameter:");
		System.out.println("      inputNetworkFile");
		System.out.println("      outputNetworkFile");
		System.out.println("      inputFacilitiesFile");
		System.out.println("      outputFacilitiesFile");
		System.out.println("      inputPlansFile");
		System.out.println("      outputPlansFile");
		System.out.println();
		System.out.println("Note: Eucledian coordinate system expected for the input scenario");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2009, matsim.org");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		if (args.length < 2) { printUsage(); return; }
		else if (args.length > 5) { printUsage(); return; }
		else if (args.length == 2) {
			if (args[1].trim().equals("zrhCutC")) {
				String [] args2 = {args[0],"683518","246836","30000"};
				reduceScenario(args2);
			}
			else if (args[1].trim().equals("zrhCutR")) {
				String [] args2 = {args[0],"640000","200000","740000","310000"};
				reduceScenario(args2);
			}
			else { printUsage(); return; }
		}
		else if (args.length == 3) { printUsage(); return; }
		else if (args.length == 4) {
			reduceScenario(args);
		}
		else { // args.length == 5
			reduceScenario(args);
		}
	}
}
