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

package playground.johannes.gsv.sim.cadyts;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.collections.ChoiceSet;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.*;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.gis.ZoneGeoJsonIO;
import playground.johannes.synpop.matrix.HashMatrix;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.NumericMatrixXMLReader;

import javax.inject.Provider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class ODBruteForceCalibrator {

	private static final Logger logger = Logger.getLogger(ODBruteForceCalibrator.class);

	private final NumericMatrix refMatrix;

	private final ZoneCollection zones;

	private final ActivityFacilities facilities;

	private final String zoneKey = "gsvId";

	private Map<String, Map<String, List<ActivityFacility>>> facilities2Zones;

	private final Random random = new XORShiftRandom();

	private final TripRouter router;

	private final double distThreshold;

	public ODBruteForceCalibrator(NumericMatrix refMatrix, Scenario scenario, ZoneCollection zones, TripRouter router, double distThreshold) {
		this.refMatrix = refMatrix;
		this.zones = zones;
		this.router = router;
		this.facilities = scenario.getActivityFacilities();
		this.distThreshold = distThreshold;

		initFacilityData();
	}

	private void initFacilityData() {
		logger.info("Initializing facility data...");
		facilities2Zones = new HashMap<>();

		ProgressLogger.init(facilities.getFacilities().values().size(), 2, 10);
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			for (ActivityOption opt : facility.getActivityOptions().values()) {
				Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
				if (zone != null) {
					String zoneId = zone.getAttribute(zoneKey);

					Map<String, List<ActivityFacility>> typeFacilities = facilities2Zones.get(zoneId);

					if (typeFacilities == null) {
						typeFacilities = new HashMap<>();
						facilities2Zones.put(zoneId, typeFacilities);
					}

					List<ActivityFacility> facilityList = typeFacilities.get(opt.getType());
					if (facilityList == null) {
						facilityList = new ArrayList<>(1000);
						typeFacilities.put(opt.getType(), facilityList);
					}

					facilityList.add(facility);
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
	}

	public void run(Population population) {
		HashMatrix<String, ODRelation> simMatrix = plans2Matrix(population, zones, facilities, distThreshold, zoneKey);

		DistanceCalculator distCalc = CartesianDistanceCalculator.getInstance();
		Discretizer disc = new LinearDiscretizer(50000);

		Set<Zone> zoneSet = zones.getZones();
		for (Zone origin : zoneSet) {
			processZone(zoneSet, origin, distCalc, disc, simMatrix);
		}
	}

	private void processZone(Set<Zone> zoneSet, Zone origin, DistanceCalculator distCalc, Discretizer disc, HashMatrix<String, ODRelation> simMatrix) {
		/*
		 * get od-pairs above distance threshold
		 */
		Map<Double, Set<Tuple<String, String>>> relations = getODPairs(zoneSet, origin, distCalc, disc);

		for (Entry<Double, Set<Tuple<String, String>>> entry : relations.entrySet()) {

			double diffSum = 0;
			int cnt = 0;

			Set<Tuple<String, String>> set = entry.getValue();
			Map<Tuple<String, String>, Double> diffs = new HashMap<>();
			for (Tuple<String, String> odPair : set) {

				Double refVol = refMatrix.get(odPair.getFirst(), odPair.getSecond());
				if (refVol != null) {
					double simVol = 0.0;
					ODRelation relation = simMatrix.get(odPair.getFirst(), odPair.getSecond());
					if (relation != null) {
						simVol = relation.getPlans().size();
					}

					double diff = (simVol - refVol);
					diffSum += diff;
					cnt++;

					diffs.put(odPair, diff);
				}
			}

			double avrDiff = diffSum / (double) cnt;

			Map<Tuple<String, String>, Double> highODs = new HashMap<>();
			Map<Tuple<String, String>, Double> lowODs = new HashMap<>();

			for (Entry<Tuple<String, String>, Double> odPair : diffs.entrySet()) {

				double diff = odPair.getValue();
				double diff2 = diff - avrDiff;
				if (diff2 > 0) {
					highODs.put(odPair.getKey(), diff2);
				} else if (diff2 < 0) {
					lowODs.put(odPair.getKey(), diff2);
				}
			}

			Random random = new XORShiftRandom();

			ChoiceSet<Tuple<String, String>> lowODKeys = new ChoiceSet<>(random);
			for (Entry<Tuple<String, String>, Double> e : lowODs.entrySet()) {
				lowODKeys.addOption(e.getKey(), Math.abs(e.getValue()));// check
																		// negative
																		// weights
			}

			for (Entry<Tuple<String, String>, Double> highOd : highODs.entrySet()) {
				int highCnt = highOd.getValue().intValue();
				ODRelation fromOD = simMatrix.get(highOd.getKey().getFirst(), highOd.getKey().getSecond());
				if (fromOD != null) {
					boolean done = false;
					int fails = 0;
					while (!done) {
						Tuple<String, String> od = lowODKeys.randomWeightedChoice();
						if (od == null) {
							logger.info("No OD pairs left.");
							done = true;
						} else {
							ODRelation toOD = simMatrix.get(od.getFirst(), od.getSecond());
							if (toOD == null) {
								toOD = new ODRelation(od.getFirst(), od.getSecond());
								simMatrix.set(od.getFirst(), od.getSecond(), toOD);
							}
							if (shift(fromOD, toOD)) {
								logger.info(String.format("Shifted 1 person from %s to %s.", fromOD.getFromId(), od.getSecond()));
								highCnt--;
								if (highCnt <= 0)
									done = true;
							} else {
								fails++;
								if (fails > 100) {
									lowODKeys.removeOption(od);
									logger.info("Failed shifting for 100 times. Removing OD from choice set.");
								}
							}
						}
					}
				} else {
					logger.info("No volume to shift.");
				}
			}

		}
	}

	private Map<Double, Set<Tuple<String, String>>> getODPairs(Set<Zone> zoneSet, Zone origin, DistanceCalculator distCalc, Discretizer disc) {
		Map<Double, Set<Tuple<String, String>>> relations = new HashMap<>();

		for (Zone dest : zoneSet) {
			if (origin != dest) {
				double d = distCalc.distance(origin.getGeometry().getCentroid(), dest.getGeometry().getCentroid());
				d = disc.discretize(d);
				Set<Tuple<String, String>> rSet = relations.get(d);
				if (rSet == null) {
					rSet = new HashSet<>();
					relations.put(d, rSet);
				}
				rSet.add(new Tuple<>(origin.getAttribute(zoneKey), dest.getAttribute(zoneKey)));
			}
		}

		return relations;
	}

	private boolean shift(ODRelation fromOD, ODRelation toOD) {
		int planIdx = random.nextInt(fromOD.getPlans().size());
		Plan plan = fromOD.getPlans().get(planIdx);
		int legIdx = fromOD.getLegIndices().get(planIdx);
		int actIdx = legIdx + 1;
		ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(actIdx);

		if (!act.getType().equalsIgnoreCase(ActivityTypes.HOME)) {
			List<ActivityFacility> zoneFacilities = facilities2Zones.get(toOD.getToId()).get(act.getType());
			ActivityFacility newFac = zoneFacilities.get(random.nextInt(zoneFacilities.size()));

			act.setFacilityId(newFac.getId());
			act.setLinkId(newFac.getLinkId());
			/*
			 * route outward trip
			 */
			if (actIdx > 1) {
				Activity prev = (Activity) plan.getPlanElements().get(actIdx - 2);
				ActivityFacility source = facilities.getFacilities().get(prev.getFacilityId());
				ActivityFacility target = newFac;

				Leg toLeg = (Leg) plan.getPlanElements().get(legIdx);
				List<? extends PlanElement> stages = router.calcRoute(toLeg.getMode(), source, target, prev.getEndTime(), plan.getPerson());
				if (stages.size() > 1) {
					throw new UnsupportedOperationException();
				}
				plan.getPlanElements().set(legIdx, stages.get(0));
			}
			/*
			 * route return trip
			 */
			if (actIdx < plan.getPlanElements().size() - 1) {
				Activity next = (Activity) plan.getPlanElements().get(actIdx + 2);
				ActivityFacility target = facilities.getFacilities().get(next.getFacilityId());
				ActivityFacility source = newFac;

				Leg fromLeg = (Leg) plan.getPlanElements().get(legIdx + 2);
				List<? extends PlanElement> stages = router.calcRoute(fromLeg.getMode(), source, target, act.getEndTime(), plan.getPerson());
				if (stages.size() > 1) {
					throw new UnsupportedOperationException();
				}
				plan.getPlanElements().set(legIdx + 2, stages.get(0));
			}
			// releaseTripRouter(router);

			fromOD.remove(planIdx);
			toOD.add(plan, legIdx);

			return true;
		} else {
			return false;
		}
	}

	private HashMatrix<String, ODRelation> plans2Matrix(Population pop, ZoneCollection zones, ActivityFacilities facilities, double distThreshold,
												String zoneIdKey) {
		HashMatrix<String, ODRelation> m = new HashMatrix<>();

		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		// Map<Tuple<String, String>, ODRelation> odPairs = new HashMap<>();

		ProgressLogger.init(pop.getPersons().size(), 2, 10);

		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			for (int i = 1; i < plan.getPlanElements().size(); i += 2) {
				Leg leg = (Leg) plan.getPlanElements().get(i);

				if (leg.getMode().equalsIgnoreCase("car")) {

					Activity orig = (Activity) plan.getPlanElements().get(i - 1);
					Activity dest = (Activity) plan.getPlanElements().get(i + 1);

					Id<ActivityFacility> origId = Id.create(orig.getFacilityId(), ActivityFacility.class);
					ActivityFacility origFac = facilities.getFacilities().get(origId);

					Id<ActivityFacility> destId = Id.create(dest.getFacilityId(), ActivityFacility.class);
					ActivityFacility destFac = facilities.getFacilities().get(destId);

					Point origPoint = MatsimCoordUtils.coordToPoint(origFac.getCoord());
					Point destPoint = MatsimCoordUtils.coordToPoint(destFac.getCoord());

					Zone origZone = zones.get(origPoint.getCoordinate());
					Zone destZone = zones.get(destPoint.getCoordinate());

					if (origZone != null && destZone != null) {
						double d = dCalc.distance(origPoint, destPoint);
						if (d >= distThreshold) {
							String id_i = origZone.getAttribute(zoneIdKey);
							String id_j = destZone.getAttribute(zoneIdKey);
							// Double val = m.get(id_i, id_j);
							// if (val == null)
							// val = 0.0;
							// val++;
							// m.set(id_i, id_j, val);

							// Tuple<String, String> odKey = new Tuple<String,
							// String>(id_i, id_j);
							ODRelation relation = m.get(id_i, id_j);
							if (relation == null) {
								relation = new ODRelation(id_i, id_j);
								m.set(id_i, id_j, relation);
							}

							relation.add(plan, i);

						}
					}
				}
			}
			ProgressLogger.step();
		}

		ProgressLogger.terminate();

		return m;
	}

	private static class ODRelation {

		private final String from;

		private final String to;

		// private double volume;

		private List<Plan> plans;

		private List<Integer> legIndices;

		public ODRelation(String from, String to) {
			this.from = from;
			this.to = to;

			plans = new ArrayList<>(5000);
			legIndices = new ArrayList<>(5000);
		}

		public void add(Plan plan, int legIndex) {
			plans.add(plan);
			legIndices.add(legIndex);
		}

		public void remove(int idx) {
			plans.remove(idx);
			legIndices.remove(idx);
		}

		public String getToId() {
			return to;
		}

		public String getFromId() {
			return from;
		}

		public List<Plan> getPlans() {
			return plans;
		}

		public List<Integer> getLegIndices() {
			return legIndices;
		}
	}

	public static void main(String args[]) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());
		netReader.readFile("/home/johannes/gsv/ger/data/network.xml.gz");

		MatsimFacilitiesReader facReader = new MatsimFacilitiesReader(scenario);
		facReader.readFile("/home/johannes/gsv/ger/data/facilities.xml.gz");

		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile("/home/johannes/gsv/ger/data/plans.xml.gz");

		logger.info("Connecting facilities to links...");
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			Coord coord = facility.getCoord();
			Link link = NetworkUtils.getNearestLink(network, coord);
			((ActivityFacilityImpl) facility).setLinkId(link.getId());
		}

		FreespeedTravelTimeAndDisutility tt = new FreespeedTravelTimeAndDisutility(1, 0, 0);
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		builder.setTravelDisutility(tt);
		builder.setTravelTime(tt);

		Provider<TripRouter> factory = builder.build(scenario);
		TripRouter router = factory.get();

		NumericMatrixXMLReader reader = new NumericMatrixXMLReader();
		reader.setValidating(false);
		reader.parse("/home/johannes/gsv/matrices/refmatrices/tomtom.de.xml");
		NumericMatrix refMatrix = reader.getMatrix();
		MatrixOperations.applyFactor(refMatrix, 1 / 16.0);
		MatrixOperations.applyFactor(refMatrix, 1 / (4 * 11.8));

		ZoneCollection zones = new ZoneCollection(null);
		String data = new String(Files.readAllBytes(Paths.get("/home/johannes/gsv/gis/nuts/de.nuts3.gk3.geojson")));
		zones.addAll(ZoneGeoJsonIO.parseFeatureCollection(data));
		zones.setPrimaryKey("gsvId");
		data = null;

		removeEntries(refMatrix, zones, 100000);

		ODBruteForceCalibrator calibrator = new ODBruteForceCalibrator(refMatrix, scenario, zones, router, 100000);
		calibrator.run(scenario.getPopulation());
	}

	private static void removeEntries(NumericMatrix m, ZoneCollection zones, double distThreshold) {
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		Set<String> keys = m.keys();
		int cnt = 0;
		for (String i : keys) {
			for (String j : keys) {
				Zone zone_i = zones.get(i);
				Zone zone_j = zones.get(j);

				if (zone_i != null && zone_j != null) {
					Point pi = zone_i.getGeometry().getCentroid();
					Point pj = zone_j.getGeometry().getCentroid();

					double d = dCalc.distance(pi, pj);

					if (d < distThreshold) {
						Double val = m.get(i, j);
						if (val != null) {
							m.set(i, j, null);
							cnt++;
						}
					}
				}
			}
		}

		logger.info(String.format("Removed %s trips with less than %s KM.", cnt, distThreshold / 1000.0));
	}
}
