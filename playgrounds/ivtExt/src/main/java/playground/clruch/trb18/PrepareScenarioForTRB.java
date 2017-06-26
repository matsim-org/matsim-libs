package playground.clruch.trb18;

import contrib.baseline.preparation.ZHCutter;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.locationchoice.utils.PlanUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.clruch.netdata.KMEANSVirtualNetworkCreator;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNetworkIO;
import playground.clruch.prep.NetworkCutClean;
import playground.clruch.prep.PopulationRequestSchedule;
import playground.clruch.traveldata.TravelData;
import playground.clruch.traveldata.TravelDataIO;
import playground.clruch.utils.GlobalAssert;
import playground.sebhoerl.avtaxi.config.AVConfig;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PrepareScenarioForTRB {
    final String VIRTUALNETWORKFOLDERNAME = "virtualNetwork";
    final String VIRTUALNETWORKFILENAME = "virtualNetwork";
    final String TRAVELDATAFILENAME = "travelData";

    final private Coord scenarioCenterCoord;
    final private double scenarioRadius;
    final private double avRadius;

    final private File outputDirectory;

    static public void main(String[] args) throws Exception {
        Config config = ConfigUtils.loadConfig(args[0]);

        String transitScheduleFile = config.transit().getTransitScheduleFile();
        String transitVehiclesFile = config.transit().getVehiclesFile();

        config.transit().setTransitScheduleFile(null);
        config.transit().setVehiclesFile(null);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        double avShare = Double.parseDouble(args[1]);
        int numberOfVirtualNodes = Integer.parseInt(args[2]);
        int dtTravelData = Integer.parseInt(args[3]);
        double avRadius = Double.parseDouble(args[4]);
        String outputPath = ".";

        PrepareScenarioForTRB prepare = new PrepareScenarioForTRB(new File(outputPath), avRadius);

        prepare.preparePopulation(scenario.getPopulation());
        prepare.prepareActivities(scenario.getPopulation(), scenario.getActivityFacilities());
        Network reducedNetwork = prepare.createAndWriteVirtualNetwork(config, scenario.getPopulation(), numberOfVirtualNodes, dtTravelData);

        prepare.applyAVPlans(scenario.getPopulation(), reducedNetwork);
        prepare.adjustInitialAVShare(scenario.getPopulation(), avShare);

        new PopulationWriter(scenario.getPopulation()).write(new File(new File(outputPath), "trb_population.xml.gz").getAbsolutePath());

        config.transit().setTransitScheduleFile(transitScheduleFile);
        config.transit().setVehiclesFile(transitVehiclesFile);

        prepare.prepareConfig(config, scenario.getPopulation());

        new ConfigWriter(config).write(new File(new File(outputPath), "trb_config.xml").getAbsolutePath());

        prepare.applyAVToNetwork(scenario.getNetwork(), reducedNetwork);
        new NetworkWriter(scenario.getNetwork()).write(new File(new File(outputPath), "trb_network.xml.gz").getAbsolutePath());
    }

    public PrepareScenarioForTRB(File outputDirectory, double avRadius) {
        this.outputDirectory = outputDirectory;
        this.outputDirectory.mkdirs();

        ZHCutter.ZHCutterConfigGroup zhConfig = new ZHCutter.ZHCutterConfigGroup("");
        this.scenarioCenterCoord = new Coord(zhConfig.getxCoordCenter(), zhConfig.getyCoordCenter());
        this.scenarioRadius = zhConfig.getRadius();
        this.avRadius = avRadius;
    }

    final private static Logger logger = Logger.getLogger(PrepareScenarioForTRB.class);

    final private List<String> modePrecedence = Arrays.asList(TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk);
    final private Set<String> allowedMainModes = new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.pt));

    private String findMainMode(Plan plan) {
        String mainMode = null;

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Leg) {
                Leg leg = (Leg) element;

                if (mainMode == null || (modePrecedence.indexOf(leg.getMode()) < modePrecedence.indexOf(mainMode))) {
                    mainMode = leg.getMode();
                }
            }
        }

        return mainMode;
    }

    private boolean isAVPlan(Plan plan) {
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Leg) {
                if (((Leg) element).getMode().equals(AVModule.AV_MODE)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void applyAVToNetwork(Network network, Network reducedNetwork) {
        for (Id<Link> linkId : reducedNetwork.getLinks().keySet()) {
            Link link = network.getLinks().get(linkId);

            HashSet<String> allowedModes = new HashSet<>(link.getAllowedModes());
            allowedModes.add(AVModule.AV_MODE);

            link.setAllowedModes(allowedModes);
        }
    }

    public void prepareConfig(Config config, Population population) {
        List<StrategyConfigGroup.StrategySettings> mainStrategies = config.strategy().getStrategySettings().stream().filter(s -> s.getSubpopulation() == null).collect(Collectors.toList());
        config.strategy().clearStrategySettings();

        for (StrategyConfigGroup.StrategySettings strategy : mainStrategies) {
            String name = strategy.getStrategyName();

            if (name.equals("ChangeExpBeta") || name.equals("ReRoute")) {
                config.strategy().addStrategySettings(strategy);
            }
        }

        config.plans().setInputFile("trb_population.xml.gz");
        config.network().setInputFile("trb_network.xml.gz");

        PlanCalcScoreConfigGroup.ModeParams avParams = config.planCalcScore().getOrCreateModeParams(AVModule.AV_MODE);
        PlanCalcScoreConfigGroup.ModeParams carParams = config.planCalcScore().getOrCreateModeParams(TransportMode.car);

        avParams.setConstant(carParams.getConstant());
        avParams.setMarginalUtilityOfTraveling(carParams.getMarginalUtilityOfTraveling());
        avParams.setMonetaryDistanceRate(carParams.getMonetaryDistanceRate());

        AVConfigGroup avConfig = new AVConfigGroup();
        avConfig.setConfigPath("av.xml");
        config.addModule(avConfig);

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        config.planCalcScore().getOrCreateScoringParameters(null).getOrCreateActivityParams(activity.getType());
                    }
                }
            }
        }
    }

    public void prepareActivities(Population population, ActivityFacilities facilities) {
        // Set link IDs for activities

        long numberOfActivities = 0;
        long numberOfFoundLinkIds = 0;

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement element : plan.getPlanElements()) {
                    if (element instanceof Activity) {
                        Activity activity = (Activity) element;
                        numberOfActivities++;

                        if (activity.getFacilityId() != null && facilities.getFacilities().containsKey(activity.getFacilityId())) {
                            Id<Link> linkId = facilities.getFacilities().get(activity.getFacilityId()).getLinkId();
                            activity.setLinkId(linkId);
                            numberOfFoundLinkIds++;
                        }

                        String activityType = activity.getType();

                        for (int i = 0; i < 20; i++) {
                            activityType = activityType.replace("_" + i, "");
                        }

                        activity.setType(activityType);
                    }
                }
            }
        }

        logger.info(String.format("Number of activities: %d", numberOfActivities));
        logger.info(String.format("Number of found link IDs: %d (%.2f%%)", numberOfFoundLinkIds, 100.0 * numberOfFoundLinkIds / numberOfActivities));
    }

    private Network buildReducedNetwork(Config config) {
        Network network = NetworkUtils.createNetwork(config.network());
        new MatsimNetworkReader(network).readFile(config.network().getInputFile());
        NetworkCutClean.elminateOutsideRadius(network, scenarioCenterCoord, avRadius);
        return network;
    }

    private Population buildReducedPopulation(Population original, Network reducedNetwork, Config config) {
        Population reduced = PopulationUtils.createPopulation(config);

        long numberOfPersons = 0;
        long numberOfReducedPersons = 0;

        for (Person originalPerson : original.getPersons().values()) {
            numberOfPersons++;

            if (original.getPersonAttributes().getAttribute(originalPerson.getId().toString(), "subpopulation") != null) {
                continue;
            }

            Person reducedPerson = reduced.getFactory().createPerson(originalPerson.getId());
            reducedPerson.addPlan(PlanUtils.createCopy(originalPerson.getSelectedPlan()));

            Iterator<PlanElement> elementIterator = reducedPerson.getSelectedPlan().getPlanElements().iterator();
            double currentActivityEndTime = 0.0;

            while (elementIterator.hasNext()) {
                PlanElement element = elementIterator.next();

                if (element instanceof Activity) {
                    Activity activity = (Activity) element;

                    //if (CoordUtils.calcEuclideanDistance(activityCoord, scenarioCenterCoord) >= avRadius) {
                    if (!reducedNetwork.getLinks().containsKey(activity.getLinkId())) {
                        elementIterator.remove();
                    } else {
                        currentActivityEndTime = activity.getEndTime();
                    }
                }

                if (element instanceof Leg) {
                    // Adapting to strange behaviour in PopulationRequestSchedule.java:74
                    ((Leg) element).setDepartureTime(currentActivityEndTime);
                }
            }

            // Also, PopulationRequestSchedule.java:74)
            Plan plan = reducedPerson.getSelectedPlan();
            while (plan.getPlanElements().size() > 0 && plan.getPlanElements().get(0) instanceof Leg) {
                plan.getPlanElements().remove(0);
            }

            while (plan.getPlanElements().size() > 0 && plan.getPlanElements().get(plan.getPlanElements().size() - 1) instanceof Leg) {
                plan.getPlanElements().remove(plan.getPlanElements().size() - 1);
            }

            reduced.addPerson(reducedPerson);
            numberOfReducedPersons++;
        }

        logger.info(String.format("Number of persons: %d", numberOfPersons));
        logger.info(String.format("Number of perssons in reduced population: %d (%.2f%%)", numberOfReducedPersons, 100.0 * numberOfReducedPersons / numberOfPersons));

        return reduced;
    }

    public Network createAndWriteVirtualNetwork(Config config, Population population, int numberOfVirtualNodes, int dtTravelData) throws Exception {
        Network reducedNetwork = buildReducedNetwork(config);
        Population reducedPopulation = buildReducedPopulation(population, reducedNetwork, config);

        KMEANSVirtualNetworkCreator kmeansVirtualNetworkCreator = new KMEANSVirtualNetworkCreator();
        VirtualNetwork virtualNetwork = kmeansVirtualNetworkCreator.createVirtualNetwork(reducedPopulation, reducedNetwork, numberOfVirtualNodes, true);

        final File virtualNetworkOutputDirectory = new File(outputDirectory, VIRTUALNETWORKFOLDERNAME);
        virtualNetworkOutputDirectory.mkdir();

        VirtualNetworkIO.toByte(new File(virtualNetworkOutputDirectory, VIRTUALNETWORKFILENAME), virtualNetwork);
        VirtualNetworkIO.toXML(new File(virtualNetworkOutputDirectory, VIRTUALNETWORKFILENAME + ".xml").toString(), virtualNetwork);

        //PopulationRequestSchedule prs = new PopulationRequestSchedule(reducedNetwork, reducedPopulation, virtualNetwork);
        //prs.exportCsv();

        //TravelData travelData = new TravelData(virtualNetwork, reducedNetwork, reducedPopulation, dtTravelData);
        //TravelDataIO.toByte(new File(virtualNetworkOutputDirectory, TRAVELDATAFILENAME), travelData);

        return reducedNetwork;
    }

    public void adjustInitialAVShare(Population population, double avShare) {
        // 3. Sets the alternative AV plan as selected for avShare % of the population
        Random random = new Random(0);

        long numberOfAgents = 0;
        long numberOfChangedAgents = 0;

        for (Person person : population.getPersons().values()) {
            if (population.getPersonAttributes().getAttribute(person.toString(), "subpopulation") == null) {
                Plan avPlan = null;
                Plan standardPlan = null;

                GlobalAssert.that(person.getPlans().size() == 2);

                for (Plan plan : person.getPlans()) {
                    if (isAVPlan(plan)) {
                        avPlan = plan;
                    } else {
                        standardPlan = plan;
                    }
                }

                GlobalAssert.that(avPlan != null);
                GlobalAssert.that(standardPlan != null);
                GlobalAssert.that(person.getSelectedPlan() == standardPlan);

                if (random.nextDouble() <= avShare) {
                    person.setSelectedPlan(avPlan);
                    numberOfChangedAgents++;
                }

                numberOfAgents++;
            }
        }

        logger.info(String.format("Number of agents: %d", numberOfAgents));
        logger.info(String.format("Number of agents with default AV plan: %d (%.2f%%)", numberOfChangedAgents, 100.0 * numberOfChangedAgents / numberOfAgents));
    }

    private List<Leg> getChangeablePTLegs(Plan plan, Network reducedNetwork) {
        LinkedList<Leg> returnLegs = new LinkedList<>();

        Activity previousActivity = null;
        Leg previousLeg = null;

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Leg) {
                previousLeg = (Leg) element;
            }

            if (element instanceof Activity) {
                Activity activity = (Activity) element;

                if (previousActivity != null && previousLeg != null) {
                    if (reducedNetwork.getLinks().containsKey(previousActivity.getLinkId()) && reducedNetwork.getLinks().containsKey(activity.getLinkId())) {
                        returnLegs.add(previousLeg);
                    }

                    previousLeg = null;
                }

                previousActivity = activity;
            }
        }

        return returnLegs;
    }

    public void applyAVPlans(Population population, Network reducedNetwork) {
        logger.info("Applying AV plans to agents ...");

        Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

        long numberOfAgents = 0;
        long numberOfChangedCarAgents = 0;
        long numberOfChangedPTAgents = 0;
        long numberOfPTLegs = 0;
        long numberOfChangedPTLegs = 0;
        long numberOfRemovedCarAgents = 0;
        long numberOfRemovedPTAgents = 0;

        while (personIterator.hasNext()) {
            numberOfAgents++;
            Person person = personIterator.next();

            if (population.getPersonAttributes().getAttribute(person.toString(), "subpopulation") == null) {
                Plan plan = person.getSelectedPlan();
                boolean isCarUser = findMainMode(plan).equals(TransportMode.car);

                boolean canBeConvertedToAV = true;

                Plan duplicate = PlanUtils.createCopy(plan);
                List<Leg> changeablePTLegs = null;

                if (isCarUser) {
                    for (PlanElement element : duplicate.getPlanElements()) {
                        if (element instanceof Activity) {
                            Activity activity = (Activity) element;

                            if (!reducedNetwork.getLinks().containsKey(activity.getLinkId())) {
                                canBeConvertedToAV = false;
                                numberOfRemovedCarAgents++;
                                break;
                            }
                        }
                    }
                } else {
                    changeablePTLegs = getChangeablePTLegs(duplicate, reducedNetwork);

                    if (changeablePTLegs.size() == 0) {
                        numberOfRemovedPTAgents++;
                        canBeConvertedToAV = false;
                    }
                }

                if (canBeConvertedToAV) {
                    if (isCarUser) {
                        for (PlanElement element : duplicate.getPlanElements()) {
                            if (element instanceof  Leg) {
                                ((Leg) element).setMode(AVModule.AV_MODE);
                            }
                        }
                    } else {
                        for (Leg leg : changeablePTLegs) {
                            leg.setMode(AVModule.AV_MODE);
                        }
                    }

                    person.addPlan(duplicate);

                    if (isCarUser) {
                        numberOfChangedCarAgents++;
                    } else {
                        numberOfChangedPTAgents++;
                    }
                } else {
                    personIterator.remove();
                }
            }
        }

        logger.info(String.format("Number of agents: %d", numberOfAgents));
        logger.info(String.format("Number of changed car agents: %d (%.2f%%)", numberOfChangedCarAgents, 100.0 * numberOfChangedCarAgents / numberOfAgents));
        logger.info(String.format("Number of changed pt agents: %d (%.2f%%)", numberOfChangedPTAgents, 100.0 * numberOfChangedPTAgents / numberOfAgents));
        logger.info(String.format("Number of PT legs: %d", numberOfPTLegs));
        logger.info(String.format("Number of changed PT legs: %d (%.2f%%)", numberOfChangedPTLegs, 100.0 * numberOfChangedPTLegs / numberOfPTLegs));
        logger.info(String.format("Number of removed car agents: %d (%.2f%%)", numberOfRemovedCarAgents, 100.0 * numberOfRemovedCarAgents / numberOfAgents));
        logger.info(String.format("Number of removed PT agents: %d (%.2f%%)", numberOfRemovedPTAgents, 100.0 * numberOfRemovedPTAgents / numberOfAgents));
    }

    public void preparePopulation(Population population) {
        logger.info("Removing special agents ...");
        // 0. In case it is a pre-routed population, remove all non-selected plans
        population.getPersons().values().stream().forEach(p -> PersonUtils.removeUnselectedPlans(p));

        // 1. Remove all agents which have walk or bike as the main mode
        // TODO: Remove also crossborder, freight and commuters???

        Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

        long numberOfRemovedAgents = 0;
        long numberOfAgents = 0;

        while (personIterator.hasNext()) {
            Person person = personIterator.next();
            String mainMode = findMainMode(person.getSelectedPlan());

            if (!allowedMainModes.contains(mainMode) || population.getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") != null) {
                personIterator.remove();
                numberOfRemovedAgents++;
            }

            numberOfAgents++;
        }

        logger.info(String.format("Number of agents: %d", numberOfAgents));
        logger.info(String.format("Number of removed agents: %d (%.2f%%)", numberOfRemovedAgents, 100.0 * numberOfRemovedAgents / numberOfAgents));
    }
}
