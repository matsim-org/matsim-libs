package org.matsim.contrib.ev.strategic;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.EvConfigGroup.DriveEnergyConsumption;
import org.matsim.contrib.ev.EvUtils;
import org.matsim.contrib.ev.discharging.AttributeBasedDriveEnergyConsumption;
import org.matsim.contrib.ev.extensions.placement.ChargerPlacement;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerReader;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingConfigGroup.SelectionStrategy;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator.Settings.PublicChargerSettings;
import org.matsim.contrib.ev.strategic.analysis.ChargerTypeAnalysisListener;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostCalculator;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters.TariffParameters;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters.ConstraintErrorMode;
import org.matsim.contrib.ev.strategic.replanning.innovator.ChargingInnovationParameters.ConstraintFallbackBehavior;
import org.matsim.contrib.ev.strategic.replanning.innovator.RandomChargingPlanInnovator;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoringParameters.ChargerTypeParams;
import org.matsim.contrib.ev.withinday.WithinDayEvConfigGroup;
import org.matsim.contrib.ev.withinday.WithinDayEvUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/**
 * This class shows how to set up a MATSim scenario for simulation with the
 * Strategic Electric Vehicle Charging (SEVC) extension.
 * 
 * See below for a list of options in the Settings object that needs to be
 * passed to the configurator.
 * 
 * The script is intended to be called directly after loading your scenario
 * on-the-fly in the code, but you can also write out the files via the command
 * line using the main() method of this class.
 * 
 * After all, it is meant to be an example and as a starting point for your own
 * configuration scripts.
 * 
 * The example configurator will take any baseline scenario and do the following
 * steps:
 * 
 * Persons:
 * - Find eligible persons (using the car at least once, having a home acitvity)
 * - Configure those persons to make use of SEVC
 * - Assign a minimum desired SoC during the day and for the end of the day
 * 
 * Vehicles:
 * - Register a new vehicle type (electric)
 * - Create one vehicle for each selected person and assign it as the car mode
 * vehicle
 * - Update the plans to take into account this vehicle instead of the initial
 * one
 * - Assign an initial SoC for each vehicle
 * - Set a maximum SoC for each vehicle up to which the persons would like to
 * charge it
 * 
 * Chargers:
 * - Create a home charger for a specified percentage of the population
 * - Create work chargers dependent on the nubmer of employees performing "work"
 * activities at every facility
 * - Create a specified number of public chargers randomly in the network
 * 
 * Tariffs:
 * - Create a tariff for each of home, work, and public chargers
 * 
 * Subscriptions:
 * - Optionally, the script will tag certain public chargers as
 * subscription-based.
 * - They will receive another tariff for agents that have a subscription
 * - A configurable share of agents receive the subscription
 * 
 * What remains for you is to set up the configuration:
 * StrategicChargingUtils.configure(config)
 * StrategicChargingUtils.configureStandaloneReplanning(config)
 * 
 * And set up the controller:
 * StrategicChargingUtils.configureController(controller)
 */
public class StrategicChargingScenarioConfigurator {
    private static final Logger logger = LogManager.getLogger(StrategicChargingScenarioConfigurator.class);

    static public class Settings {
        // a random seed to generate varying scenarios
        public int seed = 1000;

        public class PersonSettings {
            // allows to generate data only for specific subpopulation (all if empty)
            public Set<String> subpopulations = new HashSet<>();

            // the rate of persons that have an electric car among the eligible ones
            public double ownershipRate = 0.033;

            // desired minimum soc (uniform across all users)
            public double minimumSoc = 0.2;

            // desired minimum soc at the end of the day (uniform across all users)
            public double minimumEndOfDaySoc = 0.2;

            // file containing zones with a ownership_rate attribute that indicates a
            // spatialized ownership rate
            public String ownershipRateFile = null;

            // determines whether to remove non-ev users
            public boolean onlyKeepOwners = false;
        }

        public PersonSettings persons = new PersonSettings();

        public class VehicleSettings {
            // battery capacity of the electric vehicles (uniform across all evs)
            public double batteryCapacity_kWh = 55.0;

            // energy consumption of the electric vehicles (unifirm across all evs)
            public double consumption_Wh_km = 130.0;

            // initial soc at the beginning of the day (lower bound, uniformly sampled)
            public double minimumInitialSoc = 0.25;

            // initial soc at the beginning of the day (upper bound, uniformly sampled)
            public double maximumInitialSoc = 0.8;

            // maximum soc until which a vehicle is charged (uniform across all evs)
            public double maximumChargingSoc = 0.8;

            // whether to use existing electric vehicles for the persons or not
            public boolean considerExistingElectricVehicles = false;
        }

        public VehicleSettings vehicles = new VehicleSettings();

        public class HomeChargerSettings {
            // percentage of ev users that have a charger at home
            public double ownershipRate = 0.25;

            // charging power of home chargers (uniform overall home chargers)
            public double plugPower_kW = 11.0;

            // cost per energy consumption of a home charger
            public double costPerEnergy_EUR_kWh = 0.38;

            // bonus that is given for home chargers in scoring
            public double scoringBonus = 2.0;

            // file containing zones with a ownership_rate attribute that indicates how many
            // of ev users in each zone get a home charger
            public String ownershipRateFile = null;
        }

        public HomeChargerSettings homeChargers = new HomeChargerSettings();

        public class WorkChargerSettings {
            // probability of a person to work at a work place that provides chargers
            public double workChargerRate = 0.35;

            // average number of plugs generated per number of employees at a facility
            public double plugsPerEmployee = 0.1;

            // power of work chargers
            public double plugPower_kW = 33.0;

