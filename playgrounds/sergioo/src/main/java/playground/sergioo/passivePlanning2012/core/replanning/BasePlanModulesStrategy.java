package playground.sergioo.passivePlanning2012.core.replanning;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.utils.misc.Counter;

import playground.sergioo.passivePlanning2012.api.population.BasePerson;
import playground.sergioo.passivePlanning2012.api.population.EmptyTime;
import playground.sergioo.passivePlanning2012.core.population.EmptyTimeImpl;
import playground.sergioo.passivePlanning2012.core.router.EmptyLegRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BasePlanModulesStrategy implements PlanStrategy {

	//Attributes
	private final List<PlanStrategyModule> modules = new ArrayList<PlanStrategyModule>();
	private Counter counter;
	private ReplanningContext replanningContext;
	private Collection<BasePerson> persons = new ArrayList<BasePerson>();
	public final Scenario scenario;
	
	public BasePlanModulesStrategy(Scenario scenario) {
		super();
		this.scenario = scenario;
	}
	//Methods
	public int getNumberOfStrategyModules() {
		return modules.size();
	}
	public void addStrategyModule(final PlanStrategyModule module) {
		this.modules.add(module);
	}
	@Override
	public void run(HasPlansAndId<Plan, Person> plansId) {
		if(plansId instanceof BasePerson)
			persons.add((BasePerson)plansId);
	}
	@Override
	public void init(ReplanningContext replanningContext) {
		counter = new Counter("[BasePlanStrategy] handled person # ");
		replanningContext.getTripRouter().setRoutingModule("empty", LegRouterWrapper.createLegRouterWrapper("empty", scenario.getPopulation().getFactory(), new EmptyLegRouter()));
		this.replanningContext = replanningContext;
	}
	@Override
	public void finish() {
		Collection<Plan> plans = new ArrayList<Plan>();
		for(BasePerson person : persons) {
			Plan plan = new PlanImpl(person);
			for(int i=0; i<person.getBasePlan().getPlanElements().size(); i++) {
				PlanElement planElement = person.getBasePlan().getPlanElements().get(i);
				if(planElement instanceof Activity)
					plan.addActivity(new ActivityImpl((Activity) planElement));
				else if(planElement instanceof Leg)
					if(planElement instanceof EmptyTime)
						plan.addLeg(new EmptyTimeImpl((EmptyTime) planElement));
					else
						plan.addLeg(new LegImpl((LegImpl)planElement));
			}
			if(person.addPlan(plan))
				((PersonImpl)person).setSelectedPlan(plan);
			plans.add(plan);
			counter.incCounter();
		}
		for(PlanStrategyModule module : this.modules) {
			module.prepareReplanning(replanningContext);
			for(Plan plan : plans)
				module.handlePlan(plan);
			module.finishReplanning();
		}
		persons.clear();
	}

}
