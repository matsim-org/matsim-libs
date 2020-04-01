package playground.vsp.demandde.pendlermatrix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.vsp.pipeline.PersonSink;
import playground.vsp.pipeline.PersonSinkSource;

public class PersonGeoTransformatorTask implements PersonSinkSource {

	private PersonSink sink;
	
	private CoordinateTransformation transformation;
	
	public PersonGeoTransformatorTask(String from, String to) {
		this.transformation = TransformationFactory.getCoordinateTransformation(from, to);
	}

	@Override
	public void complete() {
		sink.complete();
	}

	@Override
	public void process(Person person) {
		Plan plan = person.getPlans().get(0);
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				Coord oldCoord = activity.getCoord();
				Coord newCoord = transformation.transform(oldCoord);
				activity.setCoord(newCoord);
			} 
		}
		sink.process(person);
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

}
