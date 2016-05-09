package playground.gthunig.VBB2OTFVis;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.Route;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.pt.utils.CreateVehiclesForSchedule;

import java.io.File;
import java.time.LocalDate;

/**
 * @author gthunig
 * on 18.04.16.
 */
public class RunVBB2OTFVis {

    private static final Logger log = Logger.getLogger(RunVBB2OTFVis.class);

    public static void main(String[] args) {

        String gtfsPath = "playgrounds/vspshk/input/gthunig/VBB2OTFVis/380248.zip";
        String outputRoot = "playgrounds/vspshk/output/gthunig/VBB2OTFVis/";

//        String gtfsPath = "/Users/michaelzilske/wurst/vbb/380248.zip";
//        String outputRoot = "output";

        File outputFile = new File(outputRoot);
        if (outputFile.mkdir()) {
            log.info("Did not found output root at " + outputRoot + " Created it as a new directory.");
        }

        log.info("Parsing GTFSFeed from file...");
        final GTFSFeed feed = GTFSFeed.fromFile(gtfsPath);

        log.info("Parsed trips: "+feed.trips.size());
        log.info("Parsed routes: "+feed.routes.size());
        log.info("Parsed stops: "+feed.stops.size());

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

        OTFVis.playScenario(scenario);

    }

}
