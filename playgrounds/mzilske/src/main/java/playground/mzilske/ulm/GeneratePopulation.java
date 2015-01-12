package playground.mzilske.ulm;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class GeneratePopulation {

	private Population population;

	private TransitSchedule transitSchedule;

	private List<TransitStopFacility> facs;

	public GeneratePopulation(Scenario scenario)  {
		this.population = scenario.getPopulation();
		this.transitSchedule = scenario.getTransitSchedule();
		this.facs = new ArrayList<>(transitSchedule.getFacilities().values());
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
			
			List<Leg> homeWork = createLeg();
			for (Leg leg : homeWork) {
				plan.addLeg(leg);
			}
			
			final Activity work = population.getFactory().createActivityFromCoord("work", sink);
			work.setEndTime(17*60*60);
			plan.addActivity(work);
			
			List<Leg> workHome = createLeg();
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

	private List<Leg> createLeg() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return Arrays.asList(leg);
	}

}
