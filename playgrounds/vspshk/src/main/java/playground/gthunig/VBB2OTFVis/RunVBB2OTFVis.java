package playground.gthunig.VBB2OTFVis;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Route;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import playground.gthunig.utils.StopWatch;

import javax.inject.Provider;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author gthunig
 * on 18.04.16.
 */
public class RunVBB2OTFVis {

    private static final Logger log = Logger.getLogger(RunVBB2OTFVis.class);

    public static void main(String[] args) {

//        String gtfsPath = "playgrounds/vspshk/input/gthunig/VBB2OTFVis/380248.zip";
//        String outputRoot = "playgrounds/vspshk/output/gthunig/VBB2OTFVis/";

        String gtfsPath = "/Users/michaelzilske/wurst/vbb/380248.zip";
        String outputRoot = "output";

        File outputFile = new File(outputRoot);
        if (outputFile.mkdir()) {
            log.info("Did not found output root at " + outputRoot + " Created it as a new directory.");
        }

        log.info("Parsing GTFSFeed from file...");
        final GTFSFeed feed = GTFSFeed.fromFile(gtfsPath);

        log.info("Parsed trips: " + feed.trips.size());
        log.info("Parsed routes: " + feed.routes.size());
        log.info("Parsed stops: " + feed.stops.size());

        for (Route route : feed.routes.values()) {
            switch (route.route_type) {
                case 700://bus
                    route.route_type = 3;
                    break;
                case 900://tram
                    route.route_type = 0;
                    break;
                case 109://s-bahn
                    route.route_type = 1;
                    break;
                case 100://rail
                    route.route_type = 2;
                    break;
                case 400://u-bahn
                    route.route_type = 1;
                    break;
                case 1000://ferry
                    route.route_type = 4;
                    break;
                case 102://strange railway
                    route.route_type = 2;
                    break;
                default:
                    log.warn("Unknown 'wrong' route_type. Value: " + route.route_type + "\nPlease add exception.");
                    break;
            }
        }

        Config config = ConfigUtils.createConfig();
        config.global().setRandomSeed(666);
        config.global().setNumberOfThreads(8);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.
                OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);

        CoordinateTransformation coordinateTransformation = TransformationFactory.
                getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");

        GtfsConverter converter = new GtfsConverter(feed, scenario, coordinateTransformation);
        converter.setDate(LocalDate.of(2016, 5, 16));
        converter.convert();

        CreatePseudoNetwork createPseudoNetwork =
                new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "");
        createPseudoNetwork.createNetwork();

        new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();

        log.info("Playing VBB scenario with OTFVis...");

        Population population = scenario.getPopulation();
        Network network = scenario.getNetwork();
        List<Link> links = new ArrayList<>(network.getLinks().values());

        for (int i = 0; i < 1000; ++i) {

            Coord source = links.get(MatsimRandom.getRandom().nextInt(network.getLinks().size())).getCoord();
            Coord sink = links.get(MatsimRandom.getRandom().nextInt(network.getLinks().size())).getCoord();

            Person person = population.getFactory().createPerson(Id.create(Integer.toString(i), Person.class));
            Plan plan = population.getFactory().createPlan();
            plan.addActivity(createHome(source, population));
            List<Leg> homeWork = createLeg(population);
            for (Leg leg : homeWork) {
                plan.addLeg(leg);
            }
            plan.addActivity(createWork(sink, population));
            List<Leg> workHome = createLeg(population);
            for (Leg leg : workHome) {
                plan.addLeg(leg);
            }
            plan.addActivity(createHome(source, population));
            person.addPlan(plan);
            population.addPerson(person);
        }

        Provider<TripRouter> trf = TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(scenario);

        StopWatch stopWatch = new StopWatch();

        ParallelPersonAlgorithmUtils.run(population, config.global().getNumberOfThreads(),
                () -> new PersonPrepareForSim(new PlanRouter(trf.get(), scenario.getActivityFacilities()), scenario));

        System.out.println(stopWatch.getElapsedTime());

        OTFVis.playScenario(scenario);


    }

    private static Activity createWork(Coord workLocation, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
        activity.setEndTime(17*60*60);
        return activity;
    }

    private static  Activity createHome(Coord homeLocation, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
        activity.setEndTime(9*60*60);
        return activity;
    }

    private static List<Leg> createLeg(Population population) {
        Leg leg = population.getFactory().createLeg(TransportMode.pt);
        return Arrays.asList(new Leg[]{leg});
    }

}
