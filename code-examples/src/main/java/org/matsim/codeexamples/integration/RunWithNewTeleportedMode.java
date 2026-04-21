package org.matsim.codeexamples.integration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.scenario.ScenarioUtils;

public class RunWithNewTeleportedMode {
    private static final String MY_BIKE = "myBike";

    static void main() {
        // distance: beelineDistance * beelineDistanceFactor (default 1.3)
        // speed: as configured (3.0)
        // in config:
        // <param name="teleportedModeSpeed" value="3.0" />
        // <param name="beelineDistanceFactor" value="1.3" /> (Set by automagic!)
        {
            RoutingConfigGroup.TeleportedModeParams bike = new RoutingConfigGroup.TeleportedModeParams().setMode(MY_BIKE);
            bike.setTeleportedModeSpeed(3.0);
            run(bike, "teleported");
        }

        // distance: shortest freespeed path on network (no modal filtering)
        // speed: freespeed on the respective links * teleportedModeFreespeedFactor (configured 2.0)
        // in config:
        // <param name="teleportedModeFreespeedFactor" value="2.0" />
        {
            RoutingConfigGroup.TeleportedModeParams bike = new RoutingConfigGroup.TeleportedModeParams().setMode(MY_BIKE);
            bike.setTeleportedModeFreespeedFactor(2.0);
            run(bike, "freespeedFactor");
        }

        // other possibilities:
        // - routed on network (need to put mode on links) + teleported in QSim
        // - routed on network (need to put mode on links) + simulated on network in QSim (mode needs to be in mainModes)
    }

    private static void run(RoutingConfigGroup.TeleportedModeParams bike, String runId) {
        Config config = ConfigUtils.loadConfig("scenarios/equil/config.xml");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        //do at least one replanning
        config.controller().setLastIteration(1);
        config.controller().setRunId(runId);
        config.controller().setOutputDirectory(config.controller().getOutputDirectory() + "/" + runId);

        ReplanningConfigGroup.StrategySettings stratSets = new ReplanningConfigGroup.StrategySettings()
                .setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice)
                .setWeight(1.0);
        config.replanning().addStrategySettings(stratSets);
        config.subtourModeChoice().setModes(new String[]{TransportMode.car, MY_BIKE});

        config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams().setMode(TransportMode.walk).setTeleportedModeSpeed(2.0));
        config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(TransportMode.walk));
        config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(MY_BIKE));


        config.routing().addTeleportedModeParams(bike);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        Leg leg = (Leg) scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan().getPlanElements().get(1);
        leg.setMode(MY_BIKE);
        leg.setRoute(null);
        leg.setRoutingMode(MY_BIKE);

        Controller controller = ControllerUtils.createController(scenario);
        controller.run();
    }
}
