package org.matsim.contrib.ev.extensions.battery_chargers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

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
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.EvUtils;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.charging.VehicleChargingHandler;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class TestScenarioBuilder {
    private final MatsimTestUtils utils;
    private final double vehicleBatteryCapacity_kWh;
    private final double simulationEndTime;

    public TestScenarioBuilder(MatsimTestUtils utils, double vehicleBatteryCapacity_kWh, double simulationEndTime) {
        this.utils = utils;
        this.vehicleBatteryCapacity_kWh = vehicleBatteryCapacity_kWh;
        this.simulationEndTime = simulationEndTime;
    }

    private record ChargerRecord(String id, int plugs, double plugPower_kW, Attributes attributes) {
    }

    private final List<ChargerRecord> chargerRecords = new LinkedList<>();

    public TestScenarioBuilder addCharger(String id, int plugs, double plugPower_kW,
            Consumer<Attributes> configurator) {
        Attributes attributes = new AttributesImpl();
        configurator.accept(attributes);

        chargerRecords.add(new ChargerRecord(id, plugs, plugPower_kW, attributes));
        return this;
    }

    private record VehicleRecord(String id, double chargingStartTime, double chargingEndTime, double initialSoc) {
    }

    private final List<VehicleRecord> vehicleRecords = new LinkedList<>();

    public TestScenarioBuilder addVehicle(String id, double chargingStartTime, double chargingEndTime,
            double initialSoc) {
        vehicleRecords.add(new VehicleRecord(id, chargingStartTime, chargingEndTime, initialSoc));
        return this;
    }

    public Controller build() {
        Config config = ConfigUtils.createConfig(new EvConfigGroup());

        ActivityParams genericParams = new ActivityParams("generic");
        genericParams.setScoringThisActivityAtAll(false);
        config.scoring().addActivityParams(genericParams);

        ActivityParams chargingParams = new ActivityParams("car charging interaction");
        chargingParams.setScoringThisActivityAtAll(false);
        config.scoring().addActivityParams(chargingParams);

        config.qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);
        config.controller().setOutputDirectory(utils.getOutputDirectory());

        config.controller().setLastIteration(0);

        config.qsim().setStartTime(0.0);
        config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

        config.qsim().setEndTime(simulationEndTime);
        config.qsim().setSimEndtimeInterpretation(EndtimeInterpretation.onlyUseEndtime);

        Scenario scenario = ScenarioUtils.createScenario(config);

        Network network = scenario.getNetwork();
        NetworkFactory networkFactory = network.getFactory();

        Node node = networkFactory.createNode(Id.createNodeId("node"), new Coord(0.0, 0.0));
        network.addNode(node);

        Link link = networkFactory.createLink(Id.createLinkId("link"), node, node);
        network.addLink(link);

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory vehiclesFactory = vehicles.getFactory();

        VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.createVehicleTypeId("electric"));
        vehicles.addVehicleType(vehicleType);

        ElectricFleetUtils.setElectricVehicleType(vehicleType);
        ElectricFleetUtils.setChargerTypes(vehicleType, Collections.singleton("default"));
        VehicleUtils.setEnergyCapacity(vehicleType.getEngineInformation(), vehicleBatteryCapacity_kWh);

        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        for (VehicleRecord record : vehicleRecords) {
            Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId(record.id), vehicleType);
            ElectricFleetUtils.setInitialSoc(vehicle, record.initialSoc);
            vehicles.addVehicle(vehicle);

            Person person = populationFactory.createPerson(Id.createPersonId(record.id));
            VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, Collections.singletonMap("car", vehicle.getId()));
            population.addPerson(person);

            Plan plan = populationFactory.createPlan();
            person.addPlan(plan);

            Activity start = populationFactory.createActivityFromLinkId("generic", link.getId());
            start.setEndTime(record.chargingStartTime);
            plan.addActivity(start);

            Leg first = populationFactory.createLeg("car");
            first.setRoute(RouteUtils.createLinkNetworkRouteImpl(link.getId(), link.getId()));
            TripStructureUtils.setRoutingMode(first, "car");
            plan.addLeg(first);

            Activity charging = populationFactory
                    .createActivityFromLinkId("car" + VehicleChargingHandler.CHARGING_INTERACTION, link.getId());
            charging.setEndTime(record.chargingEndTime);
            plan.addActivity(charging);

            Leg second = populationFactory.createLeg("car");
            second.setRoute(RouteUtils.createLinkNetworkRouteImpl(link.getId(), link.getId()));
            TripStructureUtils.setRoutingMode(second, "car");
            plan.addLeg(second);

            Activity end = populationFactory.createActivityFromLinkId("generic", link.getId());
            plan.addActivity(end);
        }

        ChargingInfrastructureSpecificationDefaultImpl infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

        for (ChargerRecord record : chargerRecords) {
            infrastructure.addChargerSpecification(ImmutableChargerSpecification.newBuilder() //
                    .id(Id.create(record.id, Charger.class)) //
                    .linkId(link.getId()) //
                    .plugCount(record.plugs) //
                    .plugPower(EvUnits.kW_to_W(record.plugPower_kW)) //
                    .chargerType("default") //
                    .attributes(record.attributes) //
                    .build());
        }

        Controler controller = new Controler(scenario);
        controller.addOverridingModule(new EvModule());
        EvUtils.registerInfrastructure(controller, infrastructure);

        SocListener listener = new SocListener(utils.getOutputDirectory() + "/soc.csv");

        controller.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addControllerListenerBinding().toInstance(listener);
                addEventHandlerBinding().toInstance(listener);
            }
        });

        return controller;
    }

    private class SocListener implements IterationStartsListener, IterationEndsListener, EnergyChargedEventHandler {
        private final String outputPath;

        public SocListener(String outputPath) {
            this.outputPath = outputPath;
        }

        private BufferedWriter writer;

        @Override
        public void notifyIterationStarts(IterationStartsEvent event) {
            try {
                writer = IOUtils.getBufferedWriter(outputPath);
                writer.write(String.join(";", new String[] {
                        "time", "vehicle_id", "charge_kWh"
                }) + "\n");
            } catch (IOException e) {
            }
        }

        @Override
        public void handleEvent(EnergyChargedEvent event) {
            try {
                writer.write(String.join(";", new String[] {
                        String.valueOf(event.getTime()),
                        event.getVehicleId().toString(),
                        String.valueOf(EvUnits.J_to_kWh(event.getEndCharge()))
                }) + "\n");
            } catch (IOException e) {
            }
        }

        @Override
        public void notifyIterationEnds(IterationEndsEvent event) {
            try {
                writer.close();
            } catch (IOException e) {
            }
        }
    }
}
