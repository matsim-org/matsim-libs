/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.bvgAna.mrieser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonFilterSelectedPlan;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.bvgAna.level1.PersonEnterLeaveVehicle2ActivityHandler;
import playground.andreas.bvgAna.level1.StopId2PersonEnterLeaveVehicleHandler;
import playground.andreas.bvgAna.level2.StopId2RemainSeatedDataMap;
import playground.andreas.bvgAna.level2.StopId2RemainSeatedDataMapData;
import playground.andreas.bvgAna.level3.AgentId2StopDifferenceMap;

public class RunAnalyses {

	private final static Logger log = Logger.getLogger(RunAnalyses.class);

	private final static String networkFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/output_network.xml.gz";
	private final static String plansFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/output_plans.xml.gz";
	private final static String eventsFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/250.events.xml.gz";
	private final static String transitScheduleFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/transitSchedule.oevnet.xml.gz";
	private final static String transitVehiclesFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/transitVehicles_bvg_2005.xml.gz";

	private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

	public void readNetwork() {
		new MatsimNetworkReader(this.scenario).readFile(networkFilename);
		((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());
	}

	public void readPopulation() {
		new MatsimPopulationReader(this.scenario).readFile(plansFilename);
	}

	public void extractSelectedPlansOnly() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(s).readFile(networkFilename);
		PopulationImpl pop = (PopulationImpl) s.getPopulation();
		pop.setIsStreaming(true);

		PopulationWriter writer = new PopulationWriter(pop, s.getNetwork());
		writer.startStreaming("selectedPlansOnly.xml.gz");
		pop.addAlgorithm(new PersonFilterSelectedPlan());
		pop.addAlgorithm(writer);
		new MatsimPopulationReader(s).readFile(plansFilename);
		writer.closeStreaming();
	}

	public void readSelectedPlansOnly() {
		new MatsimPopulationReader(this.scenario).readFile("selectedPlansOnly.xml.gz");
	}

