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

	public void complete() {
		personSink.complete();
	}

	private Leg createLeg(String mode) {
		Leg leg = populationFactory.createLeg(mode);
		return leg;
	}

//	private Activity createWork(Coord workLocation, String destinationActivityType) {
//		Activity activity = populationFactory.createActivityFromCoord(destinationActivityType, workLocation);
//		activity.setEndTime(calculateRandomEndTime(17*60*60));
//		return activity;
//	}
//
//	private Activity createHome(Coord homeLocation, double travelTimeOffset) {
//		Activity activity = populationFactory.createActivityFromCoord("home", homeLocation);
//		activity.setEndTime(calculateRandomEndTime(7*60*60) + travelTimeOffset);
//		return activity;
//	}
	
	private Activity createWork(Coord workLocation, double workStartTime, String destinationActivityType) {
		Activity activity = populationFactory.createActivityFromCoord(destinationActivityType, workLocation);
		activity.setEndTime(workStartTime + 8*60*60);
		return activity;
	}

	private Activity createHome(Coord homeLocation, double workStartTime, double freespeedTravelTime2Work, double travelTimeOffset) {
		Activity activity = populationFactory.createActivityFromCoord("pvHome", homeLocation);
		activity.setEndTime(Math.max((workStartTime - freespeedTravelTime2Work), 0.00));
//		System.out.println("WorkStartTime: " + workStartTime + "\t" + "TravelTime2Work: " + freespeedTravelTime2Work);
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

//	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double travelTimeOffset) {
//		for (int i=0; i<quantity; i++) {
//			Person person = populationFactory.createPerson(createId(quelle, ziel, i, mode));
//			Plan plan = populationFactory.createPlan();
//			Coord homeLocation = quelle.coord;
//			Coord workLocation = ziel.coord;
//			plan.addActivity(createHome(homeLocation, travelTimeOffset));
//			plan.addLeg(createDriveLeg());
//			plan.addActivity(createWork(workLocation, destinationActivityType));
//			plan.addLeg(createDriveLeg());
//			plan.addActivity(createHome(homeLocation, 0.0));
//			person.addPlan(plan);
//			personSink.process(person);
//		}
//	}
	
	/* @ Michael: "travelTimeOffset" wird hier uminterpretiert in "freespeedTravelTime2Work"
	 * Das ist unschön, vor allem, weil in createHome ein "travelTimeOffset" = 0.0 reingeht, welches nicht ersterem entspricht!
	 * Dies funktioniert also für den pendlerVerkehr, ist aber glaub ich nicht allgemeingültig...
	 * >> also ggf. glatt ziehen :)
	 */
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double travelTimeOffset) {
		for (int i=0; i<quantity; i++) {
			Person person = populationFactory.createPerson(createId(quelle, ziel, i, mode));
			Plan plan = populationFactory.createPlan();
			Coord homeLocation = quelle.coord;
			Coord workLocation = ziel.coord;
			// workStartTimePeak is assumed to be at 7:30am
			double workStartTime = calculateNormallyDistributedTime(8*60*60 /*+ 30*60*/);
			double freespeedTravelTime2Work = - travelTimeOffset;
			
			plan.addActivity(createHome(homeLocation, workStartTime, freespeedTravelTime2Work, 0.0));
			plan.addLeg(createLeg(mode));
			plan.addActivity(createWork(workLocation, workStartTime, destinationActivityType));
			plan.addLeg(createLeg(mode));
			plan.addActivity(createHome(homeLocation, workStartTime, freespeedTravelTime2Work, 0.0));
			person.addPlan(plan);
			personSink.process(person);
		}
	}
	
//	private double calculateFreespeedTravelTime(Zone quelle, Zone ziel) {
//		double freespeedTravelTime = 0.0;
//		
//		Node quellNode = ((NetworkImpl) network).getNearestNode(quelle.coord);
//		Node zielNode = ((NetworkImpl) network).getNearestNode(ziel.coord);
//		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0);
//		
//		for (Link l : path.links) {
//			freespeedTravelTime += l.getLength() / l.getFreespeed();
//		}
//		return freespeedTravelTime;
//	}

	public void setSink(PersonSink personSink) {
		this.personSink = personSink;
	}

}
