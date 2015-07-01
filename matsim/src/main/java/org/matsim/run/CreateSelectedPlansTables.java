/* *********************************************************************** *
 * project: org.matsim.*
 * CreateSelectedPlansTables.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.households.Households;
import org.matsim.lanes.data.v20.LaneDefinitions20;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * Creates a table
 * - containing the analysis of one or two plans files and
 * a table
 * - containing the average values of the selected plans of all persons
 *
 * @author anhorni
 */
public class CreateSelectedPlansTables {

	private Population plans0;
	private Population plans1;

	private final static String outfileTable="./output/analyseSelectedPlansTable.txt";
	private final static String outfileAverages="./output/analyseSelectedPlansTableAverages.txt";

	// true if there are two plans to evaluate (compare)
	private boolean twoPlans;
	private ScenarioImpl scenario;
//	private NetworkLayer network;

	private final double [] sumPlanTraveltime={0.0, 0.0};
	private final double [] sumPlanTraveldistance={0.0, 0.0};
	private final double [] sumLegTraveltime={0.0, 0.0};
	private final double [] sumLegTraveldistance={0.0, 0.0};
	private final int [] sumNrLegs={0, 0};



	/**
	 * @param args array with 2 or 3 entries:
	 * - path to network file: required
	 * - path to plans file 0: required
	 * - path to plans file 1: optional
	 */
	public static void main(final String[] args) {

		if (args.length < 2 || args.length > 3 ) {
			System.out.println("Too few arguments.");
			printUsage();
			System.exit(1);
		}

		Gbl.startMeasurement();
		final CreateSelectedPlansTables table=new CreateSelectedPlansTables();


		if (args.length==3) {
			table.setTwoPlans(true);
			table.run(args[0], args[1], args[2]);


		}
		else {
			table.setTwoPlans(false);
			table.run(args[0], args[1], "");

		}

		Gbl.printElapsedTime();
	}

	private void init(final String networkPath) {
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		this.plans0= ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		this.plans1= ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

		System.out.println("  reading the network...");
		new MatsimNetworkReader(this.scenario).readFile(networkPath);
	}

	private void readPlansFile(final String plansfilePath, final Population plans) {
		System.out.println("  reading file "+plansfilePath);
		final PopulationReader plansReader = new MatsimPopulationReader(new PseudoScenario(this.scenario, plans));
		plansReader.readFile(plansfilePath);
	}

