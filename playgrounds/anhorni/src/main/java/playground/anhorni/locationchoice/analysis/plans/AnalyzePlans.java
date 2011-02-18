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

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.anhorni.locationchoice.preprocess.helper.BinsOld;
import playground.anhorni.utils.Utils;

public class AnalyzePlans {

	private final ScenarioImpl scenario = new ScenarioImpl();
	private final Population plans = scenario.getPopulation();
	private final ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
	private final NetworkImpl network = scenario.getNetwork();

	String plansfilePath;

	private final DecimalFormat formatter = new DecimalFormat("0.0");
	private final static Logger log = Logger.getLogger(AnalyzePlans.class);


	public static void main(final String[] args) {
		final AnalyzePlans analyzer = new AnalyzePlans();
		analyzer.init(args[0], args[1], args[2]);
		analyzer.run(args[2]);
	}

	public void run(String plansfilePath) {
		log.info("Analyzig plans ...");
		Gbl.startMeasurement();
		this.plansfilePath = plansfilePath;
		String outpath = "output/plans/analysis/";
		this.analyze1(outpath);
		this.analyzeDesiredDuration("shop", outpath);
		this.analyzeDesiredDuration("leisure", outpath);
		this.analyze3(outpath);
		this.analyze4(outpath);
		this.analyzeLeisureDistancesCar(outpath, "leisure");
		this.analyzeLeisureDistancesCar(outpath, "shop");
		this.analyzeLeisureDistancesCar(outpath, "work");
		this.analyzeLeisureDistancesCar(outpath, "education");
		this.analyzeLeisureDistances(outpath);
		this.analyzeTripSharesAccording2MZ(outpath);
		Gbl.printElapsedTime();
	}

	private void init(String networkfilePath, String facilitiesfilePath, String plansfilePath) {

		log.info("reading the facilities ...");
		new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesfilePath);

		log.info("reading the network ...");
		new MatsimNetworkReader(this.scenario).readFile(networkfilePath);

		log.info("  reading file " + plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
		plansReader.readFile(plansfilePath);
	}


	private void analyze1(String outpath) {

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "plan_activities_summary.txt");

			int numberOfShoppingActs = 0;
			double totalDesiredShoppingDuration = 0.0;
			int numberOfLeisureActs = 0;
			double totalDesiredLeisureDuration = 0.0;
			int numberOfWorkActs = 0;
			int numberOfEducationActs = 0;

