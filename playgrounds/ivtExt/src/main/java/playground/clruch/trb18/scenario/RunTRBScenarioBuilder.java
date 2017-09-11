package playground.clruch.trb18.scenario;

import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNetworkIO;
import playground.clruch.netdata.MatsimKMEANSVirtualNetworkCreator;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.trb18.scenario.stages.TRBAVPlanSelector;
import playground.clruch.trb18.scenario.stages.TRBBackgroundTrafficCleaner;
import playground.clruch.trb18.scenario.stages.TRBConfigModifier;
import playground.clruch.trb18.scenario.stages.TRBNetworkModifier;
import playground.clruch.trb18.scenario.stages.TRBNetworkRadiusFilter;
import playground.clruch.trb18.scenario.stages.TRBNetworkShapeFilter;
import playground.clruch.trb18.scenario.stages.TRBPlanModifier;
import playground.clruch.trb18.scenario.stages.TRBPopulationCleaner;
import playground.clruch.trb18.scenario.stages.TRBPopulationDecimiser;
import playground.clruch.trb18.scenario.stages.TRBPopulationPreparer;
import playground.clruch.trb18.scenario.stages.TRBReducedPopulationExtractor;
import playground.clruch.trb18.scenario.stages.TRBRouteAppender;
import playground.clruch.trb18.scenario.stages.TRBSlowModeCleaner;

public class RunTRBScenarioBuilder {
    public static void main(String args[]) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        TRBScenarioConfig scenarioConfig = new TRBScenarioConfig();

        if (args.length > 0 && new File(args[0]).exists()) {
            scenarioConfig = mapper.readValue(new File(args[0]), TRBScenarioConfig.class);
        } else {
            mapper.writeValue(new File("builder.json"), scenarioConfig);
        }

        //ConfigUtils.loadConfig(args[0], config);

        // Read all the necessary data

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Population originalPopulation = scenario.getPopulation();
        new PopulationReader(scenario).readFile(scenarioConfig.populationInputPath);
        new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(scenarioConfig.populationAttributesInputPath);

        Network originalNetwork = scenario.getNetwork();
        new MatsimNetworkReader(originalNetwork).readFile(scenarioConfig.networkInputPath);

        // Clean up input scenario & filter network
        Population population = new TRBPopulationCleaner().clean(originalPopulation);
        Network filteredNetwork;

        if (scenarioConfig.shapefileInputPath != null) {
            filteredNetwork = new TRBNetworkShapeFilter().filter(originalNetwork, scenarioConfig.shapefileInputPath);
        } else {
            filteredNetwork = new TRBNetworkRadiusFilter().filter(originalNetwork, new Coord(scenarioConfig.centerX, scenarioConfig.centerY), scenarioConfig.radius);
        }

        // --> Now the population looks like an "initial" population without routes and collapsed PT trips
        // --> The filtered network only contains links that are usable by AVs within the specified area

        // Modify main network with AV modes
        new TRBNetworkModifier().modify(originalNetwork, filteredNetwork);

        // --> All links where AVs can drive are now annotated with "av" mode in the network!

        // Add AV plans
        TRBPlanModifier planModifier = new TRBPlanModifier(filteredNetwork, scenarioConfig.allowMultimodalPlans);
        new TRBPopulationPreparer(planModifier).filter(population, originalPopulation.getPersonAttributes());

        // --> Now all agents that MAY use AVs in the specified area have an (unselected) plan with AV trip within the area
        //     See TRBPlanModifier for how a plan and/or a trip is eligible to be converted to AV

        // Clean up useless (or unwanted) stuff
        new TRBSlowModeCleaner().clean(population);

        if (scenarioConfig.removeBackgroundTraffic) {
            new TRBBackgroundTrafficCleaner().clean(population);
        }

        // Optionally, remove agents until a maximum number of agents is reached
        new TRBPopulationDecimiser(new Random(0L)).decimise(population, scenarioConfig.maximumNumberOfAgents);

        // Extract reduced population (only the AV agents) for further processing with VirtualNetwork etc.
        Population reducedPopulation = new TRBReducedPopulationExtractor().run(population);
        // --> This can be used to generate the virtual network and to find the requests for the LP Feedfoward Dispatcher

        // --> Now population does not contain slow-mode agents and maybe also no background traffic!

        // Reassign routes and multi-part PT trips to the new population
        new TRBRouteAppender().run(population, scenario.getPopulation());

        // --> Now the population is ready to use, but AV plans are NOT selected

        // Select AV plans
        new TRBAVPlanSelector(new Random(0L)).selectAVPlans(population, scenarioConfig.avShare);

        // --> Now people have selected AV plans

        // Modify the config file
        Config matsimConfig = ConfigUtils.loadConfig(scenarioConfig.configInputPath);
        new TRBConfigModifier().modify(matsimConfig, population, scenarioConfig);

        // --> Everything done now from the pure MATSim side