	private void writeAvgFile() {

		try {

			final String header="plansfile_nr\tplantraveltime\tplantraveldistance\t" +
					"legtraveltime\tlegtraveldistance\tnumberoflegs";

			final BufferedWriter out = IOUtils.getBufferedWriter(outfileAverages);
			out.write(header);
			out.write('\n');

			final int nr_selectedplans=this.plans0.getPersons().size();

			int i_max=1;
			if (this.twoPlans) {
				i_max=2;
			}

			for (int i=0; i<i_max; i++) {
				out.write(i + "\t");
				out.write(this.sumPlanTraveltime[i]/nr_selectedplans+"\t");
				out.write(this.sumPlanTraveldistance[i]/nr_selectedplans+"\t");
				out.write(this.sumLegTraveltime[i]/nr_selectedplans+"\t");
				out.write(this.sumLegTraveldistance[i]/nr_selectedplans+"\t");
				out.write(this.sumNrLegs[i]/nr_selectedplans+"\t");
				out.write('\n');
				out.flush();
			}
			out.close();
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}


	private void writeSummaryFile() {
		try {

			String header="personid\tsex\tage\tlicense\tcaravail\temployed\thomex\thomey\thomelink\t" +
			"score0\tplantraveltime0\tplantraveldistance0\tnumberoftrips0\t";

			if (this.twoPlans) {
				header=header.concat("score1\tplantraveltime1\tplantraveldistance1\tnumberoftrips1");
			}


			final BufferedWriter out = IOUtils.getBufferedWriter(outfileTable);
			out.write(header);
			out.write('\n');

			for (final Id<Person> person_id : this.plans0.getPersons().keySet()) {

				// method person.toString() not appropriate
				out.write(person_id.toString()+"\t");
				final PersonImpl person=(PersonImpl) this.plans0.getPersons().get(person_id);
				out.write(person.getSex()+"\t");
				out.write(person.getAge()+"\t");
				out.write(person.getLicense()+"\t");
				out.write(person.getCarAvail()+"\t");
				if (person.isEmployed() != null)
					out.write((person.isEmployed() ? "yes" : "no"));
				out.write("\t");

				Plan selectedPlan = person.getSelectedPlan();
				PlanImpl selectedPlanImpl = (PlanImpl) selectedPlan;
				Activity firstActivity = selectedPlanImpl.getFirstActivity();
				Coord c = null;
				String link_id = "-";
				
				if (firstActivity.getType().substring(0,1).equals("h")) {
					// no home activity in the plan -> no home activity in the knowledge
					c = firstActivity.getCoord();
					link_id = firstActivity.getLinkId().toString();
				}
				
				if (c != null) {
					out.write(c.getX()+"\t");
					out.write(c.getY()+"\t");
				}
				else {
					out.write("-\t-\t");
				}
				out.write(link_id+"\t");

				// plan0 ----------------------------------------------
				out.write(selectedPlan.getScore()+"\t");
				out.write(this.getTravelTime(person)+"\t");
				this.sumPlanTraveltime[0]+=this.getTravelTime(person);

				out.write(this.getTravelDist(person)+"\t");
				this.sumPlanTraveldistance[0]+=this.getTravelDist(person);

				out.write(this.getNumberOfTrips(person)+"\t");
				this.sumNrLegs[0]+=this.getNumberOfTrips(person);
				this.sumLegTraveltime[0]+=(this.getTravelTime(person)/this.getNumberOfTrips(person));
				this.sumLegTraveldistance[0]+=(this.getTravelDist(person)/this.getNumberOfTrips(person));

				// plan1 ----------------------------------------------
				if (this.twoPlans) {

					final Person person_comp=this.plans1.getPersons().get(person_id);
					out.write(person_comp.getSelectedPlan().getScore()+"\t");
					out.write(this.getTravelTime(person_comp)+"\t");
					this.sumPlanTraveltime[1]+=this.getTravelTime(person_comp);

					out.write(this.getTravelDist(person_comp)+"\t");
					this.sumPlanTraveldistance[1]+=this.getTravelDist(person_comp);

					out.write(this.getNumberOfTrips(person_comp)+"\t");
					this.sumNrLegs[1]+=this.getNumberOfTrips(person_comp);
					this.sumLegTraveltime[1]+=(this.getTravelTime(person_comp)/this.getNumberOfTrips(person_comp));
					this.sumLegTraveldistance[1]+=(this.getTravelDist(person_comp)/this.getNumberOfTrips(person_comp));
				}
				out.write('\n');
				out.flush();
			}
			out.close();
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/*  TODO [AH] Combine the following three methods and make one method.
	 *  See playground.anhorni.locationchoice.analysis.PersonTimeDistanceCalculator */

	private double getTravelTime(final Person person) {
		double travelTime=0.0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				travelTime+=((Leg) pe).getTravelTime();
			}
		}
		return travelTime;
	}

	private double getTravelDist(final Person person) {
		double travelDist=0.0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				travelDist+=((Leg) pe).getRoute().getDistance();
			}
		}
		return travelDist;
	}

	private int getNumberOfTrips(final Person person) {
		int numberOfLegs=0;
		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				numberOfLegs++;
			}
		}
		return numberOfLegs;
	}

	// --------------------------------------------------------------------------

	private void run(final String networkPath, final String plansfilePath0, final String plansfilePath1) {
		this.init(networkPath);
		this.readPlansFile(plansfilePath0, this.plans0);

		System.out.println(this.twoPlans);

		if (this.twoPlans) {
			this.readPlansFile(plansfilePath1, this.plans1);
		}

		writeSummaryFile();
		writeAvgFile();
		System.out.println("finished");
	}

	private static void printUsage() {
		System.out.println();
		System.out.println("CreateSelectedPlansTables:");
		System.out.println();
		System.out.println( "Creates an agent-based table including all agent \n" +
							"attributes, the selected plan score and the plan and \n" +
							"leg travel times and distances and \n" +
							"another table containing the aggregated values.");
		System.out.println();
		System.out.println("usage: CompareSelectedPlansTable args");
		System.out.println(" arg 0: path to network file (required)");
		System.out.println(" arg 1: path to plans file 0 (required)");
		System.out.println(" arg 2: path to plans file 1 (optional)");

		System.out.println("----------------");
		System.out.println("2008, matsim.org");
		System.out.println();
	}

	public void setTwoPlans(final boolean twoPlans) {
		this.twoPlans=twoPlans;
	}

	/**
	 * Provides a real scenario, but exchanges the population.
	 * Still, network and facilities can be reused that way.
	 *
	 * @author mrieser
	 */
	private static class PseudoScenario implements Scenario {

		private final Scenario scenario;
		private final Population myPopulation;

		public PseudoScenario(final Scenario scenario, final Population population) {
			this.scenario = scenario;
			this.myPopulation = population;
		}

		@Override
		public Population getPopulation() {
			return this.myPopulation;
		}

		@Override
		public Coord createCoord(double x, double y) {
			return this.scenario.createCoord(x, y);
		}

		@Override
		public Config getConfig() {
			return this.scenario.getConfig();
		}

		@Override
		public Network getNetwork() {
			return this.scenario.getNetwork();
		}

		@Override
		public ActivityFacilities getActivityFacilities() {
			return this.scenario.getActivityFacilities();
		}

		@Override
		public TransitSchedule getTransitSchedule() {
			return scenario.getTransitSchedule();
		}

		@Override
		public void addScenarioElement(String name, Object o) {
			scenario.addScenarioElement(name, o);
		}

		@Override
		public Object removeScenarioElement(String name) {
			return scenario.removeScenarioElement(name);
		}

		@Override
		public Object getScenarioElement(String name) {
			return scenario.getScenarioElement(name);
		}

		@Override
		public Vehicles getTransitVehicles() {
			return scenario.getTransitVehicles();
		}

		@Override
		public Households getHouseholds() {
			return scenario.getHouseholds();
		}

		@Override
		public LaneDefinitions20 getLanes() {
			return scenario.getLanes();
		}

		@Override
		public Vehicles getVehicles() {
			return scenario.getVehicles() ;
		}

	}
}