            // cost per energy consumption of a work charger (if not for free)
            public double costPerEnergy_EUR_kWh = 0.24;

            // percentage of persons that have a charger at work that pay nothing
            public double withoutCostRate = 0.5;
        }

        public WorkChargerSettings workChargers = new WorkChargerSettings();

        public class PublicChargerSettings {
            // number of public chargers to be created randomly in the network
            public int count;

            // number of plugs per created charger
            public int plugs = 1;

            // power of public chargers (uniform overall public chargers)
            public double power_kW;

            // cost per hour charged when using a public charger
            public double costPerDuration_EUR_h = 0.0;

            // cost per energy consumption of a public charger
            public double costPerEnergy_EUR_kWh;

            // duration after which the blocking fee is charged
            public double blockingDuration_min;

            // blocking fee
            public double blockingFee_EUR_min = 0.15;

            public PublicChargerSettings() {
            }

            PublicChargerSettings(int count, double power_kW, double costPerEnergy_EUR_kWh,
                    double blockingDuration_min) {
                this.count = count;
                this.power_kW = power_kW;
                this.costPerEnergy_EUR_kWh = costPerEnergy_EUR_kWh;
                this.blockingDuration_min = blockingDuration_min;
            }
        }

        // default counts approximately for Berlin (10%)
        public PublicChargerSettings slowPublicChargers = new PublicChargerSettings(300, 30.0, 0.5, 120.0);
        public PublicChargerSettings fastPublicChargers = new PublicChargerSettings(55, 200.0, 0.7, 45.0);

        // a file defining additional public chargeres (point geometries, with
        // "charger_type" column [slow/fast] and optional "power_kW" and "plugs")
        public String publicChargersFile = null;

        public boolean considerExistingChargingInfrastructure = false;

        public class SubscriptionSettings {
            // number of ev users holding a special tariff subscription for public chargers
            public double subscriptionRate = 0.0;

            // number of public chargers being eligible for special tariff charging
            public double availabilityRate = 1.0;

            // cost per hour for the special charging tariff
            public double specialTariffDelta_EUR_kWh = -0.1; // 10 ct cheaper
        }

        public SubscriptionSettings subscriptions = new SubscriptionSettings();
    }

    private final Settings settings;

    public StrategicChargingScenarioConfigurator(Settings settings) {
        this.settings = settings;
    }

    /**
     * This method selects persons for electric charging.
     * 
     * - We randomly select persons (that use the car once during the day)
     * - We enable them for electric charging according to the ownershipRate
     * - We configure them so that they are used by SEVC
     */
    public void configurePersons(Scenario scenario) {
        Random random = new Random(settings.seed + 12512412);
        String carMode = getChargingMode(scenario);

        Population population = scenario.getPopulation();
        List<Person> persons = findRelevantPersons(population, carMode);

        // load zonal vehicle ownership rates
        IdMap<Person, Double> zonalRates = new IdMap<>(Person.class);

        if (settings.persons.ownershipRateFile != null) {
            zonalRates = getZonalVehicleOwnershipRates(scenario);
        }

        int numberOfPersons = 0;
        for (Person person : persons) {
            // fall back to global rate
            double personRate = zonalRates.getOrDefault(person.getId(), settings.persons.ownershipRate);

            if (random.nextDouble() < personRate) { // only select a few
                // activate for dynamic charging
                WithinDayEvUtils.activate(person);

                // set the minimum SoC at any time and the end of the day that are used in
                // scoring per person
                StrategicChargingUtils.setMinimumSoc(person, settings.persons.minimumSoc);
                StrategicChargingUtils.setMinimumEndSoc(person, settings.persons.minimumEndOfDaySoc);

                numberOfPersons++;
            }
        }

        logger.info(String.format("Persons: activated %d persons for strategic charging", numberOfPersons));

        if (settings.persons.onlyKeepOwners) {
            Set<Id<Person>> remove = new HashSet<>();

            for (Person person : population.getPersons().values()) {
                if (!WithinDayEvUtils.isActive(person)) {
                    remove.add(person.getId());
                }
            }

            remove.forEach(population::removePerson);
        }
    }

