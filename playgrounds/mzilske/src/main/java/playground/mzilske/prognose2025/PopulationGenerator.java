package playground.mzilske.prognose2025;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PopulationFactoryImpl;

import playground.mzilske.pipeline.PersonSink;

public class PopulationGenerator implements TripFlowSink {

	private static final String HOME_ACTIVITY_TYPE = "pvHome";

	public static class Entry {

		public final int source;
		public final int sink;
		public final int ptWorkTripsPerDay;
		public final int carWorkTripsPerDay;

		Entry(int source, int sink, int carWorkTripsPerYear, int ptWorkTripsPerYear) {
			this.source = source;
			this.sink = sink;
			this.carWorkTripsPerDay = carWorkTripsPerYear;
			this.ptWorkTripsPerDay = ptWorkTripsPerYear;
		}

	}

	public int countPersons() {
		int all = 0;
		for (Entry entry : entries) {
			int quantity = entry.carWorkTripsPerDay;
			all += quantity;
		}
		return all;
	}

	private Collection<Entry> entries = new ArrayList<Entry>();

	private PersonSink personSink;

	private PopulationFactory populationFactory = new PopulationFactoryImpl(null);

	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset) {
		for (int i=0; i<quantity; i++) {
			Person person = populationFactory.createPerson(createId(quelle, ziel, i, mode));
			Plan plan = populationFactory.createPlan();
			Coord homeLocation = quelle.coord;
			Coord workLocation = ziel.coord;
			double workStartTime = calculateNormallyDistributedTime(8*60*60);
			double freespeedTravelTimeToWork = - departureTimeOffset;
			double homeEndTime = Math.max((workStartTime - freespeedTravelTimeToWork), 0.00);
			double workEndTime = workStartTime + 8*60*60;
			plan.addActivity(createActivity(homeLocation, HOME_ACTIVITY_TYPE, homeEndTime));
			plan.addLeg(createLeg(mode));
			plan.addActivity(createActivity(workLocation, destinationActivityType, workEndTime));
			plan.addLeg(createLeg(mode));
			plan.addActivity(createActivity(homeLocation, HOME_ACTIVITY_TYPE, homeEndTime));
			person.addPlan(plan);
			personSink.process(person);
		}
	}

	public void complete() {
		personSink.complete();
	}

	private Leg createLeg(String mode) {
		Leg leg = populationFactory.createLeg(mode);
		return leg;
	}
	
	private Activity createActivity(Coord workLocation, String activityType, double time) {
		Activity activity = populationFactory.createActivityFromCoord(activityType, workLocation);
		activity.setEndTime(time);
		return activity;
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

	private Id createId(Zone source, Zone sink, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + source.id + "_" + sink.id + "_" + i);
	}

	public void setSink(PersonSink personSink) {
		this.personSink = personSink;
	}

}
