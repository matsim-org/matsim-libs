package playground.mzilske.ulm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class GeneratePopulation {

	private Population population;

	private TransitSchedule transitSchedule;

	private List<TransitStopFacility> facs;

	public GeneratePopulation(Scenario scenario)  {
		this.population = scenario.getPopulation();
		this.transitSchedule = scenario.getTransitSchedule();
		this.facs = new ArrayList<TransitStopFacility>(transitSchedule.getFacilities().values());
	}

	public void run() {
		for (int i=0; i<100; ++i) {
			Coord source = randomCoord();
			Coord sink = randomCoord();
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();
			Activity morning = population.getFactory().createActivityFromCoord("home", source);
			morning.setEndTime(9*60*60);
			plan.addActivity(morning);
			
			List<Leg> homeWork = createLeg(source, sink);
			for (Leg leg : homeWork) {
				plan.addLeg(leg);
			}
			
			final Activity work = population.getFactory().createActivityFromCoord("work", sink);
			work.setEndTime(17*60*60);
			plan.addActivity(work);
			
			List<Leg> workHome = createLeg(sink, source);
			for (Leg leg : workHome) {
				plan.addLeg(leg);
			}
			
			Activity night = population.getFactory().createActivityFromCoord("home", source);
			plan.addActivity(night);
			
			person.addPlan(plan);
			population.addPerson(person);
		}

	}

	private Coord randomCoord() {
		int nFac = (int) (transitSchedule.getFacilities().size() * Math.random());
		Coord coord = facs.get(nFac).getCoord();
		return new CoordImpl(coord.getX() + Math.random()*1000, coord.getY() + Math.random()*1000);
	}

	private List<Leg> createLeg(Coord source, Coord sink) {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return Arrays.asList(new Leg[]{leg});
	}

}
