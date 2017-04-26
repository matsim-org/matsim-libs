package playground.santiago.run;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;

import playground.santiago.SantiagoScenarioConstants;

public class RunExampleScenario {
	

	private static String inputPath = "C:/Users/Tesista/Desktop/LeoCamus/runs-svn/santiago/tests/";
	private static String configFile = inputPath + "config.xml";
	
	private static String testType = "Cadyts+roadPricing";
	
	

	
	
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		/** Adding traffic counts **/
		CountsConfigGroup counts = config.counts();
		counts.setInputFile("C:/Users/Tesista/Desktop/LeoCamus/runs-svn/santiago/tests/input/counts100.xml");
		counts.setOutputFormat("all");
		counts.setWriteCountsInterval(1);		
		/** **/
		
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		
		
		if (testType.equals("base")){
			controler.run();
			
		} else if (testType.equals("Cadyts")){
			
			controler.addOverridingModule(new CadytsCarModule());
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
				@Inject CadytsContext cadytsContext;
				@Inject ScoringParametersForPerson parameters;
				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					final ScoringParameters params = parameters.getScoringParameters(person);
					
					SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
					scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
					scoringFunctionAccumulator.addScoringFunction(scoringFunction );

					return scoringFunctionAccumulator;
				}
			}) ;
			controler.run();
			
		} else if (testType.equals("roadPricing")) {
			
			String gantriesFile = inputPath + "input/gantries3.xml";
			int sigma = 3;
			
			RoadPricingConfigGroup rpcg = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
			rpcg.setTollLinksFile(gantriesFile);
			
			//Adding randomness to the router, sigma = 3
			config.plansCalcRoute().setRoutingRandomness(sigma);
			controler.addOverridingModule(new RoadPricingModule());			
			controler.run();
			
		} else {
			
			String gantriesFile = inputPath + "input/gantries3.xml";
			int sigma = 3;
			
			RoadPricingConfigGroup rpcg = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
			rpcg.setTollLinksFile(gantriesFile);
			
			//Adding randomness to the router, sigma = 3
			config.plansCalcRoute().setRoutingRandomness(sigma);
			controler.addOverridingModule(new RoadPricingModule());
			
			controler.addOverridingModule(new CadytsCarModule());
			controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
				@Inject CadytsContext cadytsContext;
				@Inject ScoringParametersForPerson parameters;
				@Override
				public ScoringFunction createNewScoringFunction(Person person) {
					final ScoringParameters params = parameters.getScoringParameters(person);
					
					SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
					scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

					final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
					scoringFunction.setWeightOfCadytsCorrection(30. * config.planCalcScore().getBrainExpBeta()) ;
					scoringFunctionAccumulator.addScoringFunction(scoringFunction );

					return scoringFunctionAccumulator;
				}
			}) ;
			controler.run();
			
			
			
		}
		
		
	
		
		
	}
	
	
}
