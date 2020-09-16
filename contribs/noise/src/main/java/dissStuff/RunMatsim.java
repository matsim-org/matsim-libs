package dissStuff;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

public class RunMatsim {
    public static void main(String[] args) {
        Config config = ConfigUtils.createConfig();
        config.controler().setLastIteration(10);
        config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
        config.network().setInputFile("network.xml");
        config.plans().setInputFile("population.xml");

        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(16 * 60 * 60);
        config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(work);

        // define strategies:
        {
            StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
            strat.setWeight(0.15);
            config.strategy().addStrategySettings(strat);
        }
        {
            StrategyConfigGroup.StrategySettings strat = new StrategyConfigGroup.StrategySettings();
            strat.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
            strat.setWeight(0.9);

            config.strategy().addStrategySettings(strat);
        }
        config.strategy().setFractionOfIterationsToDisableInnovation(0.9);

        config.vspExperimental().setWritingOutputEvents(true);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;


        Controler controler = new Controler( scenario ) ;
        controler.run();

    }









}
