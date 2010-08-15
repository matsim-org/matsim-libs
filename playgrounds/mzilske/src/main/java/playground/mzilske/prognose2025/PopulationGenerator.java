package playground.mzilske.prognose2025;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PopulationGenerator implements Runnable {

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	public static class Entry {
		
		public final int source;
		public final int sink;
		public final int ptWorkTripsPerDay;
		public final int carWorkTripsPerDay;

		Entry(int source, int sink, int carWorkTripsPerYear, int ptWorkTripsPerYear) {
			this.source = source;
			this.sink = sink;
			this.carWorkTripsPerDay = (carWorkTripsPerYear / 255);
			this.ptWorkTripsPerDay = (ptWorkTripsPerYear / 255);
		}

	}
	
	public static class Zone {
		
		public final int id;

		public final int workplaces;
		
		public final int workingPopulation;
		
		public final Geometry geometry;

		public Zone(int id, int workplaces, int workingPopulation, Geometry defaultGeometry) {
			super();
			this.id = id;
			this.workplaces = workplaces;
			this.workingPopulation = workingPopulation;
			this.geometry = defaultGeometry;
		}
		
	}

	private Set<Entry> entries = new HashSet<Entry>();

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();
	
	private Scenario scenario;

	private Population population;

	private PopulationWriter populationWriter;

	public static void main(String[] args) {
		PopulationGenerator potsdamPop = new PopulationGenerator();
		potsdamPop.run();
	}

	@Override
	public void run() {
		scenario = new ScenarioImpl();
		population = scenario.getPopulation();
		for (Entry entry : entries) {
			Zone source = zones.get(entry.source);
			Zone sink = zones.get(entry.sink);
			int quantity = scale(getCarQuantity(source, sink, entry.carWorkTripsPerDay));
			createFromToCar(source, sink, quantity);
			quantity = scale(getPtQuantity(source, sink));
			createFromToPt(source, sink, quantity);
		}
		populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
	}

	void writePopulation(String filename) {
		populationWriter.write(filename);
	}
	
	public int countPersons() {
		int all = 0;
		for (Entry entry : entries) {
			Zone source = zones.get(entry.source);
			Zone sink = zones.get(entry.sink);
			int quantity = scale(getCarQuantity(source, sink, entry.carWorkTripsPerDay));
			all += quantity;
		}
		return all;
	}

	private int getPtQuantity(Zone source, Zone sink) {
		// TODO Auto-generated method stub
		return 0;
	}

	private int getCarQuantity(Zone source, Zone sink, int carWorkTripsPerDay) {
		double outWeight = ((double) source.workingPopulation * sink.workplaces) /  ((double) source.workplaces * sink.workingPopulation);
		double inWeight = ((double) source.workplaces * sink.workingPopulation) /  ((double) source.workingPopulation * sink.workplaces);
		System.out.println(sink + ": " + outWeight + " / " + inWeight);
		double outShare = outWeight / (inWeight + outWeight);
		int amount = (int) (outShare * carWorkTripsPerDay * 0.5);
		System.out.println("outCar: " + amount);
		return amount;
	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.001);
		System.out.println("scaled: " + scaled);
		return scaled;
	}

	private void createFromToCar(Zone source, Zone sink, int quantity) {
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(source, sink, i, TransportMode.car));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source);
			Coord workLocation = shoot(sink);
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private void createFromToPt(Zone source, Zone sink, int quantity) {
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(source, sink, i, TransportMode.pt));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source);
			Coord workLocation = shoot(sink);
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createPtLeg());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createPtLeg());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private Leg createDriveLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Leg createPtLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return leg;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(17*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(9*60*60);
		return activity;
	}

	private Coord shoot(Zone source) {
		Random r = new Random();
		Point point = getRandomPointInFeature(r, source.geometry);
		CoordImpl coordImpl = new CoordImpl(point.getX(), point.getY());
		// return ct.transform(coordImpl);
		return coordImpl;
	}

	private Id createId(Zone source, Zone sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source.id + "_" + sink.id + "_" + i);
	}

	public void addZone(int gemeindeschluessel, int i, int j, Geometry defaultGeometry) {
		Zone zone = new Zone(gemeindeschluessel, i, j, defaultGeometry);
		zones.put(gemeindeschluessel, zone);
	}

	public void addEntry(int quelle, int ziel, int parseInt, int parseInt2) {
		Entry entry = new Entry(quelle, ziel, parseInt, parseInt2);
		entries.add(entry);
		System.out.println(entries.size());
	}

	Map<Integer, Zone> getZones() {
		return zones;
	}


}
