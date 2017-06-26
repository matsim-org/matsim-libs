package playground.clruch.trb18.replanning;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 * Only switches main mode legs to AV and back
 *
 * WE PROBABLY DON'T NEED TIHS! NEW IDEA! (Plan repositories only contain 2 plans ...)
 */
public class ModeChoiceStrategyForTRB implements PlanStrategy {
    @Override
    public void run(HasPlansAndId<Plan, Person> person) {

    }

    @Override
    public void init(ReplanningContext replanningContext) {

    }

    @Override
    public void finish() {

    }
}
