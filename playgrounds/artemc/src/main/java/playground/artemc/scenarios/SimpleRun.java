package playground.artemc.scenarios;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import playground.artemc.annealing.AnnealingConfigGroup;
import playground.artemc.heterogeneity.HeterogeneityConfigGroup;

import java.io.File;

/**
 * Created by artemc on 24/06/16.
 */
public class SimpleRun {

	private static String input;
	private static String output;

	public static void main(String[] args) {

		input = args[0];
		output = args[1];

		Config config = ConfigUtils.loadConfig(input+"config.xml", new HeterogeneityConfigGroup(), new RoadPricingConfigGroup(), new AnnealingConfigGroup());

		config.network().setInputFile(input+"network.xml");
		boolean isPopulationZipped = new File(input+"population.xml.gz").isFile();
		if(isPopulationZipped){
			config.plans().setInputFile(input+"population.xml.gz");
		}else{
			config.plans().setInputFile(input+"population.xml");
		}

		boolean isPersonAttributesZipped = new File(input+"personAttributes.xml.gz").isFile();
		if(isPersonAttributesZipped){
			config.plans().setInputPersonAttributeFile(input+"personAttributes.xml.gz");
		}else{
			config.plans().setInputPersonAttributeFile(input+"personAttributes.xml");
		}

		config.transit().setTransitScheduleFile(input+"transitSchedule.xml");
		config.transit().setVehiclesFile(input + "vehicles.xml");

		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setLastIteration(2);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		config.controler().setOutputDirectory(output);
		Controler controler = new Controler(config);
		controler.run();
	}

}
