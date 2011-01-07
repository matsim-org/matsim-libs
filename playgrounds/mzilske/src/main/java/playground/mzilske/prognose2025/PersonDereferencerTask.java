package playground.mzilske.prognose2025;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.mzilske.pipeline.PersonSink;
import playground.mzilske.pipeline.PersonSinkSource;

public class PersonDereferencerTask implements PersonSinkSource {

	private PersonSink sink;

	@Override
	public void complete() {
		sink.complete();
	}

	@Override
	public void process(Person person) {
		Plan plan = person.getPlans().get(0);
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				ActivityImpl activity = (ActivityImpl) planElement;
				activity.setLinkId(null);
			} else if (planElement instanceof Leg) {
				LegImpl leg = (LegImpl) planElement;
				leg.setRoute(null);
			}
		}
		sink.process(person);
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

}
