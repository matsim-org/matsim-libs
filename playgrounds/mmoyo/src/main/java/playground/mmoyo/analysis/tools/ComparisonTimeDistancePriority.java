package playground.mmoyo.analysis.tools;

import java.io.FileNotFoundException;
import java.text.DecimalFormat;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.population.algorithms.PlansFilterByLegMode;

import playground.mmoyo.PTRouter.PTValues;
import playground.mmoyo.TransitSimulation.PlanRouter;
import playground.mmoyo.analysis.tools.PopulationPtAnalyzer;
import playground.mmoyo.utils.TransScenarioLoader;

/**Uses the router with increasing time priority from 0 to 100  **/ 
public class ComparisonTimeDistancePriority {

	public static void main(String[] args) {
		String configFile= null;
		if (args.length>0) {
			configFile = args[0];	
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5xptPlansOnly_noRouted.xml";	
		}
		
		PTValues.routerCalculator = 3;
		PTValues.transferPenalty = 0;

		for (double x= 0; x<=1.01; x= x+ 0.05 ){
			DecimalFormat twoDForm = new DecimalFormat("#.##");
			PTValues.timeCoefficient = Double.valueOf(twoDForm.format(x));
			PTValues.distanceCoefficient= Math.abs(Double.valueOf(twoDForm.format(1-x)));
			
			PTValues.scenarioName = "time" + PTValues.timeCoefficient + "_dist" + PTValues.distanceCoefficient;

			ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile);

			//Get rid of only car plans
			PlansFilterByLegMode plansFilter = new PlansFilterByLegMode(TransportMode.car,  PlansFilterByLegMode.FilterType.removeAllPlansWithMode) ;
			plansFilter.run(scenario.getPopulation());
			
			try {
				new PlanRouter(scenario);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("Coefficients:" + PTValues.scenarioName);
			
			new PopulationPtAnalyzer(scenario, PTValues.scenarioName + ".txt").run();

		}
	
	}
}


