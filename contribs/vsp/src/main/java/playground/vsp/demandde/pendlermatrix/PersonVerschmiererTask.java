package playground.vsp.demandde.pendlermatrix;

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.vsp.pipeline.PersonSink;
import playground.vsp.pipeline.PersonSinkSource;

public class PersonVerschmiererTask implements PersonSinkSource {

	private Verschmierer verschmierer;
	
	private PersonSink sink;
	
	int verschmiert = 0;
	int nichtVerschmiert = 0;

	public PersonVerschmiererTask(String shapeFilename) {
		this.verschmierer = new Verschmierer(shapeFilename);
	}

	@Override
	public void complete() {
		sink.complete();
	}

	@Override
	public void process(Person person) {
		Plan plan = person.getPlans().get(0);
		
		List<PlanElement> planElements = plan.getPlanElements();
		Activity home1 = (Activity) planElements.get(0);
		Activity work = (Activity) planElements.get(2);
		Activity home2 = (Activity) planElements.get(4);
		
		Coord oldCoordHome = home1.getCoord();
		Coord oldCoordWork = work.getCoord();
		
		Coord newCoordHome = verschmierer.shootIntoSameZoneOrLeaveInPlace(oldCoordHome);
		Coord newCoordWork = verschmierer.shootIntoSameZoneOrLeaveInPlace(oldCoordWork);

		home1.setCoord(newCoordHome);
		work.setCoord(newCoordWork);
		home2.setCoord(newCoordHome);
		
		sink.process(person);
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

}
