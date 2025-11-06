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
import org.matsim.contrib.ev.EvUtils;
import org.matsim.contrib.ev.fleet.ElectricFleetUtils;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerReader;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargerWriter;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecificationDefaultImpl;
import org.matsim.contrib.ev.infrastructure.ImmutableChargerSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingScenarioConfigurator.Settings.PublicChargerSettings;
import org.matsim.contrib.ev.strategic.analysis.ChargerTypeAnalysisListener;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters;
import org.matsim.contrib.ev.strategic.costs.TariffBasedChargingCostsParameters.TariffParameters;
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
            public double ownershipRate = 0.2;

            // desired minimum soc (uniform across all users)
            public double minimumSoc = 0.2;

            // desired minimum soc at the end of the day (uniform across all users)
            public double minimumEndOfDaySoc = 0.5;
        }

        public PersonSettings persons = new PersonSettings();

        public class VehicleSettings {
            // battery capacity of the electric vehicles (uniform across all evs)
            public double batteryCapacity_kWh = 75.0;

            // initial soc at the beginning of the day (lower bound, uniformly sampled)
            public double minimumInitialSoc = 0.3;

            // initial soc at the beginning of the day (upper bound, uniformly sampled)
            public double maximumInitialSoc = 0.9;

            // maximum soc until which a vehicle is charged (uniform across all evs)
            public double maximumSoc = 0.9;

            // whether to use existing electric vehicles for the persons or not
            public boolean considerExistingElectricVehicles = false;
        }

        public VehicleSettings vehicles = new VehicleSettings();

        public class HomeChargerSettings {
            // percentage of ev users that have a charger at home
            public double ownershipRate = 0.4;

            // charging power of home chargers (uniform overall home chargers)
            public double plugPower_kW = 7.0;

            // cost per energy consumption of a home charger
            public double costPerEnergy_kWh = 0.3;

            // bonus that is given for home chargers in scoring
            public double scoringBonus = 10.0;
        }

        public HomeChargerSettings homeChargers = new HomeChargerSettings();

        public class WorkChargerSettings {
            // minimum number of persons working at a facility so it receives work chargers
            public int minimumEmployees = 20;

            // number of plugs generated per number of employees at each eligible facility
            public double plugsPerEmployee = 0.2;

            // power of work chargers
            public double plugPower_kW = 12.0;
        }

        public WorkChargerSettings workChargers = new WorkChargerSettings();

        public class PublicChargerSettings {
            // number of public chargers to be created randomly in the network
            public int count = 500;

            // number of plugs per created charger
            public int plugs = 3;

            // power of public chargers (uniform overall public chargers)
            public double power_kW = 18.0;

            // cost per hour charged when using a public charger
            public double costPerDuration_h = 5.0;

            public PublicChargerSettings() {
            }

            PublicChargerSettings(double power_kWh, double costPerDuration_h) {
                this.power_kW = power_kWh;
                this.costPerDuration_h = costPerDuration_h;
            }
        }

        public PublicChargerSettings slowPublicChargers = new PublicChargerSettings(18.0, 5.0);
        public PublicChargerSettings fastPublicChargers = new PublicChargerSettings(55.0, 10.0);

        // a file defining additional public chargeres (point geometries, with
        // "charger_type" column [slow/fast] and optional "power_kW" and "plugs")
        public String externalChargersFile = null;

        public boolean considerExistingChargingInfrastructure = false;

        public class SubscriptionSettings {
            // number of ev users holding a special tariff subscription for public chargers
            public double subscriptionRate = 0.2;

            // number of public chargers being eligible for special tariff charging
            public double availabilityRate = 0.7;

            // cost per hour for the special charging tariff
            public double specialTariffCost_h = 2.0;
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

        int numberOfPersons = 0;
        for (Person person : persons) {
            if (random.nextDouble() < settings.persons.ownershipRate) { // only select a few
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

                    // set the initial SoC of the electric vehicle
                    double initialSoc = settings.vehicles.minimumInitialSoc;
                    initialSoc += random.nextDouble()
                            * (settings.vehicles.maximumInitialSoc - settings.vehicles.minimumInitialSoc);

                    ElectricFleetUtils.setInitialSoc(vehicle, initialSoc);

                    // set the maximum SoC up to which the person will charge (this is optional, 1.0
                    // is assumed)
                    StrategicChargingUtils.setMaximumSoc(vehicle, settings.vehicles.maximumSoc);

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

        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                if (random.nextDouble() < settings.homeChargers.ownershipRate) { // person gets a home charger
                    // describe the charger
                    ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                            .id(Id.create("sevc:home:" + person.getId().toString(), Charger.class)) // ,
                            .linkId(getHomeLinkId(person)) //
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
        String carMode = getChargingMode(scenario);

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(carMode));

        ActivityFacilities facilities = scenario.getActivityFacilities();

        int numberOfChargers = 0;
        int maximumNumberOfPlugs = 0;

        for (var entry : countEmployees(scenario.getPopulation(), facilities).entrySet()) {
            int employees = entry.getValue();

            if (employees >= settings.workChargers.minimumEmployees) {
                ActivityFacility facility = facilities.getFacilities().get(entry.getKey());
                int plugs = (int) Math.floor(employees * settings.workChargers.plugsPerEmployee);

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
        }

        logger.info(
                String.format("Chargers: created %d work chargers", numberOfChargers));
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
                String.format("Chargers: created %d public chargers of type %s: ", numberOfChargers, publicName));
    }

    /**
     * Loads additional public chargers
     */
    public void loadExternalChargers(Scenario scenario) {
        String carMode = getChargingMode(scenario);

        Network roadNetwork = NetworkUtils.createNetwork(scenario.getConfig());
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(roadNetwork, Collections.singleton(carMode));

        int numberOfChargers = 0;
        for (var feature : GeoFileReader.getAllFeatures(settings.externalChargersFile)) {
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

            Integer plugs = (Integer) feature.getAttribute("plugs");
            if (plugs == null || plugs == 0) {
                plugs = chargerType.equals("slow") ? settings.slowPublicChargers.plugs
                        : settings.fastPublicChargers.plugs;
            }

            // describe the charger
            ChargerSpecification charger = ImmutableChargerSpecification.newBuilder() //
                    .id(Id.create("sevc:public:" + chargerType + ":external:" + numberOfChargers, Charger.class)) // ,
                    .linkId(link.getId()) //
                    .chargerType("public:" + chargerType) // only for analysis, no logical meaning
                    .plugPower(power_kW * 1e3) //
                    .plugCount(plugs) //
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

        if (settings.externalChargersFile != null) {
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

        configureCosts(scenario.getConfig());

        if (settings.subscriptions.subscriptionRate > 0.0 && settings.subscriptions.availabilityRate > 0.0) {
            configureSubscriptions(scenario);
        }
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
     * Updates the configuration for a tariff-based cost structure
     */
    public void configureCosts(Config config) {
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);

        ConfigGroup existing = (ConfigGroup) sevcConfig.getCostParameters();
        if (existing != null) {
            sevcConfig.removeParameterSet(existing);
            logger.warn("Costs: removing existing cost parameters");
        }

        TariffBasedChargingCostsParameters tariffs = new TariffBasedChargingCostsParameters();
        sevcConfig.addParameterSet(tariffs);

        // we create the home tariff
        TariffParameters homeTariff = new TariffParameters();
        homeTariff.setTariffName("home");
        homeTariff.setCostPerEnergy_kWh(settings.homeChargers.costPerEnergy_kWh); // cost
        // per
        // kWh
        tariffs.addParameterSet(homeTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("home")) {
                StrategicChargingUtils.addTariff(charger, "home");
            }
        }

        // we create the work tariff
        TariffParameters workTariff = new TariffParameters(); // keep "for free"
        workTariff.setTariffName("work");
        tariffs.addParameterSet(workTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("work")) {
                StrategicChargingUtils.addTariff(charger, "work");
            }
        }

        // we create the public tariffs
        TariffParameters slowPublicTariff = new TariffParameters();
        slowPublicTariff.setTariffName("public:slow");
        slowPublicTariff.setCostPerDuration_min(settings.slowPublicChargers.costPerDuration_h / 60.0);
        tariffs.addParameterSet(slowPublicTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("public:slow")) {
                StrategicChargingUtils.addTariff(charger, "public:slow");
            }
        }

        TariffParameters fastPublicTariff = new TariffParameters();
        fastPublicTariff.setTariffName("public:fast");
        fastPublicTariff.setCostPerDuration_min(settings.fastPublicChargers.costPerDuration_h / 60.0);
        tariffs.addParameterSet(fastPublicTariff);

        // add tariff to all chargers with the right type
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("public:fast")) {
                StrategicChargingUtils.addTariff(charger, "public:fast");
            }
        }

        logger.warn("Costs: added tariffs for home, work, public chargers");
    }

    /**
     * Updates the configuration to favor home-based charging
     */
    public void configureScoring(Config config) {
        StrategicChargingConfigGroup sevcConfig = StrategicChargingConfigGroup.get(config);

        ChargerTypeParams homeParams = new ChargerTypeParams();
        homeParams.setChargerType("home");
        homeParams.setConstant(settings.homeChargers.scoringBonus);

        sevcConfig.getScoringParameters().addParameterSet(sevcConfig);
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

        // we add a new special tariff for public chargers
        TariffParameters specialTariff = new TariffParameters();
        specialTariff.setTariffName("special");
        specialTariff.setCostPerDuration_min(settings.subscriptions.specialTariffCost_h / 60.0);
        specialTariff.setSubscriptions(Set.of("special_subscription")); // only accessible with this subscription
        tariffs.addParameterSet(specialTariff);

        // add tariff to all chargers with the right type (for public fast chargers)
        for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
            if (charger.getChargerType().equals("public:fast")) {
                if (random.nextDouble() < settings.subscriptions.availabilityRate) {
                    StrategicChargingUtils.addTariff(charger, "special");
                }
            }
        }

        // we add the subscription to a subset of the persons
        Population population = scenario.getPopulation();

        for (Person person : population.getPersons().values()) {
            if (WithinDayEvUtils.isActive(person)) {
                if (random.nextDouble() < settings.subscriptions.subscriptionRate) {
                    StrategicChargingUtils.addSubscription(person, "special_subscription");
                }
            }
        }
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

    private Id<Link> getHomeLinkId(Person person) {
        for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                StageActivityHandling.ExcludeStageActivities)) {
            if (activity.getType().startsWith("home")) {
                return Objects.requireNonNull(activity.getLinkId());
            }
        }

        throw new IllegalStateException();
    }

    private IdMap<ActivityFacility, Integer> countEmployees(Population population, ActivityFacilities facilities) {
        IdMap<ActivityFacility, Integer> count = new IdMap<>(ActivityFacility.class);

        for (Person person : population.getPersons().values()) {
            for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
                    StageActivityHandling.ExcludeStageActivities)) {
                if (activity.getType().startsWith("work")) {
                    Id<ActivityFacility> facilityId = activity.getFacilityId();

                    if (facilityId != null) {
                        count.compute(facilityId, (key, value) -> value == null ? 1 : value + 1);
                    }
                }
            }
        }

        return count;
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
}
