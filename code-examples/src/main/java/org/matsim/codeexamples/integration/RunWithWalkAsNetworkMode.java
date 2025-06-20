package org.matsim.codeexamples.integration;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.ControllerUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Collection;
import java.util.Set;

public class RunWithWalkAsNetworkMode {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("scenarios/equil/config-with-minimal-plans-file.xml");

        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setLastIteration(1);

        final String walkOnNetwork = "walkOnNetwork";
        config.qsim().setMainModes(Set.of(TransportMode.car, walkOnNetwork ) );
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);
        config.routing().setNetworkModes(Set.of(TransportMode.car, walkOnNetwork));
        config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.walkConstantTimeToLink);
        config.routing().clearTeleportedModeParams();
        config.routing().addTeleportedModeParams(new RoutingConfigGroup.TeleportedModeParams(TransportMode.non_network_walk).setTeleportedModeSpeed(1.4));

        config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(walkOnNetwork).setConstant(0).setMarginalUtilityOfTraveling(0));
        config.scoring().addModeParams(new ScoringConfigGroup.ModeParams(TransportMode.non_network_walk).setConstant(0).setMarginalUtilityOfTraveling(0));

        config.global().setNumberOfThreads(1);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        PopulationFactory factory = scenario.getPopulation().getFactory();
        Person person = factory.createPerson(Id.createPersonId("walk_person"));
        Plan plan = factory.createPlan();

        PopulationUtils.copyFromTo(scenario.getPopulation().getPersons().get(Id.createPersonId("1")).getSelectedPlan(), plan);
        for (Leg leg : TripStructureUtils.getLegs(plan)) {
            leg.setMode(walkOnNetwork);
            leg.setRoute(null);
        }

        Activity activity = (Activity) plan.getPlanElements().get(0);
        activity.setEndTime(6 * 3600 + 10);
        plan.setPerson(person);
        person.addPlan(plan);
        scenario.getPopulation().addPerson(person);

//        scenario.getPopulation().removePerson(Id.createPersonId("1"));
//        scenario.getPopulation().removePerson(Id.createPersonId("10"));

        scenario.getNetwork().getLinks().values().forEach(link -> {
            NetworkUtils.addAllowedMode(link, TransportMode.walk);
            NetworkUtils.setLinkAccessTime(link, TransportMode.walk, 10.0);
            NetworkUtils.setLinkAccessTime(link, TransportMode.car, 10.0);

            NetworkUtils.setLinkEgressTime(link, TransportMode.walk, 10.0);
            NetworkUtils.setLinkEgressTime(link, TransportMode.car, 10.0);
        });

        scenario.getVehicles().addVehicleType(VehicleUtils.createVehicleType(Id.create("walk", VehicleType.class), TransportMode.walk).setMaximumVelocity(2.0));
        scenario.getVehicles().addVehicleType(VehicleUtils.createVehicleType(Id.create("car", VehicleType.class), TransportMode.car).setMaximumVelocity(20.0));

        Collection<VehicleType> values = scenario.getVehicles().getVehicleTypes().values();
        values.forEach(vehicleType -> {
            System.out.println(vehicleType);
        });

        Controller controller = ControllerUtils.createController(scenario);
        controller.run();
    }
}