			Iterator<? extends Person> person_it = this.plans.getPersons().values().iterator();
			while (person_it.hasNext()) {
				Person person = person_it.next();
				Plan selectedPlan = person.getSelectedPlan();

				int numberOfShoppingActsPerPerson = 0;
				int numberOfLeisureActsPerPerson = 0;
				double desiredShopPerPerson = 0.0;
				double desiredLeisurePerPerson = 0.0;

				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);
					if (act.getType().startsWith("shop")) {
						numberOfShoppingActs++;
						desiredShopPerPerson += ((PersonImpl) person).getDesires().getActivityDuration("shop");
						numberOfShoppingActsPerPerson++;
					}
					else if (act.getType().startsWith("leisure")) {
						numberOfLeisureActs++;
						desiredLeisurePerPerson += ((PersonImpl) person).getDesires().getActivityDuration("leisure");
						numberOfLeisureActsPerPerson++;
					}
					else if (act.getType().startsWith("work")) {
						numberOfWorkActs++;
					}
					else if (act.getType().startsWith("education")) {
						numberOfEducationActs++;
					}
				}
				if (numberOfShoppingActsPerPerson > 0) {
					totalDesiredShoppingDuration += (desiredShopPerPerson / numberOfShoppingActsPerPerson);
				}
				if (numberOfLeisureActsPerPerson > 0) {
					totalDesiredLeisureDuration += (desiredLeisurePerPerson / numberOfLeisureActsPerPerson);
				}
			}
			out.write("Plans file: " + this.plansfilePath +"\n");
			out.write("Number of shopping activities: \t" + numberOfShoppingActs + "\n");
			out.write("Total desired duration of shopping activities: \t" +
					formatter.format(1/3600.0 * totalDesiredShoppingDuration) + " [h] \n");
			out.write("Avg. desired shopping duration: \t" +
					formatter.format(1/3600.0 * (totalDesiredShoppingDuration / numberOfShoppingActs)) + " [h] \n");
			out.newLine();
			out.write("Number of leisure activities: \t" + numberOfLeisureActs + "\n");
			out.write("Total desired duration of leisure activities: \t" +
					formatter.format(1/3600.0 * totalDesiredLeisureDuration) + " [h] \n");
			out.write("Avg. desired leisure duration: \t" +
					formatter.format(1/3600.0 * (totalDesiredLeisureDuration / numberOfLeisureActs)) + " [h] \n\n");

			out.write("Number of work activities: \t" + numberOfWorkActs + "\n");
			out.write("Number of education activities: \t" + numberOfEducationActs + "\n");
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void analyzeTripSharesAccording2MZ(String outpath) {

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "plan_trips_summary.txt");

			int numberOfShoppingTrips = 0;
			int numberOfLeisureTrips = 0;
			int numberOfWorkTrips = 0;
			int numberOfEducationTrips = 0;
			int numberOfReturningTrips = 0;

			for (Person person : this.plans.getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();

				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 2; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);

					String actType = act.getType();

					if (actType.startsWith("home")) {
						numberOfReturningTrips++;
						actType = Utils.getActType(selectedPlan, act);
					}

					if (actType.startsWith("shop")) {
						numberOfShoppingTrips++;
					}
					else if (actType.startsWith("leisure")) {
						numberOfLeisureTrips++;
					}
					else if (actType.startsWith("work")) {
						numberOfWorkTrips++;
					}
					else if (actType.startsWith("education")) {
						numberOfEducationTrips++;
					}
					else {
						log.info("Something wrong ...");
					}
				}
			}
			out.write("Plans file: " + this.plansfilePath +"\n");
			out.write("Number of shopping trips: " +  numberOfShoppingTrips  + "\n");
			out.write("Number of leisure trips: " +  numberOfLeisureTrips  + "\n");
			out.write("Number of work trips: " +  numberOfWorkTrips  + "\n");
			out.write("Number of education trips: " +  numberOfEducationTrips  + "\n");
			out.write("Number of returning home trips: " +  numberOfReturningTrips  + "\n");

			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void analyzeDesiredDuration(final String type, String outpath) {
		try {
			final String header="Person_id\tActDuration\tDesiredDuration";
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "personActDurations_" + type + ".txt");
			final BufferedWriter outSummary = IOUtils.getBufferedWriter(outpath + "/summary_"+type+".txt");
			out.write(header);
			out.newLine();
			int numberOfPersonsDoingType = 0;

			for (Person person : this.plans.getPersons().values()) {
				boolean personSet = false;

				Plan selectedPlan = person.getSelectedPlan();

				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);
					if (act.getType().startsWith(type)) {
						out.write(person.getId().toString() + "\t" +
								String.valueOf(act.getMaximumDuration()) + "\t" +
								((PersonImpl) person).getDesires().getActivityDuration(type));
						out.newLine();

						if (!personSet) {
							numberOfPersonsDoingType++;
							personSet = true;
						}
					}
				}
				out.flush();
			}
			out.close();

			outSummary.write("Number of persons doing " + type + " :\t" + numberOfPersonsDoingType + "\n");
			outSummary.flush();
			outSummary.close();
			}
			catch (final IOException e) {
				Gbl.errorMsg(e);
			}
		}

	private void analyze3(String outpath) {

		try {
			final String header="Person_id\tnumberOfIntermediateShopandLeisureActs";
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "actchainsplananalysis.txt");
			out.write(header);
			out.newLine();

			for (Person person : this.plans.getPersons().values()) {
				Plan selectedPlan = person.getSelectedPlan();
				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();

				int countSL = 0;
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);
					if (act.getType().startsWith("s") || act.getType().startsWith("l")) {
						countSL++;
					}
					else if (act.getType().startsWith("h") || act.getType().startsWith("w")||
							act.getType().startsWith("e")) {
						if (countSL > 0) {
							out.write(person.getId().toString()+"\t"+String.valueOf(countSL)+"\n");
							countSL = 0;
						}
					}
				}
			}
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void analyze4(String outpath) {
		int countPrim = 0;
		int numberOfPersonsDoingSL = 0;
		int numberOfTrips = 0;

		for (Person person : this.plans.getPersons().values()) {
			boolean personSet = false;

			Plan selectedPlan = person.getSelectedPlan();
			List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().startsWith("s")) {
					if (!personSet) {
						numberOfPersonsDoingSL++;
						personSet = true;
					}
				}
				else if (act.getType().startsWith("l")) {
					if (!personSet) {
						numberOfPersonsDoingSL++;
						personSet = true;
					}
				}
				else if (act.getType().startsWith("h") || act.getType().startsWith("w")||
						act.getType().startsWith("e")) {
					countPrim++;
				}
			}
			numberOfTrips += (actslegs.size()-1)/2;
		}

		try {
			final BufferedWriter out = IOUtils.getBufferedWriter(outpath + "summary2.txt");
			out.write("Total number of Primary Activities: \t"+ countPrim + "\n");
			out.write("Number of Persons Doing Shop or Leisure: \t"+ numberOfPersonsDoingSL + "\n");

			double avgNumberOfTrips = numberOfTrips/(double)this.plans.getPersons().size();
			out.write("Number of persons: \t" + this.plans.getPersons().size() + "\n");
			out.write("Avg number of trips per person: \t" + avgNumberOfTrips);
			out.flush();
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void analyzeLeisureDistancesCar(String outpath, String type) {

		int cntAssigned = 0;
		double distAssigned = 0;
		int cnt = 0;
		double dist = 0;
		int cntWayThere = 0;
		double distWayThere = 0;

		Iterator<? extends Person> person_iter = this.plans.getPersons().values().iterator();
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			Plan selectedPlan = person.getSelectedPlan();
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
			ActivityImpl actPre = (ActivityImpl)actslegs.get(0);

			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);

				if (j > 0) {
					if (act.getType().startsWith(type + "_") && act.getType().startsWith("leisure")) {
						distAssigned += Double.parseDouble(act.getType().substring(8));
						cntAssigned++;
					}
					if (Utils.getActType(selectedPlan, act).startsWith(type)) {
						dist += (int)Math.round(((CoordImpl)actPre.getCoord()).calcDistance(act.getCoord()));
						cnt++;
					}
					if (act.getType().startsWith(type)) {
						distWayThere += (int)Math.round(((CoordImpl)actPre.getCoord()).calcDistance(act.getCoord()));
						cntWayThere++;
					}
				}
				actPre = act;
			}
		}
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outpath + this.plansfilePath + "_" + type +"_assigned_distance.txt");
			if (cntAssigned > 0) {
				out.write(type + ": average assigned distance: "  + distAssigned / cntAssigned +"\n");
			}
			out.write(type + ": average distance crowfly: "  + dist / cnt + "\n");
			out.write(type + ": average distance HINWEGE: "  + distWayThere / cntWayThere);
			out.flush();
			out.close();


		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void analyzeLeisureDistances(String outpath) {

		int carDistanceUnit = 1000;
		int ptDistanceUnit = 1000;
		int bikeDistanceUnit = 500;
		int walkDistanceUnit = 200;

		int carMaxDistance = 100 * 1000;
		int ptMaxDistance = 100 * 1000;
		int bikeMaxDistance = 50 * 1000;
		int walkMaxDistance = 20 * 1000;

		TreeMap<String, BinsOld> distanceBins = new TreeMap<String, BinsOld>();
		distanceBins.put("car", new BinsOld((carDistanceUnit), carMaxDistance));
		distanceBins.put("pt", new BinsOld((ptDistanceUnit), ptMaxDistance));
		distanceBins.put("bike", new BinsOld((bikeDistanceUnit), bikeMaxDistance));
		distanceBins.put("walk", new BinsOld((walkDistanceUnit), walkMaxDistance));

		Iterator<? extends Person> person_iter = this.plans.getPersons().values().iterator();
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			Plan selectedPlan = person.getSelectedPlan();
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();

			ActivityImpl actPre = (ActivityImpl)actslegs.get(0);

			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);

				if (act.getType().startsWith("leisure")) {

					if (j + 1 < actslegs.size()) {  // see figure above
						final LegImpl leg = (LegImpl)actslegs.get(j+1);

						if (!leg.getMode().equals(TransportMode.ride)) {
							int dist = (int)Math.round(((CoordImpl)actPre.getCoord()).calcDistance(act.getCoord()));
							distanceBins.get(leg.getMode()).addVal(dist, 1.0);
						}
					}
				}
				actPre = act;
			}
		}
