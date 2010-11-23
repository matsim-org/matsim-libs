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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.andreas.bvgAna.level2.StopId2RemainSeatedDataMap;
import playground.andreas.bvgAna.level2.StopId2RemainSeatedDataMapData;

public class RunAnalyses {

	private final static String networkFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/output_network.xml.gz";
	private final static String plansFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/output_plans.xml.gz";
	private final static String eventsFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/250.events.xml.gz";
	private final static String transitScheduleFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/transitSchedule.oevnet.xml.gz";
	private final static String transitVehiclesFilename = "/Volumes/Data/projects/bvg2010/runs/2010-11-21-run01/transitVehicles_bvg_2005.xml.gz";

	private final Scenario scenario = new ScenarioImpl();

	public void readNetwork() {
		new MatsimNetworkReader(this.scenario).readFile(networkFilename);
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

	public List<Id> readIdList(final String filename) {
		ArrayList<Id> list = new ArrayList<Id>(100);
		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			String line = null;
			while ((line = reader.readLine()) != null) {
				list.add(new IdImpl(line));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		list.trimToSize();
		return list;
	}

	public void readTransitSchedule() {
		this.scenario.getConfig().scenario().setUseTransit(true);
		try {
			new TransitScheduleReader(this.scenario).readFile(transitScheduleFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	public void createRemainSeatedStats() {
		StopId2RemainSeatedDataMap remainSeated = new StopId2RemainSeatedDataMap();
		TransitSchedule ts = ((ScenarioImpl) this.scenario).getTransitSchedule();

		EventsManager em = new EventsManagerImpl();
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
		for (Map.Entry<Id, List<StopId2RemainSeatedDataMapData>> e : aggregatedMap.entrySet()) {
			Collections.sort(e.getValue(), new Comparator<StopId2RemainSeatedDataMapData>() {
				@Override
				public int compare(StopId2RemainSeatedDataMapData o1, StopId2RemainSeatedDataMapData o2) {
					return Double.compare(o1.getVehicleArrivesAtFacilityEvent().getTime(), o2.getVehicleArrivesAtFacilityEvent().getTime());
				}
			});
		}
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

	public static void main(String[] args) {
		RunAnalyses app = new RunAnalyses();
		app.readNetwork();
//		app.createPersonAttributeTable("personAttributes.txt", "allPersonIds.txt");
//		List<Id> allPersonIds = app.readIdList("allPersonIds.txt");
//		System.out.println(allPersonIds.size());
		app.readTransitSchedule();
		app.createRemainSeatedStats();
	}
}
