package playground.mzilske.teach;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class PotsdamPop implements Runnable {

	private static final String PLANS_FILE = "input/plans.xml";

	private static final String FILENAME = "../../brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis_with_berlin.shp";

	private static Coord drawRandomPointFromGeometry(Geometry g) {
		Random rnd = new Random();
		Point p;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		Coord coord = new CoordImpl(p.getX(), p.getY());
		return coord;
	}

	public static enum Relation {
		P_B(12054000, 11000000, 13281), 
		P_PM(12054000,12069000, 5981), 
		P_HVL(12054000,12063000, 764), 
		P_BRB(12054000,12051000, 509), 
		P_TF(12054000, 12072000, 1624), 
		P_P(12054000, 12054000, 28863),
		B_P(11000000, 12054000, 13014),
		PM_P(12069000, 12054000, 13847),
		HVL_P(12063000, 12054000, 2515),
		BRB_P(12051000, 12054000, 1680),
		TF_P(12072000, 12054000, 2966);

		public final int source_zone;
		public final int sink_zone;
		public final int workTripsPerDay;
		
		Relation(int source_zone, int sink_zone, int workTripsPerDay) {
			this.source_zone = source_zone;
			this.sink_zone = sink_zone;
			this.workTripsPerDay = workTripsPerDay;
		}

	}

	private static EnumSet<Relation> relations = EnumSet.allOf(Relation.class);

	private Map<Integer, Geometry> zoneGeometries = new HashMap<Integer, Geometry>();

	// private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM35S);

	private CoordinateTransformation ct = new IdentityTransformation();
	
	private Scenario scenario;

	private Population population;




	public static void main(String[] args) {
		PotsdamPop potsdamPop = new PotsdamPop();
		potsdamPop.readShapeFile();
		potsdamPop.run();
	}



	private void readShapeFile() {
		ShapeFileReader reader = new ShapeFileReader();
		Set<Feature> features = reader.readFileAndInitialize(FILENAME);
		for (Feature feature : features) {
			zoneGeometries.put(Integer.parseInt((String) feature.getAttribute("Nr")), feature.getDefaultGeometry());
		}
	}

	@Override
	public void run() {
		scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		for (Relation relation : relations) {
			int quantity = scale(getCarQuantityOut(relation.workTripsPerDay));
			createFromToCar(relation.source_zone, relation.sink_zone, quantity);
			quantity = scale(getPtQuantityOut(relation));
			createFromToPt(relation.source_zone, relation.sink_zone, quantity);
		}
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(PLANS_FILE);
	}

	private int getPtQuantityOut(Relation relation) {
		return 0;
	}



	private int getCarQuantityOut(int workTripsPerDay) {
		// car market share for commuter work/education trips (taken from "Regionaler Nahverkehrsplan-Fortschreibung, MVV 2007)
		double carMarketShare = 0.67;
		// scale factor, since Pendlermatrix only considers "sozialversicherungspflichtige Arbeitnehmer" (taken from GuthEtAl2005)
		double scaleFactor = 1.29;
		return (int) ((carMarketShare * scaleFactor) * workTripsPerDay);
	}



	private int scale(int quantityOut) {
		int scaled = (int) (quantityOut * 0.01);
		System.out.println("scaled: " + scaled);
		return scaled;
	}


	private void createFromToCar(int source_zone, int sink_zone, int quantity) {
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(source_zone, sink_zone, i, TransportMode.car));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source_zone);
			Coord workLocation = shoot(sink_zone);
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLeg());
			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private void createFromToPt(int source_zone, int sink_zone, int quantity) {
		for (int i=0; i<quantity; i++) {
			Person person = population.getFactory().createPerson(createId(source_zone, sink_zone, i, TransportMode.pt));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source_zone);
			Coord workLocation = shoot(sink_zone);
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
		activity.setEndTime(calculateNormallyDistributedTime(19*60*60));
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(calculateNormallyDistributedTime(7*60*60));
		return activity;
	}

	private Coord shoot(int id) {
		Geometry g = zoneGeometries.get(id);
		if (g == null) {
			throw new RuntimeException("No geometry for zone "+id);
		}
		Coord point = drawRandomPointFromGeometry(g);
		return ct.transform(point);
	}

	private Id createId(int source_zone, int sink_zone, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source_zone + "_" + sink_zone + "_" + i);
	}

	private double calculateNormallyDistributedTime(int i) {
		Random random = new Random();
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		//linear transformation in order to optain N[i,7200²]
		double endTimeInSec = i + 120 * 60 * normal ;
		return endTimeInSec;
	}

}
