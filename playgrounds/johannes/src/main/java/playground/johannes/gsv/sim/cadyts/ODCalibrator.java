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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.collections.CollectionUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.core.network.NetworkUtils;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import playground.johannes.coopsim.utils.MatsimCoordUtils;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.gis.Zone;
import playground.johannes.synpop.gis.ZoneCollection;
import playground.johannes.synpop.matrix.MatrixOperations;
import playground.johannes.synpop.matrix.NumericMatrix;
import playground.johannes.synpop.matrix.ODMatrixOperations;
import playground.johannes.synpop.util.Executor;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author johannes
 * 
 */
public class ODCalibrator implements PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	public static final String VIRTUAL_ID_PREFIX = "virtual";

	public static final String ZONE_ID_KEY = "NO";
	
	private static final Logger logger = Logger.getLogger(ODCalibrator.class);

	private final Network network;

//	private final Map<Node, Node> real2virtual;

	private final PlanToPlanStepBasedOnEvents p2p;

	private final SimResultsAdaptor adaptor;

//	private final Map<Id<Person>, Node> person2Node;

	private final double scaleFactor;
	
	private ZoneCollection zones;
	
	private Set<Person> candidates;
	
	private final Map<Id<Person>, Integer> person2LegIndex = new ConcurrentHashMap<>();
	
	private Map<Id<ActivityFacility>, Node> facility2Node = new IdentityHashMap<>();
	
	private final Population population;

	private final Map<Id<Person>, Id<Vehicle>> person2VehicleIds = new HashMap<>();

	public ODCalibrator(Scenario scenario, CadytsContext cadytsContext, NumericMatrix odMatrix, ZoneCollection zones, double distThreshold, double countThreshold, String aggKey) {
		this.network = scenario.getNetwork();
		this.p2p = (PlanToPlanStepBasedOnEvents) cadytsContext.getPlansTranslator();
		this.adaptor = cadytsContext.getSimResultsAdaptor();
		this.scaleFactor = cadytsContext.getScalingFactor();

//		this.person2Node = new IdentityHashMap<>();
//		this.real2virtual = new IdentityHashMap<>();
		this.zones = zones;
		this.population = scenario.getPopulation();
		
		this.zones.setPrimaryKey(ZONE_ID_KEY);
		odMatrix = buildCalibrationMatrix(odMatrix, scenario, distThreshold, countThreshold, aggKey);
		buildVirtualNetwork(odMatrix, cadytsContext.getCounts(), scenario.getActivityFacilities());
		determineCandidates(zones, scenario.getPopulation(), distThreshold);
	}

	private void buildVirtualNetwork(NumericMatrix odMartix, Counts counts, ActivityFacilities facilities) {
		logger.info("Building virutal network...");
		
		Set<String> keys = odMartix.keys();
		Map<String, Node> key2Node = new HashMap<String, Node>();

		for (String key : keys) {
			Zone zone = zones.get(key);
			Id<Node> nodeId = Id.createNodeId(String.format("%s.%s", VIRTUAL_ID_PREFIX, zone.getAttribute(ZONE_ID_KEY)));
			Node node = network.getFactory().createNode(nodeId, MatsimCoordUtils.pointToCoord(zone.getGeometry().getCentroid()));
			network.addNode(node);
			key2Node.put(key, node);

		}
		logger.info(String.format("Created %s virtual nodes.", keys.size()));

		int cnt = 0;
		for (String i : keys) {
			for (String j : keys) {
				if (i != j) {
					Double volume = odMartix.get(i, j);
					if (volume != null) {
						Node ni = key2Node.get(i);
						Node nj = key2Node.get(j);
						Id<Link> linkId = Id.createLinkId(String.format("%s.%s.%s", VIRTUAL_ID_PREFIX, i, j));
						Link link = network.getFactory().createLink(linkId, ni, nj);
						network.addLink(link);
						cnt++;
						
						p2p.addCalibratedItem(link.getId());

						Count count = counts.createAndAddCount(link.getId(), link.getId().toString());

						volume = volume / 24.0;
						for (int h = 1; h < 25; h++) {
							count.createVolume(h, volume);
						}
					}
				}
			}
		}
		logger.info(String.format("Created %s virtual links.", cnt));
		
		logger.info("Assigning facilities to virtual node.");
		ProgressLogger.init(facilities.getFacilities().size(), 2, 10);
		cnt = 0;
		for (ActivityFacility fac : facilities.getFacilities().values()) {
			Coordinate c = new Coordinate(fac.getCoord().getX(), fac.getCoord().getY());
			Zone zone = zones.get(c);
			if (zone != null) {
				Node vNode = key2Node.get(zone.getAttribute(ZONE_ID_KEY));
				facility2Node.put(fac.getId(), vNode);
			} else {
				cnt++;
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
		
		if(cnt > 0) {
			logger.warn(String.format("%s facilities cannot be assigned to a virtual node.", cnt));
		}
//		
//		for (Node node : network.getNodes().values()) {
//			Zone zone = zones.get(new Coordinate(node.getCoord().getX(), node.getCoord().getY()));
//			if (zone != null) {
//				Node vNode = key2Node.get(zone.getAttribute(ZONE_ID_KEY));
//				real2virtual.put(node, vNode);
//			} else {
//				cnt++;
//			}
//		}
//
//		if (cnt > 0) {
//			logger.warn(String.format("%s nodes cannot be assigned to a virtual node.", cnt));
//		}
	}

	@Override
	public void reset(int iteration) {
		adaptor.resetVirtualCounts();
//		person2Node.clear();
		person2LegIndex.clear();

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Integer idx = person2LegIndex.get(event.getPersonId());
		if(idx != null) {
			if(event.getLegMode().equalsIgnoreCase("car")) {
				Person person = population.getPersons().get(event.getPersonId());
				Plan plan = person.getSelectedPlan();
				Activity from = (Activity) plan.getPlanElements().get(idx - 1);
				Activity to = (Activity) plan.getPlanElements().get(idx + 1);

				Node vStart = facility2Node.get(from.getFacilityId());
				Node vEnd = facility2Node.get(to.getFacilityId());
				
				if (vStart != null && vEnd != null) {
					Link vLink = NetworkUtils.getConnectingLink(vStart, vEnd);
					if (vLink != null) {

						Id<Vehicle> vehicleId = person2VehicleIds.get(event.getPersonId());
						if(vehicleId == null) {
							vehicleId = Id.createVehicleId(event.getPersonId());
							person2VehicleIds.put(event.getPersonId(), vehicleId);
						}

						p2p.handleEvent(new PersonDepartureEvent(event.getTime(), event.getPersonId(), vLink.getId(), event.getLegMode()));
						p2p.handleEvent(new LinkLeaveEvent(event.getTime(), vehicleId, vLink.getId()));
						p2p.handleEvent(new PersonArrivalEvent(event.getTime(), event.getPersonId(), vLink.getId(), event.getLegMode()));

						adaptor.addVirtualCount(vLink);
					}
				}
				
			}
		} else {
			logger.error(String.format("No departure event for person %s found.", event.getPersonId()));
		}
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Integer idx = person2LegIndex.get(event.getPersonId());
		if(idx == null) {
			idx = 1;
		} else {
			idx += 2;
		}
		person2LegIndex.put(event.getPersonId(), idx);
		
//		if (event.getLegMode().equalsIgnoreCase("car")) {
//			Node startNode = network.getLinks().get(event.getLinkId()).getFromNode();
//			person2Node.put(event.getDriverId(), startNode);
//		}
	}
	
	private NumericMatrix buildCalibrationMatrix(NumericMatrix refMatrix, Scenario scenario, double distThreshold, double countThreshold, String aggKey) {
		/*
		 * remove reference relations below distance threshold
		 */
		logger.info(String.format("Removing entries below %.2f KM in reference matrix.", distThreshold/1000.0));
		removeEntries(refMatrix, zones, distThreshold);
		/*
		 * aggregated if specified
		 */
		if(aggKey != null) {
			refMatrix = ODMatrixOperations.aggregate(refMatrix, zones, aggKey);
			zones = aggregateZones(zones, aggKey);
		}
		/*
		 * build matrix from simulated plans
		 */
		logger.info("Building matrix from simulated plans...");
		NumericMatrix simMatrix = plans2Matrix(scenario.getPopulation(), zones, scenario.getActivityFacilities(), distThreshold);
		/*
		 * calculate normalization
		 */
		double c_sim = MatrixOperations.sum(simMatrix);
		double c_ref = MatrixOperations.sum(refMatrix);
		double f = c_sim/c_ref;
		logger.info(String.format("Trip sum simulated matrix = %s, trip sum reference matrix = %s, factor = %s", c_sim, c_ref, f));
		/*
		 * remove reference relations below count threshold
		 */
		int cnt = 0;
		Set<String> keys = refMatrix.keys();
		for (String i : keys) {
			for (String j : keys) {
				Double val = refMatrix.get(i, j);
				if(val != null && val < countThreshold) {
					refMatrix.set(i, j, null);
					cnt++;
				}
			}
		}
		logger.info(String.format("Removed %s relations with less than %s trips.", cnt, countThreshold));
		/*
		 * normalize
		 */
		MatrixOperations.applyFactor(refMatrix, f);
		MatrixOperations.applyFactor(refMatrix, scaleFactor);
		/*
		 * some statistics
		 */
		double min = Double.MAX_VALUE;
		double max = 0;
		for (String i : keys) {
			for (String j : keys) {
				Double val = refMatrix.get(i, j);
				if(val != null) {
					min = Math.min(min, val);
					max = Math.max(max, val);
				}
			}
		}
		logger.info(String.format("Matrix entry stats: min = %s, max = %s", min, max));
		
		return refMatrix;
	}
	
	private void removeEntries(NumericMatrix m, ZoneCollection zones, double distThreshold) {
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
		
		logger.info(String.format("Removed %s trips with less than %s KM.", cnt, distThreshold/1000.0));
	}
	
	private NumericMatrix plans2Matrix(Population pop, ZoneCollection zones, ActivityFacilities facilities, double distThreshold) {
		NumericMatrix m = new NumericMatrix();
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
//		candidates = new LinkedHashSet<>();
		
//		double minDist = Double.MAX_VALUE;
		
		ProgressLogger.init(pop.getPersons().size(), 2, 10);
		
		for(Person person : pop.getPersons().values()) {
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
							String id_i = origZone.getAttribute(ZONE_ID_KEY);
							String id_j = destZone.getAttribute(ZONE_ID_KEY);
							Double val = m.get(id_i, id_j);
							if (val == null)
								val = 0.0;
							val++;
							m.set(id_i, id_j, val);
							
//							candidates.add(person);
							
						}
					}
				}
			}
			ProgressLogger.step();
		}
		
		ProgressLogger.terminate();
		
		return m;
	}
	
	private ZoneCollection aggregateZones(ZoneCollection zones, String key) {
		/*
		 * collect zones according key
		 */
		Map<String, Set<Zone>> zonesMap = new HashMap<>();
		for(Zone zone : zones.getZones()) {
			String code = zone.getAttribute(key);
			Set<Zone> set = zonesMap.get(code);
			if(set == null) {
				set = new HashSet<>();
				zonesMap.put(code, set);
			}
			set.add(zone);
		}
		
		ZoneCollection mergedZones = new ZoneCollection();
		
		for(Entry<String, Set<Zone>> entry : zonesMap.entrySet()) {
			Set<Zone> set = entry.getValue();
			Geometry refGeo = null;
			for(Zone zone : set) {
				if(refGeo != null) {
					Geometry geo = zone.getGeometry();
					refGeo  = refGeo.union(geo);
				} else {
					refGeo = zone.getGeometry();
				}
			}
			
			Zone zone = new Zone(refGeo);
			zone.setAttribute(key, entry.getKey());
			zone.setAttribute(ZONE_ID_KEY, entry.getKey());
			mergedZones.add(zone);
		}
		mergedZones.setPrimaryKey(key);
		
		return mergedZones;
	}
	
	public Set<Person> getCandidates() {
		return candidates;
	}
	
	private void determineCandidates(ZoneCollection zones, Population pop, double distThreshold) {
		final List<Zone> zoneList = new ArrayList<>(zones.getZones());
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		double minZoneDist = Double.MAX_VALUE;

		logger.info("Calculating minimal zone distance...");

		List<Zone>[] segments = CollectionUtils.split(zoneList, Executor.getFreePoolSize());
		List<MinDistThread> threads = new ArrayList<>();

		ProgressLogger.init(zoneList.size() * segments.length, 2, 10);
		for(List<Zone> segment : segments) {
			threads.add(new MinDistThread(zoneList, segment, dCalc, distThreshold));
		}
		Executor.submitAndWait(threads);

		for(MinDistThread thread : threads) {
			minZoneDist = Math.min(minZoneDist, thread.getMinZoneDist());
		}
		Executor.shutdown();
		ProgressLogger.terminate();

		logger.info(String.format("Minimum zone distance: %s", minZoneDist));
		
		logger.info("Determining simulation candidates...");
		candidates = new LinkedHashSet<>();
		
		ProgressLogger.init(pop.getPersons().size(), 2, 10);
		for (Person person : pop.getPersons().values()) {
			ObjectAttributes oatts = pop.getPersonAttributes();

			List<Double> atts = (List<Double>) oatts.getAttribute(person.getId().toString(), CommonKeys.LEG_GEO_DISTANCE);
			if (atts != null) {
				for (Double d : atts) {
					if (d != null && d >= minZoneDist) {
						/*
						 * do not add the foreign dummy persons
						 */
						if (!person.getId().toString().startsWith("foreign")) {
							candidates.add(person);
							break;
						}
					}
				}
			}
			ProgressLogger.step();
		}
		ProgressLogger.terminate();
		
		logger.info(String.format("Determined %s simulation candidates.", candidates.size()));
	}

	private static class MinDistThread implements Runnable {

		private final List<Zone> startZones;

		private final List<Zone> targetZones;

		private final DistanceCalculator dCalc;

		private final double distThreshold;

		private double minZoneDist = Double.MAX_VALUE;

		public MinDistThread(List<Zone> startZones, List<Zone> targetZones, DistanceCalculator dCalc, double
				distThreshold) {
			this.startZones = startZones;
			this.targetZones = targetZones;
			this.dCalc = dCalc;
			this.distThreshold = distThreshold;
		}

		public double getMinZoneDist() {
			return minZoneDist;
		}

		@Override
		public void run() {
			for(int i = 0; i < startZones.size(); i++) {
				for (int j = 0; j < targetZones.size(); j++) {
					Zone zi = startZones.get(i);
					Zone zj = targetZones.get(j);

					double controidDist = dCalc.distance(zi.getGeometry().getCentroid(), zj.getGeometry().getCentroid());
					if (controidDist >= distThreshold) {
						double d = DistanceOp.distance(zi.getGeometry(), zj.getGeometry());
						minZoneDist = Math.min(minZoneDist, d);
					}
				}
				ProgressLogger.step();
			}
		}
	}
}
