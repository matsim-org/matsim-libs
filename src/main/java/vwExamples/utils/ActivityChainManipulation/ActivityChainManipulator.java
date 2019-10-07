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

package vwExamples.utils.ActivityChainManipulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.tuple.Triple;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author saxer
 */
public class ActivityChainManipulator {
	HashSet<String> validPrimaryActs = new HashSet<String>();
	HashSet<String> bridgingActs = new HashSet<String>();
	String populationInputFile;
	String populationOutPutFile;
	HashMap<Id<Person>, ActivityClustersPerPerson> ActivityClustersPerPersonMap = new HashMap<Id<Person>, ActivityClustersPerPerson>();

	Map<String, Geometry> zoneMap = new HashMap<>();
	Set<String> zones = new HashSet<>();
	String shapeFile;
	String shapeFeature = "NO";
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	StreamingPopulationWriter popwriter;

	ActivityChainManipulator(String populationInputFile, String populationOutPutFile, String shapefile) {
		this.populationInputFile = populationInputFile;
		this.populationOutPutFile = populationOutPutFile;
		this.shapeFile = shapefile;

		this.popwriter = new StreamingPopulationWriter();
		popwriter.startStreaming(this.populationOutPutFile);

		validPrimaryActs.add("home");
		validPrimaryActs.add("work");
		validPrimaryActs.add("shopping");
		validPrimaryActs.add("other");
		validPrimaryActs.add("leisure");
		validPrimaryActs.add("education");

		// Bridging acts that are allowed to split a cluster of consecutive acts
		bridgingActs.add("shopping");
		bridgingActs.add("other");
		bridgingActs.add("leisure");
		bridgingActs.add("home");

		// Initialize shape file
		readShape();

		new PopulationReader(scenario).readFile(this.populationInputFile);

	}

	public static void main(String[] args) {

		ActivityChainManipulator manipulator = new ActivityChainManipulator(
				"D:\\\\Matsim\\\\Axer\\\\Hannover\\\\Base\\\\vw271_0.1_1\\\\vw271_0.1_1.output_plans.xml.gz",
				"D:\\\\Matsim\\\\Axer\\\\Hannover\\\\Base\\\\vw271_0.1_1\\\\vw271_0.1_1.output_plans_noMultiWork.xml.gz",
				"D:\\Matsim\\Axer\\Hannover\\ZIM\\input\\shp\\Hannover_Stadtteile.shp");

		manipulator.run();

	}

	public void run() {
		searchCandidates();
		manipulateCandidates();
		writePopulation();

	}

	public void writePopulation() {
		for (Person person : scenario.getPopulation().getPersons().values())

		{
			PersonUtils.removeUnselectedPlans(person);
			this.popwriter.writePerson(person);
		}

		this.popwriter.closeStreaming();
	}