    /**
     * Adds electric vehicles for the selected persons. They are identified by
     * checking whether WEVC is active for them.
     * 
     * - We assume here that there is no electric vehicle type in the scenario
     * - We generate a new electric vehicle type
     * - We assign a new electric vehicle to every agent and make it their car mode
     * 
     * If considerExistingElectricVehicles is set to true in the settings, existing
     * electric vehicles will be kept.
     */
    public void configureVehicles(Scenario scenario) {
        Random random = new Random(settings.seed + 95857126);

        String carMode = getChargingMode(scenario);

        Vehicles vehicles = scenario.getVehicles();
        VehiclesFactory vehiclesFactory = vehicles.getFactory();

        VehicleType sevcVehicleType = null;

        int numberOfGeneratedVehicles = 0;
        int numberOfRetainedVehicles = 0;

        // now create a vehicle per person if they don't already use an electric vehicle
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                boolean usesElectricVehicle = false;

                Id<Vehicle> vehicleId = VehicleUtils.getVehicleIds(person).get(carMode);
                if (vehicleId != null) {
                    Vehicle vehicle = vehicles.getVehicles().get(vehicleId);
                    EngineInformation engineInformation = vehicle.getType().getEngineInformation();

                    if (engineInformation != null) {
                        usesElectricVehicle = ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY
                                .equals(VehicleUtils.getHbefaTechnology(engineInformation));
                    }
                }

                if (!usesElectricVehicle || settings.vehicles.considerExistingElectricVehicles) {
                    if (sevcVehicleType == null) {
                        // create new vehicle type
                        sevcVehicleType = createVehicleType(vehicles, carMode);
                    }

                    // create vehicle
                    Vehicle vehicle = vehiclesFactory.createVehicle(
                            Id.createVehicleId("sevc:" + person.getId().toString()),
                            sevcVehicleType);
                    vehicles.addVehicle(vehicle);

                    // set consumption
                    AttributeBasedDriveEnergyConsumption.assign(vehicle, settings.vehicles.consumption_Wh_km);

                    // set the initial SoC of the electric vehicle
                    double initialSoc = settings.vehicles.minimumInitialSoc;
                    initialSoc += random.nextDouble()
                            * (settings.vehicles.maximumInitialSoc - settings.vehicles.minimumInitialSoc);

                    ElectricFleetUtils.setInitialSoc(vehicle, initialSoc);

                    // set the maximum SoC up to which the person will charge (this is optional, 1.0
                    // is assumed)
                    StrategicChargingUtils.setMaximumSoc(vehicle, settings.vehicles.maximumChargingSoc);

                    // make sure this vehicle is used by this person for the transport mode car mode
                    setVehicleId(person, vehicle.getId(), carMode);

                    // cleaning up the plans
                    updateVehicleInPlans(person, vehicle.getId(), carMode);

                    numberOfGeneratedVehicles++;
                } else {
                    numberOfRetainedVehicles++;
                }
            }
        }

        logger.info(String.format("Vehicles: generated %d vehicles", numberOfGeneratedVehicles));
        logger.info(String.format("Vehicles: retained %d existing vehicles", numberOfRetainedVehicles));
        logger.info(String.format("Vehicles: created new vehicle type: %s", sevcVehicleType == null ? "no" : "yes"));
    }

    private VehicleType createVehicleType(Vehicles vehicles, String carMode) {
        // create vehicle type
        VehicleType vehicleType = vehicles.getFactory()
                .createVehicleType(Id.create("sevc:electric", VehicleType.class));
        vehicles.addVehicleType(vehicleType);

        vehicleType.setNetworkMode(carMode); // these are cars

        // make the vehicle type electric
        VehicleUtils.setHbefaTechnology(vehicleType.getEngineInformation(),
                ElectricFleetUtils.EV_ENGINE_HBEFA_TECHNOLOGY);

        // set the energy capacity in kWh
        VehicleUtils.setEnergyCapacity(vehicleType.getEngineInformation(), settings.vehicles.batteryCapacity_kWh);

        return vehicleType;
    }

    private ChargingInfrastructureSpecificationDefaultImpl infrastructure = new ChargingInfrastructureSpecificationDefaultImpl();

    /**
     * Integrates the already existing charging infrastructure into the new
     * infrastructure that will be generated.
     */
    public void loadExistingChargingInfrastructure(Scenario scenario) {
        EvConfigGroup evConfig = EvConfigGroup.get(scenario.getConfig());

        if (evConfig.getChargersFile() != null && settings.considerExistingChargingInfrastructure) {
            new ChargerReader(infrastructure).readURL(
                    ConfigGroup.getInputFileURL(scenario.getConfig().getContext(), evConfig.getChargersFile()));
        }

        logger.info(
                String.format("Chargers: kept existing chargers: %s",
                        evConfig.getChargersFile() == null ? "no" : "yes"));
    }

    /**
     * Generate home chargers.
     * 
     * - We give home chargers to persons based on the homeChargerRate
     */
    public void configureHomeChargers(Scenario scenario) {
        Random random = new Random(settings.seed + 8476162);
        int numberOfChargers = 0;

        // load zonal ownership rates
        IdMap<Person, Double> zonalRates = new IdMap<>(Person.class);

        if (settings.homeChargers.ownershipRateFile != null) {
            zonalRates = getZonalHomeChargerRates(scenario);
        }

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                double ownershipRate = zonalRates.getOrDefault(person.getId(), settings.homeChargers.ownershipRate);

                if (random.nextDouble() < ownershipRate) { // person gets a home charger
                    // describe the charger
                    ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                            .id(Id.create("sevc:home:" + person.getId().toString(), Charger.class)) // ,
                            .linkId(getHomeLinkId(person, false)) //
                            .chargerType("home") // only for analysis, no logical meaning
                            .plugPower(settings.homeChargers.plugPower_kW * 1e3) //
                            .plugCount(1) //
                            .build();

                    // reserve this charger for the person
                    StrategicChargingUtils.assignChargerPersons(charger, Collections.singleton(person.getId()));

                    // add it to the infrastructure
                    infrastructure.addChargerSpecification(charger);

                    // for aggregated analysis
                    ChargerTypeAnalysisListener.addAnalysisType(charger, "home");

                    numberOfChargers++;
                }
            }
        }

        logger.info(
                String.format("Chargers: created %d home chargers", numberOfChargers));
    }

    /**
     * Generate work chargers.
     * 
     * - we count the number of persons working at each facility
     * - we select all facilities that have more than workMinimumEmployees
     * - we assign to each workplace plugsPerEmployee plugs
     */
    public void configureWorkChargers(Scenario scenario) {
        Random random = new Random(settings.seed + 916552);

        String carMode = getChargingMode(scenario);

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(carMode));

        // track employees
        IdMap<ActivityFacility, Set<Id<Person>>> employees = trackEmployees(scenario.getPopulation(),
                scenario.getActivityFacilities());

        // expected employees with charger at work place
        int expectedCount = (int) (employees.values().stream().mapToInt(o -> o.size()).sum()
                * settings.workChargers.workChargerRate);

        // sample work places until we get to the expected count
        List<Id<ActivityFacility>> candidates = new LinkedList<>(employees.keySet());
        Collections.sort(candidates);

        List<Id<ActivityFacility>> selectedWorkplaces = new LinkedList<>();
        Set<Id<Person>> assigned = new HashSet<>();

        while (candidates.size() > 0 && assigned.size() <= expectedCount) {
            int index = random.nextInt(candidates.size());
            Id<ActivityFacility> workplaceId = candidates.remove(index);

            assigned.addAll(employees.get(workplaceId));
            selectedWorkplaces.add(workplaceId);
        }

        // now generate chargers for each work place
        ActivityFacilities facilities = scenario.getActivityFacilities();

        int theoreticalNumberOfChargers = 0;
        int numberOfChargers = 0;
        int maximumNumberOfPlugs = 0;

        for (Id<ActivityFacility> workplaceId : selectedWorkplaces) {
            int employeeCount = employees.get(workplaceId).size();

            ActivityFacility facility = facilities.getFacilities().get(workplaceId);

            double requestedPlugs = employeeCount * settings.workChargers.plugsPerEmployee;

            int plugs = (int) Math.floor(requestedPlugs);
            requestedPlugs -= plugs;

            if (random.nextDouble() < requestedPlugs) {
                plugs++; // one more for stochastic part
            }

            if (plugs > 0) {
                // find the link id
                Id<Link> linkId = facility.getLinkId();

                if (linkId == null) {
                    linkId = NetworkUtils.getNearestLink(roadNetwork, facility.getCoord()).getId();
                }

                // describe the charger
                ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                        .id(Id.create("sevc:work:" + facility.getId().toString(), Charger.class)) // ,
                        .linkId(linkId) //
                        .chargerType("work") // only for analysis, no logical meaning
                        .plugPower(settings.workChargers.plugPower_kW * 1e3) //
                        .plugCount(plugs) // dependent on employee count
                        .build();

                // reserve this charger for activities at the specific facility
                StrategicChargingUtils.assignChargerFacilities(charger, Collections.singleton(facility.getId()));

                // add it to the infrastructure
                infrastructure.addChargerSpecification(charger);

                // for aggregated analysis
                ChargerTypeAnalysisListener.addAnalysisType(charger, "work");

                numberOfChargers++;
                maximumNumberOfPlugs = Math.max(maximumNumberOfPlugs, plugs);
            }

            theoreticalNumberOfChargers++;
        }

        logger.info(
                String.format("Chargers: created %d work chargers with at least one plug (%d chargers with zero plugs)",
                        numberOfChargers, theoreticalNumberOfChargers));
        logger.info(
                String.format("  %d plugs at largest work place", maximumNumberOfPlugs));
    }

    /**
     * Generate public chargers.
     * 
     * - we distribute `slowPublicChargers` and `fastPublicChargers` randomly in the
     * network
     */
    public void configurePublicChargers(Scenario scenario) {
        configurePublicChargers(scenario, settings.seed + 5536122, settings.slowPublicChargers, "slow");
        configurePublicChargers(scenario, settings.seed + 6362377, settings.fastPublicChargers, "fast");
    }

    /**
     * Create a set of public chargers accordign to the PublicChargerSettings
     */
    public void configurePublicChargers(Scenario scenario, int seed, PublicChargerSettings chargerSettings,
            String publicName) {
        Random random = new Random(seed);
        String carMode = getChargingMode(scenario);

        int numberOfChargers = 0;

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(carMode));

        // make a list of all links and shuffle it
        List<Link> links = new LinkedList<>(roadNetwork.getLinks().values());
        Collections.shuffle(links, random);

        for (int k = 0; k < chargerSettings.count; k++) { // create N chargers for the first N links
            Link link = links.get(k);

            // describe the charger
            ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                    .id(Id.create("sevc:public:" + publicName + ":" + k, Charger.class)) // ,
                    .linkId(link.getId()) //
                    .chargerType("public:" + publicName) // only for analysis, no logical meaning
                    .plugPower(chargerSettings.power_kW * 1e3) //
                    .plugCount(chargerSettings.plugs) //
                    .build();

            // make removable in placement process
            ChargerPlacement.setRemovable(charger, true);

            // make this a selectable public charger
            StrategicChargingUtils.assignPublic(charger, true);

            // for aggregated analysis
            ChargerTypeAnalysisListener.addAnalysisType(charger, "public");
            ChargerTypeAnalysisListener.addAnalysisType(charger, "public:" + publicName);

            // add it to the infrastructure
            infrastructure.addChargerSpecification(charger);

            numberOfChargers++;
        }

        logger.info(
                String.format("Chargers: created %d public chargers of type %s", numberOfChargers, publicName));
    }

    /**
     * Loads additional public chargers
     */
    public void loadExternalChargers(Scenario scenario) {
        String carMode = getChargingMode(scenario);

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(carMode));

        int numberOfChargers = 0;
        for (var feature : GeoFileReader.getAllFeatures(settings.publicChargersFile)) {
            Coord location = MGC.point2Coord(((Point) feature.getDefaultGeometry()));
            Link link = NetworkUtils.getNearestLink(roadNetwork, location);

            String chargerType = (String) feature.getAttribute("charger_type");
            Preconditions.checkNotNull(chargerType, "The charger_type should be defined for input chargers.");
            Preconditions.checkState(chargerType.equals("slow") || chargerType.equals("fast"),
                    "The charger_type should be either 'slow' or 'fast'");

            Double power_kW = (Double) feature.getAttribute("power_kW");
            if (power_kW == null || power_kW <= 0) {
                power_kW = chargerType.equals("slow") ? settings.slowPublicChargers.power_kW
                        : settings.fastPublicChargers.power_kW;
            }

            Long plugs = (Long) feature.getAttribute("plugs");
            if (plugs == null || plugs == 0) {
                plugs = (Long) (long) (chargerType.equals("slow") ? settings.slowPublicChargers.plugs
                        : settings.fastPublicChargers.plugs);
            }

            // describe the charger
            ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                    .id(Id.create("sevc:public:" + chargerType + ":external:" + numberOfChargers, Charger.class)) // ,
                    .linkId(link.getId()) //
                    .chargerType("public:" + chargerType) // only for analysis, no logical meaning
                    .plugPower(power_kW * 1e3) //
                    .plugCount((int) plugs.longValue()) //
                    .build();

            // make this a selectable public charger
            StrategicChargingUtils.assignPublic(charger, true);

            // for aggregated analysis
            ChargerTypeAnalysisListener.addAnalysisType(charger, "public");
            ChargerTypeAnalysisListener.addAnalysisType(charger, "public:" + chargerType);

            // add it to the infrastructure
            infrastructure.addChargerSpecification(charger);

            numberOfChargers++;
        }

        logger.info(
                String.format("Chargers: created %d public chargers from external file", numberOfChargers));
    }

    /**
     * Configures home, work, and public chargers
     */
    public void configureChargers(Scenario scenario) {
        configureHomeChargers(scenario);
        configureWorkChargers(scenario);
        configurePublicChargers(scenario);

        if (settings.publicChargersFile != null) {
            loadExternalChargers(scenario);
        }
    }

    /**
     * This is the main method that will configure EV users, the vehicles, create
     * the charging infrastructure and adjust the configuration.
     */
    public void configureScenario(Scenario scenario) {
        configurePersons(scenario);
        configureVehicles(scenario);

        loadExistingChargingInfrastructure(scenario);
        configureChargers(scenario);

        configureConsumption(scenario.getConfig());
        configureCosts(scenario.getConfig());
        configureScoring(scenario.getConfig());
        configureInnovation(scenario.getConfig());

        if (settings.subscriptions.subscriptionRate > 0.0 && settings.subscriptions.availabilityRate > 0.0) {
            configureSubscriptions(scenario);
        }

        countUsersWithChargersAtWork(scenario);
    }

    /**
     * Returns the configured infrastructure
     */
    public ChargingInfrastructureSpecification getInfrastructure() {
        return infrastructure;
    }

    /**
     * Helper to integrate the infrastructure into the controller
     */
    public void applyInfrastructure(Controler controller) {
        EvUtils.registerInfrastructure(controller, getInfrastructure());
    }

    /**
     * Updates the configuration for vehicle-based consumption
     */
    public void configureConsumption(Config config) {
        EvConfigGroup evConfig = EvConfigGroup.get(config, true);
        evConfig.setDriveEnergyConsumption(DriveEnergyConsumption.AttributeBased);
    }

    /**
     * Updates the configuration for a tariff-based cost structure
     */
    public void configureCosts(Config config) {
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);

        ConfigGroup existing = (ConfigGroup) sevcConfig.getCostParameters();
        if (existing != null) {
            sevcConfig.removeParameterSet(existing);
            logger.info("Costs: removing existing cost parameters");
        }

        TariffBasedChargingCostsParameters tariffs = new TariffBasedChargingCostsParameters();
        sevcConfig.addParameterSet(tariffs);

        // we create the home tariff
        TariffParameters homeTariff = new TariffParameters();
        homeTariff.setTariffName("home");
        homeTariff.setCostPerEnergy_kWh(settings.homeChargers.costPerEnergy_EUR_kWh);
        tariffs.addParameterSet(homeTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("home")) {
                StrategicChargingUtils.addTariff(charger, "home");
            }
        }

        // we create the work tariff
        TariffParameters workTariff = new TariffParameters();
        workTariff.setTariffName("work");
        workTariff.setCostPerEnergy_kWh(settings.workChargers.costPerEnergy_EUR_kWh);
        tariffs.addParameterSet(workTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("work")) {
                StrategicChargingUtils.addTariff(charger, "work");
            }
        }

        if (settings.workChargers.withoutCostRate > 0.0) {
            // some work places get a free tariff
            TariffParameters freeTariff = new TariffParameters(); // keep "for free"
            freeTariff.setTariffName("work:free");
            tariffs.addParameterSet(freeTariff);

            // we make sure that x% of the plugs are free
            for (ChargerSpecification charger : getFreeWorkChargers()) {
                charger.getAttributes().removeAttribute(TariffBasedChargingCostCalculator.TARIFFS_CHARGER_ATTRIBUTE);
                StrategicChargingUtils.addTariff(charger, "work:free");
            }
        }

        // we create the public tariffs
        TariffParameters slowPublicTariff = new TariffParameters();
        slowPublicTariff.setTariffName("public:slow");
        slowPublicTariff.setCostPerDuration_min(settings.slowPublicChargers.costPerDuration_EUR_h / 60.0);
        slowPublicTariff.setCostPerEnergy_kWh(settings.slowPublicChargers.costPerEnergy_EUR_kWh);
        slowPublicTariff.setBlockingDuration_min(settings.slowPublicChargers.blockingDuration_min);
        slowPublicTariff.setCostPerBlockingDuration_min(settings.slowPublicChargers.blockingFee_EUR_min);
        tariffs.addParameterSet(slowPublicTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().startsWith("public:slow")) {
                StrategicChargingUtils.addTariff(charger, "public:slow");
            }
        }

        TariffParameters fastPublicTariff = new TariffParameters();
        fastPublicTariff.setTariffName("public:fast");
        fastPublicTariff.setCostPerDuration_min(settings.fastPublicChargers.costPerDuration_EUR_h / 60.0);
        fastPublicTariff.setCostPerEnergy_kWh(settings.fastPublicChargers.costPerEnergy_EUR_kWh);
        fastPublicTariff.setBlockingDuration_min(settings.fastPublicChargers.blockingDuration_min);
        fastPublicTariff.setCostPerBlockingDuration_min(settings.fastPublicChargers.blockingFee_EUR_min);
        tariffs.addParameterSet(fastPublicTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().startsWith("public:fast")) {
                StrategicChargingUtils.addTariff(charger, "public:fast");
            }
        }

        logger.info("Costs: added tariffs for home, work, public chargers");
    }

    /**
     * Updates the configuration to favor home-based charging
     */
    public void configureScoring(Config config) {
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);

        ChargingPlanScoringParameters scoring = sevcConfig.getScoringParameters();
        scoring.setDetourTime_min(-12.0 / 60.0); // VOT
        scoring.setBelowMinimumEndSoc(-20.0);
        scoring.setBelowMinimumSoc(-10.0);

        // bonus for charging at home
        ChargerTypeParams homeParams = new ChargerTypeParams();
        homeParams.setChargerType("home");
        homeParams.setConstant(settings.homeChargers.scoringBonus);
        scoring.addParameterSet(homeParams);
    }

    /**
     * Updates the innovation configuration
     */
    public void configureInnovation(Config config) {
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);

        sevcConfig.setSelectionStrategy(SelectionStrategy.Exponential);
        sevcConfig.setSelectionProbability(0.9);

        sevcConfig.getInnovationParameters().setConstraintIterations(1000);
        sevcConfig.getInnovationParameters().setConstraintErrorMode(ConstraintErrorMode.none);
        sevcConfig.getInnovationParameters().setConstraintFallbackBehavior(ConstraintFallbackBehavior.returnNone);

        sevcConfig.setMinimumEnrouteChargingDuration(600.0);
        sevcConfig.setMinimumEnrouteDriveTime(900.0);

        RandomChargingPlanInnovator.Parameters parameters = (RandomChargingPlanInnovator.Parameters) sevcConfig
                .getInnovationParameters();
        parameters.setActivityInclusionProbability(0.25);
        parameters.setLegInclusionProbability(0.25);
        parameters.setRetentionProbability(0.5);
        parameters.setUpdateChargerProbability(0.5);
    }

    /**
     * This is an optional method that will create a special subscription tariff for
     * certain registered users with a favorable tariff.
     */
    public void configureSubscriptions(Scenario scenario) {
        Random random = new Random(settings.seed + 113353);

        // get the tariffs
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(scenario.getConfig());
        TariffBasedChargingCostsParameters tariffs = (TariffBasedChargingCostsParameters) sevcConfig
                .getCostParameters();

        // we add a new special tariff for slow public chargers
        TariffParameters slowTariff = new TariffParameters();
        slowTariff.setTariffName("public:slow:subscription");
        slowTariff.setCostPerEnergy_kWh(
                settings.slowPublicChargers.costPerEnergy_EUR_kWh + settings.subscriptions.specialTariffDelta_EUR_kWh);
        slowTariff.setBlockingDuration_min(settings.slowPublicChargers.blockingDuration_min);
        slowTariff.setCostPerBlockingDuration_min(settings.slowPublicChargers.blockingFee_EUR_min);
        slowTariff.setSubscriptions(Set.of("subscription")); // only accessible with this subscription
        tariffs.addParameterSet(slowTariff);

        // we add a new special tariff for fast public chargers
        TariffParameters fastTariff = new TariffParameters();
        fastTariff.setTariffName("public:fast:subscription");
        fastTariff.setCostPerEnergy_kWh(
                settings.fastPublicChargers.costPerEnergy_EUR_kWh + settings.subscriptions.specialTariffDelta_EUR_kWh);
        fastTariff.setBlockingDuration_min(settings.fastPublicChargers.blockingDuration_min);
        fastTariff.setCostPerBlockingDuration_min(settings.fastPublicChargers.blockingFee_EUR_min);
        fastTariff.setSubscriptions(Set.of("subscription")); // only accessible with this subscription
        tariffs.addParameterSet(fastTariff);

        // add tariff to all chargers with the right type (for public fast chargers)
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().startsWith("public:slow")) {
                if (random.nextDouble() < settings.subscriptions.availabilityRate) {
                    StrategicChargingUtils.addTariff(charger, "public:slow:subscription");
                }
            } else if (charger.getChargerType().startsWith("public:fast")) {
                if (random.nextDouble() < settings.subscriptions.availabilityRate) {
                    StrategicChargingUtils.addTariff(charger, "public:fast:subscription");
                }
            }
        }

        // we add the subscription to a subset of the persons
        Population population = scenario.getPopulation();

        for (Person person : population.getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                if (random.nextDouble() < settings.subscriptions.subscriptionRate) {
                    StrategicChargingUtils.addSubscription(person, "subscription");
                }
            }
        }
    }

    /**
     * An analysis method that prints the percentage of ev users that have a charger
     * at work. Used for calibrating the work charger rate.
     */
    public void countUsersWithChargersAtWork(Scenario scenario) {
        // prepare lookup
        Population population = scenario.getPopulation();

        // track facilities with work charger
        IdMap<ActivityFacility, List<ChargerSpecification>> workplacesWithCharger = new IdMap<>(ActivityFacility.class);

        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().startsWith("work")) {
                for (Id<ActivityFacility> facilityId : StrategicChargingUtils.getChargerFacilities(charger)) {
                    workplacesWithCharger.computeIfAbsent(facilityId, id -> new LinkedList<>()).add(charger);
                }
            }
        }

        // counting
        int totalUsers = 0;
        int usersWithChargerAtWork = 0;
        int usersWithFreeChargerAtWork = 0;

        for (Person person : population.getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                boolean isWorking = false;
                boolean hasChargerAtWork = false;
                boolean hasFreeChargerAtWork = false;

                for (Activity activity : getWorkActivities(person)) {
                    if (activity.getFacilityId() != null) {
                        if (workplacesWithCharger.containsKey(activity.getFacilityId())) {
                            hasChargerAtWork = true;

                            for (ChargerSpecification charger : workplacesWithCharger.get(activity.getFacilityId())) {
                                if (StrategicChargingUtils.getTariffs(charger).contains("work:free")) {
                                    hasFreeChargerAtWork = true;
                                    break;
                                }
                            }
                        }
                    }

                    isWorking = true;
                }

                if (hasChargerAtWork) {
                    usersWithChargerAtWork++;
                }

                if (hasFreeChargerAtWork) {
                    usersWithFreeChargerAtWork++;
                }

                if (isWorking) {
                    totalUsers++;
                }
            }
        }

        logger.info("Work charger analysis ...");
        logger.info(
                String.format("  %.2f%% of working ev users have charger at work",
                        100.0 * (double) usersWithChargerAtWork / (double) totalUsers));
        logger.info(
                String.format("  %.2f%% of working ev users with work charger have free access",
                        100.0 * (double) usersWithFreeChargerAtWork / (double) usersWithChargerAtWork));
    }

    // THIS IS THE COMMAND LINE SCRIPT

    /*
     * The following parameters can be given to the command line script:
     * 
     * --config-path [path] defines the configuration file that will be updated
     * 
     * --settings-path [path] optionally points to a JSON script that will override
     * the default settings
     * 
     * --plans-path [path] optionally overrides the plans path in the config (in
     * case you want to apply the changes to an output plans file of an already
     * performed simulation)
     * 
     * This script will write out a config file that is prefixed with "sevc_". Other
     * files such as the population will also be prefixed and generated next to the
     * config file.
     */
    static public void main(String[] args)
            throws ConfigurationException, StreamReadException, DatabindException, IOException {
        CommandLine cmd = new CommandLine.Builder(args) //
                .requireOptions("config-path") //
                .allowOptions("settings-path", "plans-path", "prefix") //
                .build();

        // load config and scenario
        Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));

        if (cmd.hasOption("plans-path")) {
            // in case input plans should be overridden
            config.plans().setInputFile(cmd.getOptionStrict("plans-path"));
        }

        // no need to load certain data sets
        String householdsInputPath = config.households().getInputFile();
        config.households().setInputFile(null);

        String transitScheduleInputPath = config.transit().getTransitScheduleFile();
        config.transit().setTransitScheduleFile(null);

        String transitVehiclesInputPath = config.transit().getVehiclesFile();
        config.transit().setVehiclesFile(null);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        // load settings
        Settings settings = new Settings();

        if (cmd.hasOption("settings-path")) {
            // load from JSON if given
            settings = new ObjectMapper().readValue(new File(cmd.getOptionStrict("settings-path")), Settings.class);
        }

        // execute configurator
        StrategicChargingScenarioConfigurator configurator = new StrategicChargingScenarioConfigurator(settings);
        configurator.configureScenario(scenario);

        // prepare writing
        String prefix = cmd.getOption("prefix").orElse("sevc");
        File parentPath = new File(cmd.getOptionStrict("config-path")).getParentFile();

        // write assets
        new PopulationWriter(scenario.getPopulation())
                .write(new File(parentPath, prefix + "_plans.xml.gz").toString());
        config.plans().setInputFile(prefix + "_plans.xml.gz");

        new MatsimVehicleWriter(scenario.getVehicles())
                .writeFile(new File(parentPath, prefix + "_vehicles.xml.gz").toString());
        config.vehicles().setVehiclesFile(prefix + "_vehicles.xml.gz");

        new ChargerWriter(configurator.getInfrastructure().getChargerSpecifications().values().stream())
                .write(new File(parentPath, prefix + "_chargers.xml.gz").toString());
        EvConfigGroup.get(config).setChargersFile(prefix + "_chargers.xml.gz");

        // reset ignored files
        config.households().setInputFile(householdsInputPath);
        config.transit().setTransitScheduleFile(transitScheduleInputPath);
        config.transit().setVehiclesFile(transitVehiclesInputPath);

        // write config
        new ConfigWriter(config).write(new File(parentPath, prefix + "_config.xml").toString());
    }

    // BELOW THIS POINT ONLY HELPER METHODS

    private void setVehicleId(Person person, Id<Vehicle> vehicleId, String carMode) {
        Map<String, Id<Vehicle>> vehicles = new HashMap<>();
        vehicles.putAll(VehicleUtils.getVehicleIds(person));
        vehicles.put(carMode, vehicleId);
        VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, vehicles);
    }

    private List<Person> findRelevantPersons(Population population, String carMode) {
        List<Person> relevant = new LinkedList<>();

        for (Person person : population.getPersons().values()) {
            String subpopulation = PopulationUtils.getSubpopulation(person);

            if (settings.persons.subpopulations.size() > 0
                    && !settings.persons.subpopulations.contains(subpopulation)) {
                continue;
            }

            boolean foundCar = false;

            for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan())) {
                if (leg.getMode().equals(carMode)) {
                    foundCar = true;
                    break;
                }
            }

            boolean foundHome = false;

            for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                    StageActivityHandling.ExcludeStageActivities)) {
                if (activity.getType().startsWith("home")) {
                    foundHome = true;
                    break;
                }
            }

            if (foundCar && foundHome) {
                relevant.add(person);
            }
        }

        return relevant;
    }

    private Id<Link> getHomeLinkId(Person person, boolean allowNull) {
        for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                StageActivityHandling.ExcludeStageActivities)) {
            if (activity.getType().startsWith("home")) {
                return Objects.requireNonNull(activity.getLinkId());
            }
        }

        if (allowNull) {
            return null;
        } else {
            throw new IllegalStateException();
        }
    }

    private IdMap<ActivityFacility, Set<Id<Person>>> trackEmployees(Population population,
            ActivityFacilities facilities) {
        IdMap<ActivityFacility, Set<Id<Person>>> employees = new IdMap<>(ActivityFacility.class);

        for (Person person : population.getPersons().values()) {
            for (Activity activity : getWorkActivities(person)) {
                Id<ActivityFacility> facilityId = activity.getFacilityId();

                if (facilityId != null) {
                    employees.computeIfAbsent(facilityId, id -> new HashSet<>()).add(person.getId());
                }
            }
        }

        return employees;
    }

    private List<Activity> getWorkActivities(Person person) {
        List<Activity> activities = new LinkedList<>();

        for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                StageActivityHandling.ExcludeStageActivities)) {
            if (activity.getType().startsWith("work")) {
                activities.add(activity);
            }
        }

        return activities;
    }

    private void updateVehicleInPlans(Person person, Id<Vehicle> vehicleId, String carMode) {
        for (Plan plan : person.getPlans()) {
            for (Leg leg : TripStructureUtils.getLegs(plan)) {
                if (leg.getMode().equals(carMode)) {
                    ((NetworkRoute) leg.getRoute()).setVehicleId(vehicleId);
                }
            }
        }
    }

    private String getChargingMode(Scenario scenario) {
        WithinDayEvConfigGroup wevcConfig = WithinDayEvConfigGroup.get(scenario.getConfig());
        return wevcConfig.getCarMode();
    }

    private record ZonalRate(Polygon geometry, double rate) {
    }

    private IdMap<Person, Double> getZonalHomeChargerRates(Scenario scenario) {
        logger.info("Calculating zonal home charger ownership rates ...");

        List<ZonalRate> zones = new LinkedList<>();

        for (var feature : GeoFileReader.getAllFeatures(settings.homeChargers.ownershipRateFile)) {
            Polygon geometry = (Polygon) feature.getDefaultGeometry();

            Double rate = (Double) feature.getAttribute("ownership_rate");
            if (rate == null) {
                rate = settings.homeChargers.ownershipRate;
            }

            zones.add(new ZonalRate(geometry, rate));
        }

        IdMap<Person, Double> zonalRates = new IdMap<>(Person.class);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                Id<Link> homeLinkId = getHomeLinkId(person, true);

                if (homeLinkId != null) {
                    Link link = scenario.getNetwork().getLinks().get(homeLinkId);
                    Point location = MGC.coord2Point(link.getCoord());

                    for (ZonalRate zone : zones) {
                        if (zone.geometry.covers(location)) {
                            zonalRates.put(person.getId(), zone.rate);
                        }
                    }
                }
            }
        }

        logger.info("  Done!");

        return zonalRates;
    }

    private IdMap<Person, Double> getZonalVehicleOwnershipRates(Scenario scenario) {
        logger.info("Calculating zonal vehicle ownership rates ...");

        List<ZonalRate> zones = new LinkedList<>();

        for (var feature : GeoFileReader.getAllFeatures(settings.persons.ownershipRateFile)) {
            Polygon geometry = (Polygon) feature.getDefaultGeometry();

            Double rate = (Double) feature.getAttribute("ownership_rate");
            if (rate == null) {
                rate = settings.persons.ownershipRate;
            }

            zones.add(new ZonalRate(geometry, rate));
        }

        IdMap<Person, Double> personRates = new IdMap<>(Person.class);

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id<Link> homeLinkId = getHomeLinkId(person, true);

            if (homeLinkId != null) {
                Link link = scenario.getNetwork().getLinks().get(homeLinkId);
                Point location = MGC.coord2Point(link.getCoord());

                for (ZonalRate zone : zones) {
                    if (zone.geometry.covers(location)) {
                        personRates.put(person.getId(), zone.rate);
                    }
                }
            }
        }

        logger.info("  Done!");

        return personRates;
    }

    /**
     * This helper produces a list of work chargers that should obtain a free
     * tariff. The logic is that the number of plugs is proportionate to the number
     * of employees. And our goal is that X% of persons with access to a work
     * charger have access to a free charger. Therefore, we put the work chargers in
     * a random order and then collect them until we reach X% of the plugs. The
     * collected ones get a free tariff, the others remain priced.
     */
    private List<ChargerSpecification> getFreeWorkChargers() {
        Random random = new Random(settings.seed + 581272);

        List<ChargerSpecification> workChargers = new LinkedList<>();
        int totalPlugs = 0;

        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().startsWith("work")) {
                workChargers.add(charger);
                totalPlugs += charger.getPlugCount();
            }
        }

        Collections.shuffle(workChargers, random);

        int endIndex = 0;
        int cumulativePlugs = 0;

        while (endIndex < workChargers.size()) {
            cumulativePlugs += workChargers.get(endIndex).getPlugCount();

            if (cumulativePlugs > totalPlugs * settings.workChargers.withoutCostRate) {
                // we collected enough chargers to have 50% of plugs for free
                break;
            }

            endIndex++;
        }

        return workChargers.subList(0, endIndex);
    }
}
