package playground.artemc.psim;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.heterogeneity.HeterogeneityConfigGroup;

/**
 * Created by artemc on 25/4/15.
 */
public class ScenarioInitializerFromOutput {



	public static Config initScenario(String input, String output) {

		Config config = ConfigUtils.loadConfig(input + "output_config.xml", new HeterogeneityConfigGroup(), new RoadPricingConfigGroup());

		config.network().setInputFile(input+"output_network.xml.gz");
		config.plans().setInputFile(input + "output_plans.xml.gz");
		config.plans().setInputPersonAttributeFile(input + "output_personAttributes.xml.gz");
		config.transit().setTransitScheduleFile(input + "output_transitSchedule.xml.gz");
		config.transit().setVehiclesFile(input + "output_transitVehicles.xml.gz");

		config.controler().setOutputDirectory(output);

		//Roadpricing module config
		if(config.getModules().containsKey(RoadPricingConfigGroup.GROUP_NAME)){
			ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(input + "output_toll.xml.gz");
		}

		//Heterogeneity module config
		if(config.getModules().containsKey(RoadPricingConfigGroup.GROUP_NAME)){
			ConfigUtils.addOrGetModule(config,HeterogeneityConfigGroup.GROUP_NAME,HeterogeneityConfigGroup.class).setIncomeFile(input + "output_personAttributes.xml.gz");
		}


		return config;
	}
}
