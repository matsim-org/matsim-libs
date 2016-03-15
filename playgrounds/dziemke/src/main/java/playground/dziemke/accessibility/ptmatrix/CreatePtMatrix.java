package playground.dziemke.accessibility.ptmatrix;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.conveyal.gtfs.GTFSFeed;

/**
 * @author gthunig on 25.02.16.
 */
public class CreatePtMatrix {

    private static final Logger log = Logger.getLogger(CreatePtMatrix.class);

    public static void main(String[] args) {
    	final long timeStart = System.currentTimeMillis();

    	// String gtfsPath = "playgrounds/dziemke/input/sample-feed/sample-feed.zip";
    	// String coordinateConversionSystem = "EPSG:26911";
    	String gtfsPath = "playgrounds/dziemke/input/353413.zip";
    	String coordinateConversionSystem = "EPSG:25832";
    	// String networkFile = "examples/pt-tutorial/multimodalnetwork.xml";
    	String outputRoot = "playgrounds/dziemke/output/";
    	File outputFile = new File(outputRoot);
    	if (outputFile.mkdir()) {
    		log.info("Did not found output root at " + outputRoot + " Created it as a new directory.");
    	}

    	double departureTime = 8 * 60 * 60;

    	Config config = ConfigUtils.createConfig();
    	config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);

        CoordinateTransformation ct =
                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,"EPSG:25832");
        GtfsConverter converter = new GtfsConverter(GTFSFeed.fromFile(gtfsPath), scenario, ct );
        converter.setDate(LocalDate.of(2015,10,8));
//        GtfsConverter converter = new GtfsConverter(gtfsPath, scenario, ct );
//        converter.setDate(20151008);
        converter.convert();

        Map<Id<Coord>, Coord> ptMatrixLocationsMap = new HashMap<>();

        for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
            if (transitStopFacility.getName().contains("(Berlin)")) {
                Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
                Coord coord = transitStopFacility.getCoord();
                ptMatrixLocationsMap.put(id, coord);
            }
        }

//        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
//        networkReader.readFile(networkFile);

        MatrixBasedPtInputUtils.createStopsFile(ptMatrixLocationsMap, outputRoot + "ptStops.csv", ",");

        // The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
        // In other uses the two maps may be different -- thus the duplication here.
        log.info("Start matrix-computation...");
        ThreadedMatrixCreator tmc = new ThreadedMatrixCreator(scenario, ptMatrixLocationsMap,
                ptMatrixLocationsMap, departureTime, outputRoot, " ", 1);

        //waiting for the output to be written
        try {
            tmc.getThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final long timeEnd = System.currentTimeMillis();
        System.out.println("Verlaufszeit der Schleife: " + (timeEnd - timeStart) + " Millisek.");
    }
}