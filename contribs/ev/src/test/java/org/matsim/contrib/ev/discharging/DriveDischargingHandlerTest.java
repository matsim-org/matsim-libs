package org.matsim.contrib.ev.discharging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.EventHandler;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.AuxEnergyConsumption;
import org.matsim.contrib.ev.EvConfigGroup.DriveEnergyConsumption;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.EvUtils;
import org.matsim.contrib.ev.example.RunEvExample;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author sebhoerl
 */
public class DriveDischargingHandlerTest {
    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testEnterAndLeaveNetworkInSameSecond() {
        int retries = 1; // switch to a high number (1000) as this is a race condition issue

        for (int k = 0; k < retries; k++) {
            // CONFIG
            Config config = ConfigUtils.createConfig();

            config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
            config.controller().setOutputDirectory(utils.getOutputDirectory());
            config.controller().setLastIteration(0);

            // needed for scoring
            config.scoring().addActivityParams(new ActivityParams("generic").setScoringThisActivityAtAll(false));

            // we want to use a specific ev
            config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

            // don't prematurely end simulation
            config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);
            config.qsim().setEndTime(5.0);

            // prepare ev in config
            EvConfigGroup evConfig = new EvConfigGroup();
            config.addModule(evConfig);

            // SCENARIO
            Scenario scenario = ScenarioUtils.createScenario(config);

            Population population = scenario.getPopulation();
            PopulationFactory populationFactory = population.getFactory();

            Network network = scenario.getNetwork();
            NetworkFactory networkFactory = network.getFactory();

            Vehicles vehicles = scenario.getVehicles();
            VehiclesFactory vehiclesFactory = vehicles.getFactory();

            // VEHICLE TYPE
            VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.createVehicleTypeId("electric"));
            vehicleType.setNetworkMode("car");
            VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(),
                    ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);
            VehicleUtils.setEnergyCapacity(vehicleType.getEngineInformation(), 50.0);
            vehicles.addVehicleType(vehicleType);

            // VEHICLE
            Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId("vehicle"), vehicleType);
            ElectricFleetUtils.setInitialSoc(vehicle, 1.0);
            vehicles.addVehicle(vehicle);

            // NETWORK

            // a single node
            Node node = networkFactory.createNode(Id.createNodeId("node"), new Coord(0.0, 0.0));
            network.addNode(node);

            // connected to itself by a single link (distance zero)
            Link link = networkFactory.createLink(Id.createLinkId("link"), node, node);
            network.addLink(link);

            // PERSON
            Person person = populationFactory.createPerson(Id.createPersonId("person"));
            population.addPerson(person);

            Plan plan = populationFactory.createPlan();
            person.addPlan(plan);

            // set vehicle
            PopulationUtils.insertVehicleIdsIntoPersonAttributes(person,
                    Collections.singletonMap("car", vehicle.getId()));

            // first activity ends at 0.0 at the only link
            Activity firstActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("link"));
            firstActivity.setEndTime(0.0);
            plan.addActivity(firstActivity);

            // first car leg
            Leg firstLeg = populationFactory.createLeg("car");
            plan.addLeg(firstLeg);

            // second activity ends asap at the link (zero duration drive between)
            Activity secondActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("link"));
            plan.addActivity(secondActivity);
            secondActivity.setEndTime(0.0);

            Leg secondLeg = populationFactory.createLeg("car");
            plan.addLeg(secondLeg);

            // third activity ends asap at the link (zero duration drive between)
            Activity thirdActivity = populationFactory.createActivityFromLinkId("generic", Id.createLinkId("link"));
            plan.addActivity(thirdActivity);

            // CONTROLLER
            Controler controller = new Controler(scenario);

            // active evs
            controller.addOverridingModule(new EvModule());

            // empy infrastructure
            EvUtils.registerInfrastructure(controller, new ChargingInfrastructureSpecificationDefaultImpl());

            controller.run();

            /*
             * What happens in this simulation:
             * - agent starts at 0.0 (VehicleEntersNetwork)
             * - agent arrives at 0.0 (VehicleLeavesNetwork)
             * - agent starts at 1.0 (VehicleEntersNetwork)
             * - agent arrives at 1.0 (VehicleLeavesNetwork)
             * 
             * This leads to a NPE in the current version of DriveDischargingHandler:
             * 
             * In second 0.0:
             * 
             * - VehicleEntersNetwork is processed at 0.0 -> this creates an energy tracking
             * object
             * 
             * - VehicleLeavesNetwork is tracked at 0.0 to be processed later
             * 
             * In second 1.0:
             * 
             * - VehicleEntersNetwork is processed at 1.0 -> this creates an energy tracking
             * object (but there is already one, it is overwritten - FIRST ISSUE)
             * 
             * - VehicleLeavesNetwork from second 0.0 is processed -> this first works with
             * the energy tracking object, then removes it
             * 
             * NOTE: The order here is important. The event handler that processes the
             * VehicleEntersNetwork event runs in parallel with the onSimStep. If the order
             * is inversed, everything will work without a problem.
             * 
             * In second 2.0:
             * 
             * - VehicleLeavesNetwork from second 1.0 is processed -> this tries to access
             * the energy tracking object, but it has already been deleted! This gives an
             * NPE.
             * 
             */
        }
    }

    @Test
	public void ohdeSlaskiTest() {
        String[] args = {
            RunEvExample.DEFAULT_CONFIG_FILE,
            "--config:controler.outputDirectory", utils.getOutputDirectory()
        };

        SummedEnergy energy = new SummedEnergy();

        new RunEvExample().run(args, config -> {}, scenario -> {}, controller -> {
            energy.install(controller);
        });

        assertEquals(925.38917, energy.energy_kWh, 1e-3);
	}

    @Test
	public void attributeBasedTest() {
        String[] args = {
            RunEvExample.DEFAULT_CONFIG_FILE,
            "--config:controler.outputDirectory", utils.getOutputDirectory()
        };

        SummedEnergy energy = new SummedEnergy();

        new RunEvExample().run(args, config -> {
            EvConfigGroup.get(config).setDriveEnergyConsumption(DriveEnergyConsumption.AttributeBased);
        }, scenario -> {
            for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
                AttributeBasedDriveEnergyConsumption.assign(vehicle, 120.0);
            }
        }, controller -> {
            energy.install(controller);
        });

        assertEquals(627.923475, energy.energy_kWh, 1e-3);
	}

    static public class SummedEnergy implements DrivingEnergyConsumptionEventHandler {
        public double energy_kWh = 0.0;
        
        @Override
        public void handleEvent(DrivingEnergyConsumptionEvent event) {
            energy_kWh += EvUnits.J_to_kWh(event.getEnergy());
        }

        public void install(Controler controller) {
            SummedEnergy self = this;

            controller.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addEventHandlerBinding().toInstance(self);
                }
            });
        }
    }
}
