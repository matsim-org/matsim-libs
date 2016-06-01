package playground.smetzler.bikeDemand;


import java.util.HashMap;
import java.util.Map;

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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class PopGeneration implements Runnable {
	
	//String outputPop = "../../../../13.Sem - Uni WS 15-16/Masterarbeit/demand/skalitzer/skalitzer_pop.xml";
	//String outputPop = "../smetzler/input/demand/equil_bike.xml";
	String outputPop = "../../../shared-svn/studies/countries/de/berlin-bike/input/demand/equil_bike.xml";
	//String DHDN = "EPSG:3068";
	private Map<String, Coord> zoneGeometries = new HashMap<>();

	//private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, DHDN);

	private Scenario scenario;

	private Population population;

	public static void main(String[] args) {
		PopGeneration Pop = new PopGeneration();
		Pop.run();
	}

	@Override
	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		fillZoneData();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(outputPop);
	}

	private void fillZoneData() {
		// Add the locations you want to use here.
		// (with proper coordinates)
		// 13.4310436,52.4944612,13.4520292,52.5024297
//		zoneGeometries.put("home1", new Coord((double) 13.4310436, (double) 52.4944612));
//		zoneGeometries.put("work1", new Coord((double) 13.4520292, (double) 52.5024297));
		zoneGeometries.put("home1", new Coord((double) -20000, (double) -10000));
		zoneGeometries.put("work1", new Coord((double) 5000, (double) -10000));
	}

	private void generatePopulation() {
		generateHomeWorkHomeTrips("home1", "work1", 20); // create 20 trips from zone 'home1' to 'work1'
		//... generate more trips here
	}

	private void generateHomeWorkHomeTrips(String from, String to, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Coord source = zoneGeometries.get(from);
			Coord sink = zoneGeometries.get(to);
			Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.bike));
			Plan plan = population.getFactory().createPlan();
//			Coord homeLocation = shoot(ct.transform(source));
//			Coord workLocation = shoot(ct.transform(sink));
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

	private Leg createDriveLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.bike);
		return leg;
	}

	private Coord shoot(Coord source) {
		// Insert code here to blur the input coordinate.
		// For example, add a random number to the x and y coordinates.
		return source;
	}

	private Activity createWork(Coord workLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(14*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(10*60*60);
		return activity;
	}

	private Id<Person> createId(String source, String sink, int i, String transportMode) {
		return Id.create(transportMode + "_" + source + "_" + sink + "_" + i, Person.class);
	}

}

