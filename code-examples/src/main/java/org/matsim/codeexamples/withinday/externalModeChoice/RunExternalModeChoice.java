package org.matsim.codeexamples.withinday.externalModeChoice;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import java.util.Set;

public class RunExternalModeChoice {
    public static final String REINFORCEMENT_MODE = "rl";

    static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("scenarios/equil/config.xml");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controller().setLastIteration(1);

        // disable on purpose; otherwise the router checks the new "rl" mode, which is only a dummy mode.
        config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

        // add walk scoring parameters, because the default router adds access and egress legs with mode walk.
        config.scoring().addModeParams(new ScoringConfigGroup.ModeParams("walk"));

        //

        // reset replanning method assuming that the RL method does all the replanning within the simulation.
        config.replanning().clearStrategySettings();
        ReplanningConfigGroup.StrategySettings keepLast = new ReplanningConfigGroup.StrategySettings()
                .setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.KeepLastSelected)
                .setWeight(1.0);
        config.replanning().addStrategySettings(keepLast);

        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        VehicleType car = VehicleUtils.createVehicleType(Id.createVehicleTypeId(TransportMode.car)).setNetworkMode(TransportMode.car);
        scenario.getVehicles().addVehicleType(car);

        // Set modes in population. This step can be omitted if you have a custom population from a file or are creating the
        // population on your own in code.
        setLegModesToRL(scenario);

        Controller controller = ControllerUtils.createController(scenario);
        final WithinDayTravelTime travelTime = new WithinDayTravelTime(controller.getScenario(), Set.of(REINFORCEMENT_MODE));
        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                // bing the withinday travel time in order to be able to use it in the mode choice listener
                this.bind(TravelTime.class).toInstance(travelTime);
                this.addEventHandlerBinding().toInstance(travelTime);
                this.addMobsimListenerBinding().toInstance(travelTime);

                // bing the interface for the RL
                this.bind(RLModeChoiceListener.WithinDayModeChoice.class).to(RLModeChoiceListener.CarModeChoice.class);
                // add the RL mode choice listener to the mobsim listeners
                this.addMobsimListenerBinding().to(RLModeChoiceListener.class);
            }
        });
        controller.run();
    }

    private static void setLegModesToRL(Scenario scenario) {
        scenario.getPopulation().getPersons().values().stream().flatMap(p -> p.getPlans().stream()).forEach(p -> {
            for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(p)) {
                for (Leg leg : trip.getLegsOnly()) {
                    // set the mode to custom reinforcement mode. The module will later on check for this mode and call the reinforcement module.
                    leg.setRoutingMode(REINFORCEMENT_MODE);
                    leg.setMode(REINFORCEMENT_MODE);
                }
            }
        });
    }
}
