package org.matsim.contrib.pseudosimulation.replanning;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

import java.util.Map;


/**
 * Created by fouriep on 4/16/15.
 */
public class DistributedPlanMutatorStrategy implements PlanStrategy {
    private final char gene;
    String delegateName;
    PlanCatcher slave;
    MatsimServices controler;
    private PlanStrategy delegate;

    public DistributedPlanMutatorStrategy(String delegateName, PlanCatcher slave, MatsimServices controler, char gene) {
        this.delegateName = delegateName;
        this.slave = slave;
        this.controler = controler;
        this.gene =gene;
    }


    @Override
    public void run(HasPlansAndId<Plan, Person> person) {
        delegate.run(person);
        slave.addPlansForPsim(person.getSelectedPlan());
        //TODO: genometracking
    }

    @Override
    public void init(ReplanningContext replanningContext) {
        if (delegate == null) {
            delegate =  controler.getInjector().getBinding(Key.get(PlanStrategy.class, Names.named(delegateName))).getProvider().get();
        }
        delegate.init(replanningContext);
    }

    @Override
    public void finish() {
        delegate.finish();
    }
}