	public void createPersonAttributeTable(final String attributesFilename, final String idsFilename) {
		PopulationImpl pop = (PopulationImpl) scenario.getPopulation();
		pop.setIsStreaming(true);
		try {
			PersonAttributesWriter attributesWriter = new PersonAttributesWriter(attributesFilename);
			pop.addAlgorithm(attributesWriter);
			PersonIdsWriter idsWriter = new PersonIdsWriter(idsFilename);
			pop.addAlgorithm(idsWriter);
			new MatsimPopulationReader(scenario).readFile(plansFilename);
			attributesWriter.close();
			idsWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static class PersonAttributesWriter implements PersonAlgorithm {

		private final BufferedWriter writer;

		public PersonAttributesWriter(final String filename) throws FileNotFoundException, IOException {
			this.writer = IOUtils.getBufferedWriter(filename);
			this.writer.write("Id\tage\tgender\tlicense\tcarAvail\temployed\n");
		}

		@Override
		public void run(Person person) {
			try {
				PersonImpl p = (PersonImpl) person;
				this.writer.write(person.getId() + "\t" + p.getAge() + "\t");
				this.writer.write(p.getSex() + "\t" + p.getLicense() + "\t");
				this.writer.write(p.getCarAvail() + "\t" + p.isEmployed() + "\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void close() throws IOException {
			this.writer.close();
		}
	}

	private static class PersonIdsWriter implements PersonAlgorithm {

		private final BufferedWriter writer;

		public PersonIdsWriter(final String filename) throws FileNotFoundException, IOException {
			this.writer = IOUtils.getBufferedWriter(filename);
		}

		@Override
		public void run(Person person) {
			try {
				this.writer.write(person.getId() + "\n");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void close() throws IOException {
			this.writer.close();
		}
	}

	public Set<Id> readIdSet(final String filename) {
		Set<Id> set = new HashSet<Id>(500);
		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			String line = null;
			while ((line = reader.readLine()) != null) {
				set.add(new IdImpl(line));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return set;
	}

	public void readTransitSchedule() {
		this.scenario.getConfig().scenario().setUseTransit(true);
		new TransitScheduleReader(this.scenario).readFile(transitScheduleFilename);
	}

	public void createRemainSeatedStats() {
		StopId2RemainSeatedDataMap remainSeated = new StopId2RemainSeatedDataMap();
		TransitSchedule ts = ((ScenarioImpl) this.scenario).getTransitSchedule();

		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(remainSeated);
		new MatsimEventsReader(em).readFile(eventsFilename);
		Map<Id, List<StopId2RemainSeatedDataMapData>> remainSeatedMap = remainSeated.getStopId2RemainSeatedDataMap();
		System.out.println("# stops with data: " + remainSeatedMap.size());
		System.out.println("# stops in schedule: " + ts.getFacilities().size());
		// aggregate similar stops
		Map<Id, List<StopId2RemainSeatedDataMapData>> aggregatedMap = new TreeMap<Id, List<StopId2RemainSeatedDataMapData>>();
		for (Map.Entry<Id, List<StopId2RemainSeatedDataMapData>> e : remainSeatedMap.entrySet()) {
			Id baseId = getBaseId(e.getKey());
			List<StopId2RemainSeatedDataMapData> list = aggregatedMap.get(baseId);
			if (list == null) {
				list = new ArrayList<StopId2RemainSeatedDataMapData>();
				aggregatedMap.put(baseId, list);
			}
			list.addAll(e.getValue());
		}
		// sort by time, likely not needed
		for (Map.Entry<Id, List<StopId2RemainSeatedDataMapData>> e : aggregatedMap.entrySet()) {
			Collections.sort(e.getValue(), new Comparator<StopId2RemainSeatedDataMapData>() {
				@Override
				public int compare(StopId2RemainSeatedDataMapData o1, StopId2RemainSeatedDataMapData o2) {
					return Double.compare(o1.getVehicleArrivesAtFacilityEvent().getTime(), o2.getVehicleArrivesAtFacilityEvent().getTime());
				}
			});
		}
		// print absolute values
		System.out.print("stopId\tstopX\tstopY\tstopName");
		for (int slot = 0; slot <= 24*4; slot++) {
			String time = Time.writeTime(slot*15*60, Time.TIMEFORMAT_HHMM, '_');
			time = time.replaceAll("_", "");
			System.out.print("\tenter_" + time);
			System.out.print("\tleave_" + time);
			System.out.print("\tstay_" + time);
		}
		System.out.println();
		for (Map.Entry<Id, List<StopId2RemainSeatedDataMapData>> e : aggregatedMap.entrySet()) {
			System.out.print(e.getKey().toString() + "\t");
			TransitStopFacility stop = ts.getFacilities().get(e.getKey());
			System.out.print(stop.getCoord().getX() + "\t" + stop.getCoord().getY() + "\t" + stop.getName());
			int[] entering = new int[24*4 + 1];
			int[] leaving = new int[24*4 + 1];
			int[] staying = new int[24*4 + 1];
			for (StopId2RemainSeatedDataMapData d : e.getValue()) {
				int slot = getTimeSlot(d.getVehicleArrivesAtFacilityEvent().getTime());
				entering[slot] += d.getNumberOfAgentsEntering();
				leaving[slot] += d.getNumberOfAgentsLeaving();
				staying[slot] += d.getNumberOfAgentsRemainedSeated();
			}
			for (int slot = 0; slot < entering.length; slot++) {
				System.out.print("\t" + entering[slot]);
				System.out.print("\t" + leaving[slot]);
				System.out.print("\t" + staying[slot]);
			}
			System.out.println();
		}
		// print relative values
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.print("stopId\tstopX\tstopY\tstopName");
		for (int slot = 0; slot <= 24*4; slot++) {
			String time = Time.writeTime(slot*15*60, Time.TIMEFORMAT_HHMM, '_');
			time = time.replaceAll("_", "");
			System.out.print("\tenter%" + time);
			System.out.print("\tleave%" + time);
			System.out.print("\tstay%" + time);
		}
		System.out.println();
		for (Map.Entry<Id, List<StopId2RemainSeatedDataMapData>> e : aggregatedMap.entrySet()) {
			System.out.print(e.getKey().toString() + "\t");
			TransitStopFacility stop = ts.getFacilities().get(e.getKey());
			System.out.print(stop.getCoord().getX() + "\t" + stop.getCoord().getY() + "\t" + stop.getName());
			int[] arriving = new int[24*4 + 1];
			int[] entering = new int[24*4 + 1];
			int[] leaving = new int[24*4 + 1];
			for (StopId2RemainSeatedDataMapData d : e.getValue()) {
				int slot = getTimeSlot(d.getVehicleArrivesAtFacilityEvent().getTime());
				arriving[slot] += d.getNumberOfAgentsArriving();
				entering[slot] += d.getNumberOfAgentsEntering();
				leaving[slot] += d.getNumberOfAgentsLeaving();
			}
			for (int slot = 0; slot < entering.length; slot++) {
				int departing = entering[slot] + arriving[slot];
				if (departing == 0) {
					System.out.print("\t0");
				} else {
					System.out.print("\t" + ((double) entering[slot]) / ((double) departing));
				}
				if (arriving[slot] == 0) {
					System.out.print("\t0");
					System.out.print("\t0");
				} else {
					System.out.print("\t" + ((double) leaving[slot]) / ((double) arriving[slot]));
					System.out.print("\t" + ((double) (arriving[slot] - leaving[slot])) / ((double) arriving[slot]));
				}
			}
			System.out.println();
		}

	}

	private Id getBaseId(final Id stopId) {
		String s = stopId.toString();
		int pos = s.indexOf('.');
		if (pos < 0) {
			return stopId;
		}
		return new IdImpl(s.substring(0, pos));
	}

	private int getTimeSlot(final double time) {
		int slot = (int) (time / (15*60));
		if (slot > 24*4) {
			slot = 24*4;
		}
		return slot;
	}

	public void createMissedConnectionStats(Set<Id> agentIds) {
		AgentId2StopDifferenceMap missedConnections = new AgentId2StopDifferenceMap(this.scenario.getPopulation(), agentIds);

		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(missedConnections);
		new MatsimEventsReader(em).readFile(eventsFilename);

		Map<Id, List<Tuple<Id, Integer>>> missedVehiclesMap = missedConnections.getNumberOfMissedVehiclesMap();

		// Histogram: Distribution of missed connections per user
		System.out.println("# persons with data: " + missedVehiclesMap.size());

		int[] nOfMissedConnectionsHistogram = new int[11];
		for (Id personId : scenario.getPopulation().getPersons().keySet()) {
			List<Tuple<Id, Integer>> missed = missedVehiclesMap.get(personId);
			int slot = 0;
			if (missed != null) {
				for (Tuple<Id, Integer> t : missed) {
					if (t.getSecond().intValue() > 0) {
						slot++;
					}
				}
				if (slot >= nOfMissedConnectionsHistogram.length) {
					slot = nOfMissedConnectionsHistogram.length - 1;
				}
			}
			nOfMissedConnectionsHistogram[slot]++;
		}

		System.out.println("Distribution of missed connections:");
		for (int i = 0; i < nOfMissedConnectionsHistogram.length; i++) {
			System.out.println("with " + i + " missed connections: " + nOfMissedConnectionsHistogram[i]);
		}

		// List of #agents with missed connections per stop
		System.out.println();
		System.out.println();
		System.out.println();
		Map<Id, Integer> missedConnectionsPerStop = new HashMap<Id, Integer>();
		for (List<Tuple<Id, Integer>> entries : missedVehiclesMap.values()) {
			for (Tuple<Id, Integer> t : entries) {
				Id stopId = getBaseId(t.getFirst());
				int missedVehicles = t.getSecond().intValue();
				if (missedVehicles > 0) {
					Integer cnt = missedConnectionsPerStop.get(stopId);
					if (cnt == null) {
						missedConnectionsPerStop.put(stopId, 1);
					} else {
						missedConnectionsPerStop.put(stopId, cnt.intValue() + 1);
					}
				}
			}
		}
		TransitSchedule ts = ((ScenarioImpl) this.scenario).getTransitSchedule();
		System.out.println("stopId\tstopX\tstopY\tstopName\t#affectPrs");
		Set<Id> handledStops = new HashSet<Id>();
		for (TransitStopFacility stop : ts.getFacilities().values()) {
			Id stopId = getBaseId(stop.getId());
			if (!handledStops.contains(stopId)) {
				handledStops.add(stopId);
				System.out.print(stopId + "\t" + stop.getCoord().getX() + "\t" + stop.getCoord().getY() + "\t" + stop.getName());
				Integer affectPrs = missedConnectionsPerStop.get(stop.getId());
				if (affectPrs == null) {
					System.out.println("\t0");
				} else {
					System.out.println("\t" + affectPrs.intValue());
				}
			}
		}
	}

	public void createCatchmentAreaStats(Set<Id> agentIds, Id lineId, Id[] routeIds) {
		TransitSchedule ts = ((ScenarioImpl) this.scenario).getTransitSchedule();
		Set<Id> stopIds = new HashSet<Id>();
		Set<Id> vehicleIds = new HashSet<Id>();
		TransitLine line = ts.getTransitLines().get(lineId);
		for (Id routeId : routeIds) {
			TransitRoute route = line.getRoutes().get(routeId);
			for (TransitRouteStop stop : route.getStops()) {
				stopIds.add(stop.getStopFacility().getId());
			}
			for (Departure dep : route.getDepartures().values()) {
				vehicleIds.add(dep.getVehicleId());
			}
		}

		StopId2PersonEnterLeaveVehicleHandler enterLeave = new StopId2PersonEnterLeaveVehicleHandler(stopIds);
		PersonEnterLeaveVehicle2ActivityHandler enterLeave2Act = new PersonEnterLeaveVehicle2ActivityHandler(agentIds);
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(enterLeave);
		em.addHandler(enterLeave2Act);
		new MatsimEventsReader(em).readFile(eventsFilename);

//		Set<Id> agentIds = new HashSet<Id>(1000);
//		for (List<PersonEntersVehicleEvent> events : enterLeave.getStopId2PersonEnterEventMap().values()) {
//			for (PersonEntersVehicleEvent event : events) {
//				agentIds.add(event.getPersonId());
//			}
//		}

		Map<Id, List<PersonEntersVehicleEvent>> enterEventsPerPerson = new HashMap<Id, List<PersonEntersVehicleEvent>>();
		Map<Id, List<PersonLeavesVehicleEvent>> leaveEventsPerPerson = new HashMap<Id, List<PersonLeavesVehicleEvent>>();

		for (List<PersonEntersVehicleEvent> events : enterLeave.getStopId2PersonEnterEventMap().values()) {
			for (PersonEntersVehicleEvent event : events) {
				if (vehicleIds.contains(event.getVehicleId())) {
					List<PersonEntersVehicleEvent> list = enterEventsPerPerson.get(event.getPersonId());
					if (list == null) {
						list = new ArrayList<PersonEntersVehicleEvent>(3);
						enterEventsPerPerson.put(event.getPersonId(), list);
					}
					list.add(event);
				}
			}
		}
		for (List<PersonLeavesVehicleEvent> events : enterLeave.getStopId2PersonLeaveEventMap().values()) {
			for (PersonLeavesVehicleEvent event : events) {
				if (vehicleIds.contains(event.getVehicleId())) {
					List<PersonLeavesVehicleEvent> list = leaveEventsPerPerson.get(event.getPersonId());
					if (list == null) {
						list = new ArrayList<PersonLeavesVehicleEvent>(3);
						leaveEventsPerPerson.put(event.getPersonId(), list);
					}
					list.add(event);
				}
			}
		}

		// step in parallel through both lists, match the data
		Map<PersonEntersVehicleEvent, ActivityEndEvent> activityEnds = enterLeave2Act.getPersonEntersVehicleEvent2ActivityEndEvent();
		Map<PersonLeavesVehicleEvent, ActivityStartEvent> activityStarts = enterLeave2Act.getPersonLeavesVehicleEvent2ActivityStartEvent();
		Map<Id, List<AgentTripData>> agentData = new HashMap<Id, List<AgentTripData>>();
		for (Id personId : enterEventsPerPerson.keySet()) {
			List<PersonEntersVehicleEvent> enterList = enterEventsPerPerson.get(personId);
			List<PersonLeavesVehicleEvent> leaveList = leaveEventsPerPerson.get(personId);

			Comparator<Event> eventComparator = new Comparator<Event>() {
				@Override
				public int compare(Event o1, Event o2) {
					return Double.compare(o1.getTime(), o2.getTime());
				}
			};
			Collections.sort(enterList, eventComparator);
			Collections.sort(leaveList, eventComparator);

			int n = Math.min(enterList.size(), leaveList.size());
			List<AgentTripData> datalist = new LinkedList<AgentTripData>();
			agentData.put(personId, datalist);
			for (int i = 0; i < n; i++) {
				AgentTripData data = new AgentTripData(personId);
				PersonEntersVehicleEvent enterEvent = enterList.get(i);
				ActivityEndEvent endEvent = activityEnds.get(enterEvent);
				data.fromActType = endEvent.getActType();
				data.fromLinkId = endEvent.getLinkId();

				PersonLeavesVehicleEvent leaveEvent = leaveList.get(i);
				ActivityStartEvent startEvent = activityStarts.get(leaveEvent);
				if (startEvent == null) {
					log.warn("No ActivityStartEvent found for PersonLeavesVehicleEvent: time=" + leaveEvent.getTime() + " person=" + leaveEvent.getPersonId() + " vehicle=" + leaveEvent.getVehicleId());
				} else {
					data.toActType = startEvent.getActType();
					data.toLinkId = startEvent.getLinkId();
				}
				datalist.add(data);
			}
		}

		// now go through population and figure out exact coordinates if available

		Population pop = this.scenario.getPopulation();
		for (List<AgentTripData> list : agentData.values()) {
			for (AgentTripData data : list) {
				Person p = pop.getPersons().get(data.agentId);
				Activity prevActivity = null;
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof Activity) {
						Activity a = (Activity) pe;

						if (prevActivity != null) {
							if (data.toActType == null) {
								// we can only compare from activity, use first match
								if (prevActivity.getType().equals(data.fromActType) && prevActivity.getLinkId().equals(data.fromLinkId)) {
									data.fromActCoord = prevActivity.getCoord();
									data.toActCoord = a.getCoord();
									break;
								}
							} else {
								// compare from and to activity
								if (prevActivity.getType().equals(data.fromActType) && a.getType().equals(data.toActType)
										&& prevActivity.getLinkId().equals(data.fromLinkId) && a.getLinkId().equals(data.toLinkId)) {
									data.fromActCoord = prevActivity.getCoord();
									data.toActCoord = a.getCoord();
									break;
								}
							}
						}
						if (!a.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
							prevActivity = a;
						}
					}
				}
				if (data.fromActCoord == null) {
					log.error("No from.coord found for person=" + data.agentId + " from.act=" + data.fromActType + " from.link=" + data.fromLinkId + " to.act=" + data.toActType + " to.link=" + data.toLinkId);
				}
			}
		}

		// now write everything out
		System.out.println();
		System.out.println();
		System.out.println("Passenger-Analysis for line " + lineId + " on the following routes:");
		for (Id routeId : routeIds) {
			System.out.println("  " + routeId.toString());
		}
		System.out.println("AgentId\tfromType\tfromX\tfromY\ttoType\ttoX\ttoY");
		for (List<AgentTripData> list : agentData.values()) {
			for (AgentTripData data : list) {
				if (data.fromActCoord == null) {
					// write nothing
				} else if (data.toActCoord == null) {
					System.out.println(data.agentId + "\t" + data.fromActType + "\t" + data.fromActCoord.getX() +
							"\t" + data.fromActCoord.getY() + "\t" + data.toActType + "\t\t");
				} else {
					System.out.println(data.agentId + "\t" + data.fromActType + "\t" + data.fromActCoord.getX() +
							"\t" + data.fromActCoord.getY() + "\t" + data.toActType + "\t" + data.toActCoord.getX() +
							"\t" + data.toActCoord.getY());
				}
			}
		}

		// aggregation
		int aggregateSize = 100; // 100m x 100m blocks
		Map<Tuple<Integer, Integer>, Integer> fromBlockCnt = new HashMap<Tuple<Integer, Integer>, Integer>();
		Map<Tuple<Integer, Integer>, Integer> toBlockCnt = new HashMap<Tuple<Integer, Integer>, Integer>();
		for (List<AgentTripData> list : agentData.values()) {
			for (AgentTripData data : list) {
				if (data.fromActCoord != null) {
					Tuple<Integer, Integer> blockId = new Tuple<Integer, Integer>((int) data.fromActCoord.getX() / aggregateSize, (int) data.fromActCoord.getY() / aggregateSize);
					Integer blockCnt = fromBlockCnt.get(blockId);
					if (blockCnt == null) {
						fromBlockCnt.put(blockId, 1);
					} else {
						fromBlockCnt.put(blockId, blockCnt.intValue() + 1);
					}
				}
				if (data.toActCoord != null) {
					Tuple<Integer, Integer> blockId = new Tuple<Integer, Integer>((int) data.toActCoord.getX() / aggregateSize, (int) data.toActCoord.getY() / aggregateSize);
					Integer blockCnt = toBlockCnt.get(blockId);
					if (blockCnt == null) {
						toBlockCnt.put(blockId, 1);
					} else {
						toBlockCnt.put(blockId, blockCnt.intValue() + 1);
					}
				}
			}
		}
		Set<Tuple<Integer, Integer>> keySet = new HashSet<Tuple<Integer, Integer>>(fromBlockCnt.keySet());
		keySet.addAll(toBlockCnt.keySet());
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("BlockX\tBlockY\tfromCount\ttoCount");
		for (Tuple<Integer, Integer> t : keySet) {
			System.out.print(t.getFirst() * aggregateSize + aggregateSize * 0.5 + "\t" + (t.getSecond() * aggregateSize + aggregateSize * 0.5) + "\t");
			Integer cnt = fromBlockCnt.get(t);
			if (cnt == null) {
				System.out.print("0");
			} else {
				System.out.print(cnt.intValue());
			}
			System.out.print("\t");
			cnt = toBlockCnt.get(t);
			if (cnt == null) {
				System.out.print("0");
			} else {
				System.out.print(cnt.intValue());
			}
			System.out.println();
		}

	}

	private static class AgentTripData {
		public final Id agentId;
		public String fromActType = null;
		public Id fromLinkId = null;
		public Coord fromActCoord = null;
		public String toActType = null;
		public Id toLinkId = null;
		public Coord toActCoord = null;

		public AgentTripData(final Id agentId) {
			this.agentId = agentId;
		}
	}

	public static void main(String[] args) {
		RunAnalyses app = new RunAnalyses();
		app.readNetwork();
//		app.readPopulation();
//		app.extractSelectedPlansOnly();
		app.readSelectedPlansOnly();
//		app.createPersonAttributeTable("personAttributes.txt", "allPersonIds.txt");
		Set<Id> allPersonIds = app.readIdSet("allPersonIds.txt");
//		System.out.println(allPersonIds.size());
		app.readTransitSchedule();
//		app.createRemainSeatedStats();
//		app.createMissedConnectionStats(allPersonIds);
		app.createCatchmentAreaStats(allPersonIds, new IdImpl("BVB----145"),
				new Id[] {new IdImpl("BVB----145.10.GEN12.R"), new IdImpl("BVB----145.10.GEN5.R"), new IdImpl("BVB----145.10.GEN8.R")});
	}
}
