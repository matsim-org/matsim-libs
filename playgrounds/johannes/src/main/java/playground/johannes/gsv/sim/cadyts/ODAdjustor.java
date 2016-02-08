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
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TDoubleIntHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.HashMatrix;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author johannes
 * 
 */
public class ODAdjustor {

	private static final Logger logger = Logger.getLogger(ODAdjustor.class);

	public static final String ZONE_ID_KEY = "NO";

	private final ActivityFacilities facilities;

	private final TripRouter router;

	private final ZoneCollection zones;

	private Map<String, Map<String, List<ActivityFacility>>> facilities2Zones;

	private Map<Plan, TIntArrayList> changeSet;

	private double volumeFactor = 100;

	private final NumericMatrix refMatrix;

	private final String outDir;

	public ODAdjustor(ActivityFacilities facilities, TripRouter router, ZoneCollection zones, NumericMatrix refMatrix, String outDir) {
		this.facilities = facilities;
		this.router = router;
		this.zones = zones;
		this.refMatrix = refMatrix;
		this.outDir = outDir;
		
		initFacilityData(zones);
	}

	private void initFacilityData(ZoneCollection zones) {
		logger.info("Initializing facility data...");
		facilities2Zones = new HashMap<>();

		ProgressLogger.init(facilities.getFacilities().values().size(), 2, 10);
		for (ActivityFacility facility : facilities.getFacilities().values()) {
			for (ActivityOption opt : facility.getActivityOptions().values()) {
				Zone zone = zones.get(new Coordinate(facility.getCoord().getX(), facility.getCoord().getY()));
				if (zone != null) {
					String zoneId = zone.getAttribute(ZONE_ID_KEY);

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

	public void run(Population pop) {
		HashMatrix<String, ODRelation> simMatrix = plans2Matrix(pop, zones, refMatrix);

		adjustRefMatrix(refMatrix, simMatrix);

		logger.info(String.format("Average error before: %s", calcAvrError(refMatrix, simMatrix)));

		changeSet = new HashMap<Plan, TIntArrayList>();

		DistanceCalculator distCalc = new CartesianDistanceCalculator();
		Discretizer disc = new LinearDiscretizer(100000);

		Random random = new XORShiftRandom();

		List<Zone> zoneSet = new ArrayList<>(zones.getZones());

//		Map<Zone, Set<Tuple<Integer, List<Zone>>>> zoneMap = new HashMap<>();
		List<Segment> segments = new ArrayList<>(10000);
		for (Zone origin : zones.getZones()) {
			Set<Segment> relations = getTargetZones(zoneSet, origin, simMatrix, distCalc, disc);
			for(Segment s : relations) {
				segments.add(s);
			}
		}
		 
		 
		for (long iter = 0; iter < zoneSet.size() * 100; iter++) {
			Segment s = segments.get(random.nextInt(segments.size()));
//			Zone origin = getZones.get(random.nextInt(getZones.size()));
//
//			Set<Tuple<Integer, List<Zone>>> relations = zoneMap.get(origin);
//			if (relations == null) {
//				relations = getTargetZones(getZones, origin, simMatrix, distCalc, disc);
//				zoneMap.put(origin, relations);
//			}
//
//			for (Tuple<Integer, List<Zone>> tuple : relations) {
				process(simMatrix, refMatrix, s.origin, s.targets, (long) (s.volume * volumeFactor), random);
//			}

			if (iter % 100 == 0) {
				logger.info(String.format("Average error after %s iterations: %s", iter, calcAvrError(refMatrix, simMatrix)));
			}
		}

		logger.info(String.format("Average error after: %s", calcAvrError(refMatrix, simMatrix)));

		logger.info("Rerouting populatiuon...");
		reroute(pop);
	}

	private Set<Segment> getTargetZones(Collection<Zone> zoneSet, Zone origin, HashMatrix<String, ODRelation> simMatrix,
			DistanceCalculator distCalc, Discretizer disc) {
		Map<Double, List<Zone>> relations = new HashMap<>();
		TDoubleIntHashMap volumes = new TDoubleIntHashMap();

		String originId = origin.getAttribute(ZONE_ID_KEY);
		for (Zone dest : zoneSet) {
			if (origin != dest) {
				double d = distCalc.distance(origin.getGeometry().getCentroid(), dest.getGeometry().getCentroid());
				d = disc.discretize(d);
				List<Zone> rSet = relations.get(d);
				if (rSet == null) {
					rSet = new ArrayList<>(1000);
					relations.put(d, rSet);
				}
				rSet.add(dest);

				ODRelation od = simMatrix.get(originId, dest.getAttribute(ZONE_ID_KEY));
				if (od != null) {
					int vol = od.getPlans().size();
					volumes.adjustOrPutValue(d, vol, vol);
				}
			}
		}

		Set<Segment> set = new HashSet<>();
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(String.format("%s/targetZones.txt", outDir), true));

			for (Entry<Double, List<Zone>> entry : relations.entrySet()) {
				Integer vol = volumes.get(entry.getKey());
				Segment s = new Segment();
				s.origin = origin;
				s.volume = vol;
				s.targets = entry.getValue();
				set.add(s);
			
				writer.write(s.origin.getAttribute("nuts3_code"));
				writer.write("\t");
				writer.write(String.valueOf(entry.getKey()));
				writer.write("\t");
				writer.write(String.valueOf(vol.intValue()));
				writer.write("\t");
				writer.write(String.valueOf(entry.getValue().size()));
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return set;
	}

	private void process(HashMatrix<String, ODRelation> simMatrix, NumericMatrix refMatrix, Zone origin, List<Zone> targets, long iterations, Random random) {
		String originId = origin.getAttribute(ZONE_ID_KEY);

		for (long i = 0; i < iterations; i++) {
			Zone z1 = targets.get(random.nextInt(targets.size()));
			Zone z2 = targets.get(random.nextInt(targets.size()));
			if (z1 != z2) {
				String id1 = z1.getAttribute(ZONE_ID_KEY);
				String id2 = z2.getAttribute(ZONE_ID_KEY);

				Double refVolOD1 = refMatrix.get(originId, id1);
				Double refVolOD2 = refMatrix.get(originId, id2);

				if (refVolOD1 != null && refVolOD2 != null) {

					ODRelation od1 = simMatrix.get(originId, id1);
					ODRelation od2 = simMatrix.get(originId, id2);

					if (od1 != null) {
						int simVolOD1 = od1.getPlans().size();
						if (simVolOD1 > 0) {
							int simVolOD2 = 0;

							if (od2 != null)
								simVolOD2 = od2.getPlans().size();

							double error1 = (simVolOD1 - refVolOD1) / refVolOD1;
							double error2 = (simVolOD2 - refVolOD2) / refVolOD2;

							double errorBefore = Math.abs(error1) + Math.abs(error2);

							simVolOD1--;
							simVolOD2++;

							error1 = (simVolOD1 - refVolOD1) / refVolOD1;
							error2 = (simVolOD2 - refVolOD2) / refVolOD2;

							double errorAfter = Math.abs(error1) + Math.abs(error2);

							if (errorAfter < errorBefore) {
								if (od2 == null)
									od2 = new ODRelation(originId, id2);

								shift(od1, od2, random);
							}
						}
					}
				}
			}
		}
	}

	private boolean shift(ODRelation fromOD, ODRelation toOD, Random random) {
		int planIdx = random.nextInt(fromOD.getPlans().size());
		Plan plan = fromOD.getPlans().get(planIdx);
		int legIdx = fromOD.getLegIndices().get(planIdx);
		int actIdx = legIdx + 1;
		ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(actIdx);

		if (!act.getType().equalsIgnoreCase(ActivityTypes.HOME)) {
			List<ActivityFacility> zoneFacilities = facilities2Zones.get(toOD.getToId()).get(act.getType());
			if (zoneFacilities != null) {
				ActivityFacility newFac = zoneFacilities.get(random.nextInt(zoneFacilities.size()));

				act.setFacilityId(newFac.getId());
				act.setLinkId(newFac.getLinkId());

				TIntArrayList changedIndices = changeSet.get(plan);
				if (changedIndices == null) {
					changedIndices = new TIntArrayList();
					changeSet.put(plan, changedIndices);
				}
				changedIndices.add(actIdx);

				fromOD.remove(planIdx);
				toOD.add(plan, legIdx);

				return true;
			} else {
				return false; // should not happen!
			}
		} else {
			return false;
		}
	}

	private HashMatrix<String, ODRelation> plans2Matrix(Population pop, ZoneCollection zones, NumericMatrix refMatrix) {
		HashMatrix<String, ODRelation> m = new HashMatrix<>();

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

						String id_i = origZone.getAttribute(ZONE_ID_KEY);
						String id_j = destZone.getAttribute(ZONE_ID_KEY);

						if (refMatrix.get(id_i, id_j) != null) {
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

	private double calcAvrError(NumericMatrix refMatrix, HashMatrix<String, ODRelation> simMatrix) {
		double errSum = 0;
		int cnt = 0;

		Set<String> keys = refMatrix.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double refVal = refMatrix.get(i, j);
				if (refVal != null) {
					double simVal = 0;
					ODRelation od = simMatrix.get(i, j);
					if (od != null)
						simVal = od.getPlans().size();

					double err = (simVal - refVal) / refVal;

					errSum += Math.abs(err);

					cnt++;
				}
			}
		}

		return errSum / (double) cnt;
	}

	private void reroute(Population pop) {
		ProgressLogger.init(changeSet.size(), 2, 10);
		for (Entry<Plan, TIntArrayList> entry : changeSet.entrySet()) {
			TIntArrayList indices = entry.getValue();
			Plan plan = entry.getKey();
			for (int i = 0; i < indices.size(); i++) {
				int actIdx = indices.get(i);
				Activity act = (Activity) plan.getPlanElements().get(actIdx);
				ActivityFacility newFac = facilities.getFacilities().get(act.getFacilityId());
				/*
				 * route outward trip
				 */
				if (actIdx > 1) {
					Activity prev = (Activity) plan.getPlanElements().get(actIdx - 2);
					ActivityFacility source = facilities.getFacilities().get(prev.getFacilityId());
					ActivityFacility target = newFac;

					Leg toLeg = (Leg) plan.getPlanElements().get(actIdx - 1);
					List<? extends PlanElement> stages = router.calcRoute(toLeg.getMode(), source, target, prev.getEndTime(), plan.getPerson());
					if (stages.size() > 1) {
						throw new UnsupportedOperationException();
					}
					plan.getPlanElements().set(actIdx - 1, stages.get(0));
				}
				/*
				 * route return trip
				 */
				if (actIdx < plan.getPlanElements().size() - 1) {
					Activity next = (Activity) plan.getPlanElements().get(actIdx + 2);
					ActivityFacility target = facilities.getFacilities().get(next.getFacilityId());
					ActivityFacility source = newFac;

					Leg fromLeg = (Leg) plan.getPlanElements().get(actIdx + 1);
					List<? extends PlanElement> stages = router.calcRoute(fromLeg.getMode(), source, target, act.getEndTime(), plan.getPerson());
					if (stages.size() > 1) {
						throw new UnsupportedOperationException();
					}
					plan.getPlanElements().set(actIdx + 1, stages.get(0));
				}
				// releaseTripRouter(router);
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
	}

	private static class ODRelation {

		private final String from;

		private final String to;

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

	private void adjustRefMatrix(NumericMatrix refMatrix, HashMatrix<String, ODRelation> simMatrix) {
		double c = ODUtils.calcNormalization(refMatrix, object2KeyMatrix(simMatrix));
		MatrixOperations.applyFactor(refMatrix, c);
		/*
		 * some statistics
		 */
		double min = Double.MAX_VALUE;
		double max = 0;
		Set<String> keys = refMatrix.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = refMatrix.get(i, j);
				if (val != null) {
					min = Math.min(min, val);
					max = Math.max(max, val);
				}
			}
		}
		logger.info(String.format("Matrix entry stats: min = %s, max = %s", min, max));
	}

	private static NumericMatrix object2KeyMatrix(HashMatrix<String, ODRelation> simMatrix) {
		NumericMatrix m = new NumericMatrix();
		Set<String> keys = simMatrix.keys();
		for (String i : keys) {
			for (String j : keys) {
				ODRelation od = simMatrix.get(i, j);
				if (od != null)
					m.set(i, j, (double) od.getPlans().size());
			}
		}

		return m;
	}
	
	private static class Segment {
		
		private int volume;
		
		private Zone origin;
		
		private List<Zone> targets;
	}
}
