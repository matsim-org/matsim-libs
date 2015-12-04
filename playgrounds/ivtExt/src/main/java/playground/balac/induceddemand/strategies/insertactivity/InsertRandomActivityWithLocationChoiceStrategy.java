package playground.balac.induceddemand.strategies.insertactivity;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.MaxDCScoreWrapper;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import playground.balac.induceddemand.strategies.InsertRandomActivity;

public class InsertRandomActivityWithLocationChoiceStrategy implements PlanStrategy {
	private PlanStrategyImpl planStrategyDelegate;
	private final QuadTree shopFacilityQuadTree;
	private final QuadTree leisureFacilityQuadTree;
	private Scenario scenario;

		
	@Inject
	public  InsertRandomActivityWithLocationChoiceStrategy(final Scenario scenario, 
			@Named("shopQuadTree") QuadTree shopFacilityQuadTree,
			@Named("leisureQuadTree") QuadTree leisureFacilityQuadTree) {
		
	   // PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
	   
		this.scenario = scenario;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
	}	
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
		
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		 InsertRandomActivity ira = new InsertRandomActivity(scenario, shopFacilityQuadTree,
		    		leisureFacilityQuadTree);
	
		/*
		 * Somehow this is ugly. Should be initialized in the constructor. But I do not know, how to initialize the lc scenario elements
		 * such that they are already available at the time of constructing this object. ah feb'13
		 */
		DestinationChoiceBestResponseContext lcContext = (DestinationChoiceBestResponseContext) scenario.getScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME);
		Config config = lcContext.getScenario().getConfig();
		DestinationChoiceConfigGroup dccg = (DestinationChoiceConfigGroup) config.getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		MaxDCScoreWrapper maxDcScoreWrapper = (MaxDCScoreWrapper)scenario.getScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME);
		if ( !DestinationChoiceConfigGroup.Algotype.bestResponse.equals(dccg.getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}		
		String planSelector = dccg.getPlanSelector();
		if (planSelector.equals("BestScore")) {
			planStrategyDelegate = new PlanStrategyImpl(new BestPlanSelector<Plan, Person>());
		} else if (planSelector.equals("ChangeExpBeta")) {
			planStrategyDelegate = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			planStrategyDelegate = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		}
		planStrategyDelegate.addStrategyModule(new TripsToLegsModule(scenario.getConfig()));
		planStrategyDelegate.addStrategyModule(ira);
		planStrategyDelegate.addStrategyModule(new BestReplyDestinationChoice(lcContext, maxDcScoreWrapper.getPersonsMaxDCScoreUnscaled()));
		planStrategyDelegate.addStrategyModule(new ReRoute(lcContext.getScenario()));
		planStrategyDelegate.init(replanningContext);
		
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
		
	}

}
