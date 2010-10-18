package playground.mzilske.prognose2025;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class PopulationGenerator implements Runnable {

	private static final Logger log = Logger.getLogger(PopulationGenerator.class);

	private Random random = MatsimRandom.getLocalInstance();

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

		public final Coord coord;

		public Geometry geometry;

		public Zone(int id, int workplaces, int workingPopulation, Coord coord) {
			super();
			this.id = id;
			this.workplaces = workplaces;
			this.workingPopulation = workingPopulation;
			this.coord = coord;
		}

	}

	private Collection<Entry> entries = new ArrayList<Entry>();

	private Map<Integer, Zone> zones = new HashMap<Integer, Zone>();

	private Scenario scenario;

	private PopulationImpl population;

	private PopulationWriter populationWriter;

	private CoordinateTransformation DhdnGk4Towgs84 = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);

	private String filename; 

	@Override
	public void run() {
		scenario = new ScenarioImpl();
		population = (PopulationImpl) scenario.getPopulation();
		population.setIsStreaming(true);
		populationWriter = new PopulationWriter(population, scenario.getNetwork());
		population.addAlgorithm(populationWriter);
		populationWriter.startStreaming(filename);
		
		int i=0;
		for (Entry entry : entries) {
			Zone source = zones.get(entry.source);
			Zone sink = zones.get(entry.sink);
			int quantity = scale(getCarQuantity(source, sink, entry.carWorkTripsPerDay));
			createFromToCar(source, sink, quantity);
			quantity = scale(getPtQuantity(source, sink));
			createFromToPt(source, sink, quantity);
			log.info(++i + " / " + entries.size());
		}
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
		// System.out.println(source.id + " -> " + sink.id + ": " + outWeight + " / " + inWeight);
		double outShare = outWeight / (inWeight + outWeight);
		int amount = (int) (outShare * carWorkTripsPerDay * 0.5);
		return amount;
	}

	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.1);
		if (quantityOut != 0) {
			System.out.println("quantity: " + quantityOut);
			System.out.println("scaled: " + scaled);
		}
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
		activity.setEndTime(calculateRandomEndTime(17*60*60));
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(calculateRandomEndTime(8*60*60));
		return activity;
	}

	private double calculateRandomEndTime(int i) {
		Random random = new Random();
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();

		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		//linear transformation in order to optain N[i,3600²]
		double endTimeInSec = 60 * 60 * normal + i;
		return endTimeInSec;
	}

	private Coord shoot(Zone source) {
		if (source.geometry != null) {
			return DhdnGk4Towgs84.transform(doShoot(source));
		} else {
			return DhdnGk4Towgs84.transform(source.coord);
		}
	}

	private Coord doShoot(Zone source) {
		Coord coord;
		Point point = getRandomPointInFeature(this.random, source.geometry);
		coord = new CoordImpl(point.getX(), point.getY());
		return coord;
	}

	private Id createId(Zone source, Zone sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source.id + "_" + sink.id + "_" + i);
	}

	public void addZone(int gemeindeschluessel, Geometry defaultGeometry) {
		Zone zone = zones.get(gemeindeschluessel);
		if (zone == null) {
			log.error("No such zone. " + gemeindeschluessel);
		} else {
			zone.geometry = defaultGeometry;
		}
	}

	public void addEntry(int quelle, int ziel, int parseInt, int parseInt2) {
		Entry entry = new Entry(quelle, ziel, parseInt, parseInt2);
		entries.add(entry);
		System.out.println(entries.size());
	}

	public void addNode(int zoneId, int i, int j, Coord coord) {
		Zone zone = new Zone(zoneId, i, j, coord);
		zones.put(zoneId, zone);
	}

	Map<Integer, Zone> getZones() {
		return zones;
	}

	Zone findZone(Coord coord) {
		GeometryFactory gf = new GeometryFactory();
		Point point = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for (Zone zone : zones.values()) {
			if (zone.geometry != null && zone.geometry.contains(point)) {
				return zone;
			}
		}
		log.error("Cannot find zone for coordinate: " + coord);
		return null;
	}

	Coord shootIntoSameZoneOrLeaveInPlace(Coord coord) {
		Zone zone = findZone(coord);
		if (zone != null) {
			return doShoot(zone);
		} else {
			return coord;
		}
	}

	public void setFilename(String filename2) {
		this.filename = filename2;
	}

}
