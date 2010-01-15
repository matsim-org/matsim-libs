package playground.mmoyo.analysis.comp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.ScenarioLoaderFactoryImpl;
import org.matsim.population.algorithms.PlansFilterByLegMode;

import playground.mmoyo.analysis.PTLegIntoPlanConverter;

public class CarPlansRemover {

	public void run(Population population) {
		PlansFilterByLegMode plansFilter = new PlansFilterByLegMode(TransportMode.pt, false);
		plansFilter.run(population) ;
	}

	public static void main(final String[] args) {
		String[] configs;
		if (args.length>0){
			configs = args;
		}else{
			configs= new String[3];
			configs[0]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_rieser.xml";
			configs[1]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_time.xml";
			configs[2]="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/routed_plans/routed_configs/config_900s_small_moyo_parameterized.xml";
		}

		CarPlansRemover carPlansRemover = new CarPlansRemover();
		for (byte i=0; i<configs.length; i++ ){
			ScenarioLoader sl = new ScenarioLoaderFactoryImpl().createScenarioLoader(configs[i]);
			Scenario scenario = sl.loadScenario();
			new PTLegIntoPlanConverter().run((ScenarioImpl) scenario);
			carPlansRemover.run(scenario.getPopulation());

			
			String outputFile = scenario.getConfig().plans().getInputFile() + ".NoCarPlans.xml";
			System.out.println("writing output plan file..." + outputFile);
			PopulationWriter popwriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
			popwriter.write(outputFile) ;
			System.out.println("done");
		}
	}
}