        // Write all the relevant data
        new PopulationWriter(population).write(scenarioConfig.populationOutputPath);
        new ObjectAttributesXmlWriter(population.getPersonAttributes()).writeFile(scenarioConfig.populationAttributesOutputPath);
        new NetworkWriter(scenario.getNetwork()).write(scenarioConfig.fullNetworkOutputPath);
        new NetworkWriter(filteredNetwork).write(scenarioConfig.filteredNetworkOutputPath);
        new ConfigWriter(matsimConfig).write(scenarioConfig.configOutputPath);

        // Now Dispatching stuff!
        // Attention, there are some hacks here to give the IDSC methods that data they want
        //   (they don't like populations where not EVERY leg is AV)
        //   See further down for the generated "hack" populations
        //
        // Eventually, the IDSC scripts should be adapted (TripStructureUtils etc.)

        MatsimKMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new MatsimKMEANSVirtualNetworkCreator();
        Population virtualNetworkPopulation = createVirtualNetworkPopulation(reducedPopulation);
        VirtualNetwork<Link> virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(virtualNetworkPopulation, filteredNetwork, scenarioConfig.numberOfVirtualNodes, true);

        final File virtualNetworkOutputDirectory = new File(scenarioConfig.virtualNetworkOutputDirectory);
        virtualNetworkOutputDirectory.mkdir();

        (new VirtualNetworkIO<Link>()).toByte(new File(virtualNetworkOutputDirectory, scenarioConfig.virtualNetworkFileName), virtualNetwork);
        //VirtualNetworkIO.toXML(new File(virtualNetworkOutputDirectory, scenarioConfig.virtualNetworkFileName + ".xml").toString(), virtualNetwork);

        Population requestSchedulePopulation = createRequestSchedulePopulation(reducedPopulation);
        PopulationRequestSchedule prs = new PopulationRequestSchedule(filteredNetwork, requestSchedulePopulation, virtualNetwork);
        prs.exportCsv();

        Population travelDataPopulation = createTravelDataPopulation(reducedPopulation);
        TravelData travelData = new TravelData(virtualNetwork, filteredNetwork, travelDataPopulation, scenarioConfig.dtTravelData);
        TravelDataIO.toByte(new File(virtualNetworkOutputDirectory, scenarioConfig.travelDataFileName), travelData);
    }

    /**
     * KMEANSVirtualNetworkCreator is only interested in the activities (ie. activites that are reached or left by AV!)
     * This method creates a population that only contains activities. Input should be a reduced population.
     */
    static public Population createVirtualNetworkPopulation(Population population) {
        Population vnPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person person : population.getPersons().values()) {
            Person vnPerson = vnPopulation.getFactory().createPerson(person.getId());
            Plan vnPlan = vnPopulation.getFactory().createPlan();

            Set<Activity> activities = new HashSet<>();

            for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE))) {
                if (trip.getLegsOnly().get(0).getMode().equals("av")) {
                    activities.add(trip.getOriginActivity());
                    activities.add(trip.getDestinationActivity());
                }
            }

            vnPlan.getPlanElements().addAll(activities);

            vnPerson.addPlan(vnPlan);
            vnPopulation.addPerson(vnPerson);
        }

        return vnPopulation;
    }

    /**
     * PopulationRequestSchedule is looking for ACT -> LEG -> ACT sequences to find trips
     * This function creates plans that only contain such sequences with actual AV legs.
     */
    static public Population createRequestSchedulePopulation(Population population) {
        Population rsPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person person : population.getPersons().values()) {
            Person rsPerson = rsPopulation.getFactory().createPerson(person.getId());
            Plan rsPlan = rsPopulation.getFactory().createPlan();

            for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan(), new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE))) {
                if (trip.getLegsOnly().get(0).getMode().equals("av")) {
                    rsPlan.addActivity(trip.getOriginActivity());
                    rsPlan.addLeg(trip.getLegsOnly().get(0));
                    rsPlan.addActivity(trip.getDestinationActivity());
                }
            }

            rsPerson.addPlan(rsPlan);
            rsPopulation.addPerson(rsPerson);
        }

        return rsPopulation;
    }

    /**
     * TravelData does not like legs that depart after 30:00:00
     */
    static public Population createTravelDataPopulation(Population population) {
        Population tdPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person person : population.getPersons().values()) {
            Person tdPerson = tdPopulation.getFactory().createPerson(person.getId());
            Plan tdPlan = PlanUtils.createCopy(person.getSelectedPlan());

            for (PlanElement element : tdPlan.getPlanElements()) {
                if (element instanceof Leg) {
                    Leg leg = (Leg) element;

                    if (leg.getDepartureTime() >= 30.0 * 60.0 * 60.0) {
                        leg.setDepartureTime(30.0 * 60.0 * 60.0 - 1.0);
                    }
                }
            }

            tdPerson.addPlan(tdPlan);
            tdPopulation.addPerson(tdPerson);
        }

        return tdPopulation;
    }
}