	public void searchCandidates() {

		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (livesInsideShape(person)) {
				checkConsecutiveActOccurance(person, "work");
				// System.out.print(consecutiveActs);
			}

		}

	}

	public void manipulateCandidates() {

		int minClusterSize = 3;

		for (ActivityClustersPerPerson clustersPerPerson : ActivityClustersPerPersonMap.values()) {

			Id<Person> personId = clustersPerPerson.personId;

//			if (personId.toString().contains("41_12_89688101")) {
//				int hans = 9;
//			}

			if (clustersPerPerson.totalSize >= minClusterSize)

			{

				for (ActivityCluster cluster : clustersPerPerson.ActivityClusters) {
					int peStart = cluster.peStart;
					int peEnd = cluster.peEnd;

					Person personToBeModified = scenario.getPopulation().getPersons().get(personId);

					Plan plan = personToBeModified.getSelectedPlan();

					// Derive required times
					Activity endOfClusterAct = (Activity) plan.getPlanElements().get(peEnd);
					Activity StartOfClusterAct = (Activity) plan.getPlanElements().get(peStart);

					double clusterEndTime = PopulationUtils.getNextLeg(plan, endOfClusterAct).getDepartureTime();
					double clusterStartTime = PopulationUtils.getPreviousLeg(plan, StartOfClusterAct).getDepartureTime()
							+ PopulationUtils.getPreviousLeg(plan, StartOfClusterAct).getTravelTime();
					int totalDurationOfMergedAct = (int) ((clusterEndTime - clusterStartTime) / 3600.0);

					// Update activities
					StartOfClusterAct.setEndTime(clusterEndTime);
					StartOfClusterAct.setType("work_" + totalDurationOfMergedAct);
					StartOfClusterAct.setMaximumDuration(Time.UNDEFINED_TIME);

				}

				Person personToBeModified = scenario.getPopulation().getPersons().get(personId);

				Plan plan = personToBeModified.getSelectedPlan();

				// Delete planElements
				for (ActivityCluster cluster : clustersPerPerson.ActivityClusters) {
					// We delete the everything between end of first Act of the cluster, starting
					// with a leg...
					// Till end of the cluster, including the following leg
					// Exmaple: h--w--w--w--o--w--w--h
					// Exmaple: h--w--h
					int peStart = cluster.peStart + 1;
					int peEnd = cluster.peEnd + 1;

					// Rewrite Planelement to null
					int peIdx = 0;
					for (PlanElement pe : plan.getPlanElements()) {

						if (peIdx >= peStart && peIdx <= peEnd) {

							if (peIdx == peEnd) {
								Leg leg = (Leg) plan.getPlanElements().get(peIdx);
								leg.setRoute(null);
								leg.setDepartureTime(Time.UNDEFINED_TIME);
								leg.setTravelTime(Time.UNDEFINED_TIME);
							} else {
								plan.getPlanElements().set(peIdx, null);
							}
						}
						peIdx++;

					}


				}

				// Delete null from plan at the end. Otherwise we will shit indices to early
				for (Iterator<PlanElement> i = plan.getPlanElements().iterator(); i.hasNext();) {
					PlanElement pe = i.next();
					if (pe == null) {
						i.remove();
					}
				}
				System.out.println("Changed Agent "+personId.toString() );

			}

		}

	}

	public void readShape() {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(this.shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(this.shapeFeature).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}

	public boolean isWithinZone(Coord coord) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		for (String zone : this.zoneMap.keySet()) {
			Geometry geometry = this.zoneMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return true;
			}
		}

		return false;
	}

	public boolean livesInsideShape(Person person) {

		Plan plan = person.getSelectedPlan();

		for (PlanElement pe : plan.getPlanElements()) {

			if (pe instanceof Activity) {
				Activity act = (Activity) pe;

				if (act.getType().contains("home")) {
					Coord coord = act.getCoord();

					if (isWithinZone(coord)) {
						return true;
					}
				}
			}

		}

		return false;

	}

	public void checkConsecutiveActOccurance(Person person, String checkAct) {

		Plan plan = person.getSelectedPlan();
		ArrayList<String> consecutiveChain = new ArrayList<String>();
		ArrayList<String> activityChain = new ArrayList<String>();

		int primaryActIdx = 0;
		int peIdx = 0;

		Integer startPE = null;
		Integer endPE = null;

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {

				Activity activity = (Activity) pe;
				String act = activity.getType().toString().split("_")[0];
				if (this.validPrimaryActs.contains(act)) {

					if ((consecutiveChain.isEmpty()) && act.contains(checkAct)) {
						// Registers the first occurrence of checkAct
						consecutiveChain.add(act);
						startPE = peIdx;
					} else if (!activityChain.isEmpty() && activityChain.get(primaryActIdx - 1).contains(checkAct)
							&& act.contains(checkAct)) {
						consecutiveChain.add(act);
						endPE = peIdx;
					}
					// Registers any consecutive occurrence of checkAct

					primaryActIdx++;
					activityChain.add(act);

				}

				// Reached home activity save results till now and allow to find a new
				// agglomeration of checkAct
				if (bridgingActs.contains(act) && (peIdx > 1) && consecutiveChain.size() > 1) {

					if (ActivityClustersPerPersonMap.containsKey(person.getId())) {
						ActivityClustersPerPersonMap.get(person.getId())
								.addActivityCluster(new ActivityCluster(consecutiveChain.size(), startPE, endPE));
					} else {

						ActivityClustersPerPersonMap.put(person.getId(), new ActivityClustersPerPerson(person.getId()));
						ActivityClustersPerPersonMap.get(person.getId())
								.addActivityCluster(new ActivityCluster(consecutiveChain.size(), startPE, endPE));

					}

					// totalSumOfChainedActs=totalSumOfChainedActs+consecutiveChain.size();
					// System.out
					// .println("Person: "+person.getId().toString()+" totalSize:
					// "+totalSumOfChainedActs +" size: " + consecutiveChain.size() + " startPE: " +
					// startPE + " endPE: " + endPE);
					startPE = null;
					endPE = null;

					consecutiveChain.clear();

					// totalSumOfChainedActs=0;
				}

			}
			peIdx++;
		}

		// System.out.println(consecutiveChain.size() + " || " + activityChain);

	}
}