//		DecimalFormat formatter = new DecimalFormat("0.00");

		String [] modes = {"pt", "car", "bike", "walk"};
		int [] distanceUnits = {ptDistanceUnit, carDistanceUnit, bikeDistanceUnit, walkDistanceUnit};
		for (int i = 0; i < 4; i++) {
			String [] categories  = new String[distanceBins.get(modes[i]).getSizes().length];
			for (int j = 0; j < categories.length; j++) {
				categories[j] = Integer.toString(j);
			}
			BarChart chartDistancesLeisureSize = new BarChart(modes[i] + " : leisure trip distances",
					"distance bin [" + distanceUnits[i]/1000.0 + "km]", "seize", categories);
			chartDistancesLeisureSize.addSeries(
					"Seize per bin", distanceBins.get(modes[i]).getSizes());
			chartDistancesLeisureSize.saveAsPng(outpath  + "/" + modes[i] + "_leisure" + " bin unit = " +
					formatter.format(distanceUnits[i]/1000.0) + "_TripDistances.png", 1600, 800);
		}
		for (int i = 0; i < 4; i++) {

			try {
				BufferedWriter outLeisure = IOUtils.getBufferedWriter(outpath + modes[i]
				        + "_leisure" + " bin unit = " +
						formatter.format(distanceUnits[i]/1000.0) + "_TripDistances.txt");
				outLeisure.write("Distance bin [" + distanceUnits[i]/1000.0 + "km]" + "\tSize\n");
				for (int j = 0; j < distanceBins.get(modes[i]).getMedian().size();  j++) {
					outLeisure.write(j + "\t" + distanceBins.get(modes[i]).getSizes()[j] + "\n");
				}
				outLeisure.flush();
				outLeisure.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	public Population getPlans() {
		return plans;
	}

	public ActivityFacilitiesImpl getFacilities() {
		return facilities;
	}

	public NetworkImpl getNetwork() {
		return network;
	}

}
