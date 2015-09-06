package playground.mzilske.teach.tasks2012;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class OwnScoringFunctionAndReplanning {
	
	private static class MyTimeMutator implements PlanStrategyModule {

		@Override
		public void finishReplanning() {
			// TODO Auto-generated method stub

		}

		@Override
		public void handlePlan(Plan plan) {
			for ( PlanElement pe : plan.getPlanElements() ) {
				if ( pe instanceof Activity ) {
					// ...
				}
			}
		}

		@Override
		public void prepareReplanning(ReplanningContext replanningContext) {
			// TODO Auto-generated method stub

		}

	}
	
	private static class MyScoringFunction implements ScoringFunction {
		
		ScoringFunction delegate;
		private Plan plan;

		public MyScoringFunction(Plan plan, ScoringFunction scoringFunction) {
			this.plan = plan;
			this.delegate = scoringFunction;
		}

		@Override
		public void handleActivity(Activity activity) {
			// Bewerte zu früh oder zu spät auf der Arbeit auftauchen.
			delegate.handleActivity(activity);
		}

		@Override
		public void handleLeg(Leg leg) {
			delegate.handleLeg(leg);
		}

		@Override
		public void agentStuck(double time) {
			delegate.agentStuck(time);
		}

		@Override
		public void addMoney(double amount) {
			delegate.addMoney(amount);
		}

		@Override
		public void finish() {
			delegate.finish();
		}

		@Override
		public double getScore() {
			return delegate.getScore();
		}

		@Override
		public void handleEvent(Event event) {
			// TODO Auto-generated method stub
			
		}

	}

	private static class MyScoringFunctionFactory implements
			ScoringFunctionFactory {

		private CharyparNagelScoringFunctionFactory delegate;
		
		public MyScoringFunctionFactory(Config config, Network network) {
			this.delegate = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), config.scenario(), network);
		}

		@Override
		public ScoringFunction createNewScoringFunction(Person person) {
			return new MyScoringFunction(person.getSelectedPlan(), delegate.createNewScoringFunction(person));
		}

	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		ObjectAttributes workTimes = new ObjectAttributes();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			workTimes.putAttribute(person.getId().toString(), "workStartTime", 12345.0);
		}
		new ObjectAttributesXmlWriter(workTimes).writeFile("output/worktimes.xml");
		
		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.setScoringFunctionFactory(new MyScoringFunctionFactory(config, scenario.getNetwork()));
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent controlerEvent) {
				PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector()) ;
				strategy.addStrategyModule(new MyTimeMutator() ) ;
				controler.getStrategyManager().addStrategyForDefaultSubpopulation(strategy, 1.0 ) ;
			}
		}) ;
		controler.run();
	}

}
