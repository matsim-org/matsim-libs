package playground.sebhoerl.avtaxi.replanning;

import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.routing.AVRoute;

@Singleton
public class AVOperatorChoiceStrategy implements PlanStrategy {
    @Inject
    private Map<Id<AVOperator>, AVOperator> operators;

    @Override
    public void run(HasPlansAndId<Plan, Person> person) {
	for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
	    if (element instanceof Leg && ((Leg) element).getMode().equals(AVModule.AV_MODE)) {
		AVRoute route = (AVRoute) ((Leg) element).getRoute();
		route.setOperatorId(chooseRandomOperator());
	    }
	}
    }

    @Override
    public void init(ReplanningContext replanningContext) {
	// TODO jan intentionally empty?
    }

    @Override
    public void finish() {
	// TODO jan intentionally empty?
    }

    public Id<AVOperator> chooseRandomOperator() {
	int draw = MatsimRandom.getRandom().nextInt(operators.size());
	Iterator<Id<AVOperator>> iterator = operators.keySet().iterator();

	for (int i = 0; i < draw; i++) {
	    iterator.next();
	}

	return iterator.next();
    }
}
