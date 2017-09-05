package playground.clruch.trb18.scenario.stages;

import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;

import playground.clruch.trb18.scenario.TRBScenarioConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;

public class TRBConfigModifier {
    /**
     * Modifies an input config file with relevant TRB settings
     * - Population is the used full population. It is used to set up all the needed activitiy types in the config file
     */
    public void modify(Config config, Population population, TRBScenarioConfig scenarioConfig) {
        List<StrategyConfigGroup.StrategySettings> mainStrategies = config.strategy().getStrategySettings().stream().filter(s -> s.getSubpopulation() == null).collect(Collectors.toList());
        config.strategy().clearStrategySettings();

        config.plans().setInputFile(scenarioConfig.populationOutputPath);
        config.plans().setInputPersonAttributeFile(scenarioConfig.populationAttributesOutputPath);
        config.network().setInputFile(scenarioConfig.fullNetworkOutputPath);

        PlanCalcScoreConfigGroup.ModeParams avParams = config.planCalcScore().getOrCreateModeParams(AVModule.AV_MODE);
        PlanCalcScoreConfigGroup.ModeParams carParams = config.planCalcScore().getOrCreateModeParams(TransportMode.car);

        avParams.setConstant(carParams.getConstant());
        avParams.setMarginalUtilityOfTraveling(carParams.getMarginalUtilityOfTraveling());
        avParams.setMonetaryDistanceRate(carParams.getMonetaryDistanceRate());

        AVConfigGroup avConfig = new AVConfigGroup();
        avConfig.setConfigPath(scenarioConfig.avConfigPath);
        config.addModule(avConfig);

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        PlanCalcScoreConfigGroup.ActivityParams params = config.planCalcScore().getOrCreateScoringParameters(null).getOrCreateActivityParams(activity.getType());
                        params.setScoringThisActivityAtAll(false);
                    }
                }
            }
        }

        config.controler().setLastIteration(20);

        if (scenarioConfig.allowModeChoice) {
            StrategyConfigGroup.StrategySettings settings = new StrategyConfigGroup.StrategySettings();
            settings.setStrategyName("ChangeExpBeta");
            settings.setWeight(1.0);
            config.strategy().addStrategySettings(settings);
        } else {
            StrategyConfigGroup.StrategySettings settings = new StrategyConfigGroup.StrategySettings();
            settings.setStrategyName("KeepLastSelected");
            settings.setWeight(1.0);
            config.strategy().addStrategySettings(settings);
        }

        StrategyConfigGroup.StrategySettings settings = new StrategyConfigGroup.StrategySettings();
        settings.setStrategyName("KeepLastSelected");
        settings.setSubpopulation("noav");
        settings.setWeight(1.0);
        config.strategy().addStrategySettings(settings);
    }
}
