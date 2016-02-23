package patryk.simulationStarters;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;

// import org.matsim.contrib.cadyts.general.ExpBetaPlanChangerWithCadytsPlanRegistration;

public final class RunWithRoadPricing {

	final static String CADYTS_STRATEGY_NAME = "CadytsAsScoring";

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		
		boolean useCadyts = false;
		if (args[1].equals("cadyts")) {
			useCadyts = true;
		}
		double cadytsWeight = 30.;

		String configFile = args[0];

		// load the config, telling it to "materialize" the road pricing
		// section:
		Config config = ConfigUtils.loadConfig(configFile,
				new RoadPricingConfigGroup());

		//if (useCadyts) {
		//	registerCadytsStrategy(config);
		//}

		// load the scenario:
		Scenario scenario = ScenarioUtils.loadScenario(config);

		/*
		 * ObjectAttributes objAttr = new ObjectAttributes();
		 * ObjectAttributesXmlReader reader = new
		 * ObjectAttributesXmlReader(objAttr);
		 * reader.parse("toll_sthlm/agentAttributes.xml");
		 */

		// Population population = scenario.getPopulation();

		// int i = 0;
		// for(Person p : population.getPersons().values()) {
		// if (i % 100 == 0) {
		// Id<Person> persID = p.getId();
		// System.out.println(persID.toString());
		// PlanCalcScoreConfigGroup.ActivityParams configActParam =
		// new
		// PlanCalcScoreConfigGroup.ActivityParams(objAttr.getAttribute(persID.toString(),
		// "Income").toString());
		// configActParam.setOpeningTime(12000);
		// config.planCalcScore().addActivityParams(configActParam);
		// }
		// i++;
		// }
		//

		// PlanCalcScoreConfigGroup.ActivityParams configActParam = new
		// PlanCalcScoreConfigGroup.ActivityParams("other");
		// configActParam.setOpeningTime(12000);
		// config.planCalcScore().addActivityParams(configActParam);

		// instantiate the controler:
		Controler controler = new Controler(scenario);

		if (useCadyts)
			registerCadyts(controler, cadytsWeight);

		// use the road pricing module.
		// (loads the road pricing scheme, uses custom travel disutility
		// including tolls, etc.)
		controler.setModules(new ControlerDefaultsWithRoadPricingModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		// run the controler:
		controler.run();

		// ConfigWriter cfgWriter = new ConfigWriter(config);
		// cfgWriter.write("config_utskrift.xml");
	}

//	private static void registerCadytsStrategy(Config config) {
//		// probably remove standard PlansSelector?
//		StrategySettings strategySettings = new StrategySettings(
//				ConfigUtils.createAvailableStrategyId(config));
//		strategySettings.setStrategyName(CADYTS_STRATEGY_NAME);
//		strategySettings.setWeight(1.0);
//		config.strategy().addStrategySettings(strategySettings);
//	}

	private static void registerCadyts(final Controler controler,
			final double cadytsWeight) {
		final Config config = controler.getConfig();
		// create the cadyts context and add it to the control(l)er:
		
		// TODO this does not compile any more
//		controler.addOverridingModule(new CadytsCarModule());

		// the following is a standard ExpBetaPlanChanger with cadyts plans
		// registration added (would be nice to get rid of this but
		// haven't found an easy way)

		// >>>>> GUNNAR CHANGED THIS >>>>>

		/*
		 * Patryk, jag har tittat på den nya versionen av RunCadyts4CarExample.
		 * Det ser ut som om man helt enkelt inte behöver den nedanstående koden
		 * längre och istället skulle nu lägga till en normal ExpBetaPlanChanger
		 * i config filen. Vore det mögligt att du testar om jag har rätt med
		 * detta? Gunnar
		 */

		// controler.addPlanStrategyFactory(CADYTS_STRATEGY_NAME, new
		// PlanStrategyFactory() {
		// @Override
		// public PlanStrategy createPlanStrategy(Scenario scenario,
		// EventsManager eventsManager) {
		// PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new
		// ExpBetaPlanChangerWithCadytsPlanRegistration<Link>(
		// scenario.getConfig().planCalcScore().getBrainExpBeta(), cContext)) ;
		// return builder.build() ;
		// }
		// } ) ;

		// <<<<< GUNNAR CHANGED THIS

		// include cadyts into the plan scoring (this will add the cadyts
		// corrections to the scores):
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject private CharyparNagelScoringParametersForPerson parameters;
			@Inject private CadytsContext cadytsContext;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {

				final CharyparNagelScoringParameters params = parameters.getScoringParameters(person);

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction
						.addScoringFunction(new CharyparNagelActivityScoring(
								params));
				sumScoringFunction
						.addScoringFunction(new CharyparNagelLegScoring(params,
								controler.getScenario().getNetwork()));
				sumScoringFunction
						.addScoringFunction(new CharyparNagelMoneyScoring(
								params));
				sumScoringFunction
						.addScoringFunction(new CharyparNagelAgentStuckScoring(
								params));

				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(
						person.getSelectedPlan(), config, cadytsContext);
				final double cadytsScoringWeight = cadytsWeight
						* config.planCalcScore().getBrainExpBeta();
				scoringFunction
						.setWeightOfCadytsCorrection(cadytsScoringWeight);
				sumScoringFunction.addScoringFunction(scoringFunction);

				return sumScoringFunction;
			}
		});
	}

}
