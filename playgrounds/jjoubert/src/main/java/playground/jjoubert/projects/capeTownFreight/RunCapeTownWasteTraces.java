package playground.jjoubert.projects.capeTownFreight;

import java.io.File;
import java.util.Arrays;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.DefaultPlanStrategiesModule;

import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;

public class RunCapeTownWasteTraces {

	public static void main(String[] args) {
		Header.printHeader(RunCapeTownWasteTraces.class.toString(), args);
		String networkFile = args[0];
		String populationFile = args[1];
		String outputFolder = args[2];
		
		/* Clean out the output directory. */
		FileUtils.delete(new File(outputFolder));
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration );
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(outputFolder);

		
		String[] modes ={"waste"};
		config.qsim().setMainModes( Arrays.asList(modes) );
		config.plansCalcRoute().setNetworkModes(Arrays.asList(modes));
		
		/* PlanCalcScore */
		ActivityParams waste = new ActivityParams("waste");
		waste.setTypicalDuration(5*3600);
		config.planCalcScore().addActivityParams(waste);

		/* Generic strategy */
		StrategySettings changeExpBetaStrategySettings = new StrategySettings(ConfigUtils.createAvailableStrategyId(config));
		changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaStrategySettings.setWeight(1.0);
		config.strategy().addStrategySettings(changeExpBetaStrategySettings);

		new Controler(config).run();
		
		Header.printFooter();
	}

}
