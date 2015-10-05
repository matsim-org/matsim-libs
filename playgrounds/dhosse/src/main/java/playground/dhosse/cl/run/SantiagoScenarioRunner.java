package playground.dhosse.cl.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;


public class SantiagoScenarioRunner {

	private static String inputPath = "../../../runs-svn/santiago/run11c/input/";
//	private static String inputPath = "../../../runs-svn/santiago/run9/input/";
	private static boolean doModeChoice = false;
	
	public static void main(String args[]){
		
//		OTFVis.convert(new String[]{
//						"",
//						outputPath + "modeChoice.output_events.xml.gz",	//events
//						outputPath + "modeChoice.output_network.xml.gz",	//network
//						outputPath + "visualisation.mvi", 		//mvi
//						"60" 									//snapshot period
//		});
//		
//		OTFVis.playMVI(outputPath + "visualisation.mvi");
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, inputPath + "config_final.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		Controler controler = new Controler(scenario);
		
		// adding pt fare in a simplified way
		controler.getEvents().addHandler(new PTFlatFareHandler(controler));
		
		// adding subtour mode choice strategy parameters for car and non-car users
		if(doModeChoice){
			setModeChoiceForSubpopulations(controler);
		}
		controler.run();
	}

	private static void setModeChoiceForSubpopulations(final Controler controler) {
		StrategySettings carAvail = new StrategySettings();
		carAvail.setStrategyName("SubtourModeChoice_".concat("carAvail"));
		carAvail.setSubpopulation("carAvail");
		carAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(carAvail);
		
		StrategySettings nonCarAvail = new StrategySettings();
		nonCarAvail.setStrategyName("SubtourModeChoice_".concat("nonCarAvail"));
		// TODO: check if this really refers to the people without car
		nonCarAvail.setSubpopulation(null);
		nonCarAvail.setWeight(0.1);
		controler.getConfig().strategy().addStrategySettings(nonCarAvail);
		
		// adding subtour mode choice strategy module for car and non-car users
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat("carAvail")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {"car", "bike", "bus", "metro", "walk"};
					String[] chainBasedModes = {"car", "bike"};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						// TODO: check what considerCarAvailable does
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding("SubtourModeChoice_".concat("nonCarAvail")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {"bike", "bus", "metro", "walk"};
					String[] chainBasedModes = {"bike"};

					@Override
					public PlanStrategy get() {
						// TODO: check what considerCarAvailable does
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
						builder.addStrategyModule(new ReRoute(controler.getScenario()));
						return builder.build();
					}
				});
			}
		});
		
	}
}