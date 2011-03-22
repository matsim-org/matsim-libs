package tutorial.programming.example08DemandGeneration;

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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.ConfigUtils;

public class PPopulationGenerator implements Runnable {

	private static Map<String, Coord> zoneGeometries = new HashMap<String, Coord>();

	private CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

	private Scenario scenario;

	private Population population;

	public static void main(String[] args) {
		PPopulationGenerator potsdamPop = new PPopulationGenerator();
		potsdamPop.run();
	}

	@Override
	public void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		generatePopulation();
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("./input/population.xml");
	}

	private void generatePopulation() {
		generateHomeWorkHomeTrips("P", "B", 20);
	}

	private void generateHomeWorkHomeTrips(String from, String to, int quantity) {
		for (int i=0; i<quantity; ++i) {
			Coord source = zoneGeometries.get(from);
			Coord sink = zoneGeometries.get(to);
			Person person = population.getFactory().createPerson(createId(from, to, i, TransportMode.car));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = ct.transform(shoot(source));
			Coord workLocation = ct.transform(shoot(sink));
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
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Coord shoot(Coord source) {
		return source;
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

	private Id createId(String source, String sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source + "_" + sink + "_" + i);
	}

}
