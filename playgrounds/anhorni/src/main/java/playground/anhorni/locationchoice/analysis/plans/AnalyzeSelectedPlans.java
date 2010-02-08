/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzePlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.plans;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.geometry.CoordImpl;

public class AnalyzeSelectedPlans {

	private final ScenarioImpl scenario = new ScenarioImpl();
	private final PopulationImpl plans = scenario.getPopulation();
	private final ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
	private final NetworkImpl network = scenario.getNetwork();
	private String outfile = "output/plans/plan_analysis.txt";
	private final TripAnalyzer tripAnalyzer = new TripAnalyzer();
	private double normalizedWeightFactor = 1.0;
	String plansfilePath;

	private final static Logger log = Logger.getLogger(AnalyzeSelectedPlans.class);

	public static void main(final String[] args) {
		final AnalyzeSelectedPlans analyzer = new AnalyzeSelectedPlans();

		// arg 0: facilities file
		// arg 1: network file;
		// arg 2: plans file
		// arg 3: out file (+ path)
		// arg 4: weight by score
		analyzer.init(args[0], args[1], args[2], args[3], args[4]);
		analyzer.run();
	}

	public void run() {
		log.info("Analyzing selected plans ...");
		Gbl.startMeasurement();
		this.analyze();
		Gbl.printElapsedTime();
	}

	private void init(String facilitiesfilePath, String networkfilePath, String plansfilePath,
			String outfile, String weightByScore) {

		if (facilitiesfilePath.length() > 1) {
			log.info("reading the facilities ...");
			new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesfilePath);
		}

		log.info("reading the network ...");
		new MatsimNetworkReader(this.scenario).readFile(networkfilePath);

		log.info("  reading file " + plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
		plansReader.readFile(plansfilePath);

		this.plansfilePath = plansfilePath;
		this.outfile = outfile;

		boolean wgByScore = Boolean.valueOf(weightByScore);
		this.fillTrips(wgByScore);
	}

	private void fillTrips(boolean wgByScore) {
		int numberOfBorderCrossingPersons = 0;
		int numberOfPersons = 0;
		double numberOfWeightedLegs = 0.0;
		int numberOfLegs = 0;

		if (wgByScore) {
			this.computeNormalizedWeight();
			log.info("Weighted computation. Normalization factor: " + normalizedWeightFactor);
		}

		for (Person person : this.plans.getPersons().values()) {
			numberOfPersons++;

			// do not include border crossing traffic
			if (Integer.valueOf(person.getId().toString())>1000000000) {
				numberOfBorderCrossingPersons++;
				continue;
			}
			Plan selectedPlan = person.getSelectedPlan();
			List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 1; j < actslegs.size(); j = j+2) {
				final LegImpl leg = (LegImpl)actslegs.get(j);
				ActivityImpl nextAct = (ActivityImpl)actslegs.get(j + 1);

				numberOfLegs++;
				Trip trip = new Trip();
				trip.setPurpose(nextAct.getType().substring(0, 1));
				trip.setMode(leg.getMode());
				trip.setDuration(leg.getTravelTime());

				if (wgByScore)  {
					trip.setWeight(selectedPlan.getScore() * normalizedWeightFactor);
					numberOfWeightedLegs += trip.getWeight();
				}

				double distance = 0.0;
				// we do only have crowfly data for validation
				if (leg.getRoute().getStartLinkId() != null && true == false) {
				//if (leg.getRoute().getStartLink() != null) {
					double crowFlyDistance = ((CoordImpl)this.network.getLinks().get(leg.getRoute().getStartLinkId()).getCoord()).
					calcDistance(this.network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord());

					distance = Math.max(crowFlyDistance, leg.getRoute().getDistance());
				}
				else {
					ActivityImpl actPrevious = (ActivityImpl)actslegs.get(j-1);
					distance = ((CoordImpl)actPrevious.getCoord()).calcDistance(nextAct.getCoord());
				}
				trip.setDistance(distance);
				this.tripAnalyzer.addTrip(trip);
			}
		}
		log.info("Number of persons: " + numberOfPersons);
		log.info("Number of border-crossing persons: " + numberOfBorderCrossingPersons);
		log.info("Number of legs: " + numberOfLegs);
		log.info("Number of weighted legs: " + numberOfWeightedLegs);
	}

	private void computeNormalizedWeight() {
		int numberOfLegs = 0;
		double numberOfWeightedLegs = 0.0;

		for (Person person : this.plans.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 1; j < actslegs.size(); j = j+2) {
				numberOfLegs++;
				numberOfWeightedLegs += selectedPlan.getScore();
			}
		}
		this.normalizedWeightFactor = numberOfLegs / numberOfWeightedLegs;
	}

	private void analyze() {
		this.tripAnalyzer.analyze(outfile);
	}
}
