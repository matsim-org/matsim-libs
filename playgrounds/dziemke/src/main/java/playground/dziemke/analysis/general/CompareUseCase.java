package playground.dziemke.analysis.general;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import playground.dziemke.analysis.GnuplotUtils;
import playground.dziemke.analysis.general.matsim.Events2TripsParser;
import playground.dziemke.analysis.general.matsim.FromMatsimTrip;
import playground.dziemke.analysis.general.matsim.FromMatsimTripFilterImpl;
import playground.dziemke.analysis.general.srv.FromSrvTrip;
import playground.dziemke.analysis.general.srv.FromSrvTripFilterImpl;
import playground.dziemke.analysis.general.srv.Srv2MATSimPopulation;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author gthunig on 24.04.2017.
 */
public class CompareUseCase {
    public static final Logger log = Logger.getLogger(CompareUseCase.class);

    //FromMatsim Parameters
        private static final String RUN_ID = "be_118"; // <----------
        private static final String ITERATION_FOR_ANALYSIS = "";
        private static final String CEMDAP_PERSONS_INPUT_FILE_ID = "21"; // Check if this number corresponds correctly to the RUN_ID

        // Input and output
//        private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network.xml.gz"; // <----------
        private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds_v1.xml.gz"; // <----------
        private static final String EVENTS_FILE = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/" + RUN_ID + ".output_events.xml.gz";
//        private static final String EVENTS_FILE = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/ITERS/it." + ITERATION_FOR_ANALYSIS + "/" + RUN_ID + "." + ITERATION_FOR_ANALYSIS + ".events.xml.gz";
        private static final String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + CEMDAP_PERSONS_INPUT_FILE_ID + "/persons1.dat"; // TODO
        private static final String AREA_SHAPE_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
        //    private static String outputDirectory = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis";
        // private static String fromMatsimOutputDirectory = "/Users/dominik/test-analysis";
        private static String fromMatsimOutputDirectory = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis_run";

    //FromSrv Parameters
        private static final String SRV_BASE_DIR = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/input/";
        private static final String SRV_PERSON_FILE_PATH = SRV_BASE_DIR + "P2008_Berlin2.dat";
        private static final String SRV_TRIP_FILE_PATH = SRV_BASE_DIR + "W2008_Berlin_Weekday.dat";
        private static final String OUTPUT_POPULATION_FILE_PATH = SRV_BASE_DIR + "testOutputPopulation.xml";
//        private static String fromSrvOutputDirectory = "/Users/dominik/test-analysis";
//        private static String fromSrvOutputDirectory = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/analysis_srv";
        private static String fromSrvOutputDirectory = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/srv/output/";


    public static void main(String[] args) {

        Events2TripsParser events2TripsParser = new Events2TripsParser(EVENTS_FILE, NETWORK_FILE);
        List<FromMatsimTrip> fromMatsimTrips = events2TripsParser.getTrips();

        //TODO highlight tripfilter?
        FromMatsimTripFilterImpl fromMatsimTripFilter = new FromMatsimTripFilterImpl();
        fromMatsimTripFilter.activateModeChoice(TransportMode.car);
        fromMatsimTripFilter.activateStartsOrEndsIn(events2TripsParser.getNetwork(), AREA_SHAPE_FILE, 11000000);
        fromMatsimTripFilter.activateDist(0, 100);
        fromMatsimTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);
        List<Trip> filteredFromMatsimTrips = TripFilter.castTrips(fromMatsimTripFilter.filter(fromMatsimTrips));

        //determine output directory
        fromMatsimOutputDirectory = fromMatsimOutputDirectory + "_" + ITERATION_FOR_ANALYSIS;
        fromMatsimOutputDirectory = fromMatsimTripFilter.adaptOutputDirectory(fromMatsimOutputDirectory);
        new File(fromMatsimOutputDirectory).mkdirs();

        //write output
        GeneralTripAnalyzer.analyze(filteredFromMatsimTrips, events2TripsParser.getNoPreviousEndOfActivityCounter(), fromMatsimOutputDirectory);



        Srv2MATSimPopulation srv2MATSimPopulation = new Srv2MATSimPopulation(SRV_PERSON_FILE_PATH, SRV_TRIP_FILE_PATH);
        srv2MATSimPopulation.writePopulation(OUTPUT_POPULATION_FILE_PATH);

        List<FromSrvTrip> fromSrvTrips = srv2MATSimPopulation.getTrips();

        //TODO highlight tripfilter?
        FromSrvTripFilterImpl fromSrvTripFilter = new FromSrvTripFilterImpl();
        fromSrvTripFilter.activateModeChoice(TransportMode.car);
        fromSrvTripFilter.activateDist(0, 100);
        fromSrvTripFilter.activateDepartureTimeRange(16. * 3600, 22. * 3600);
        List<Trip> filteredFromSrvTrips = TripFilter.castTrips(fromSrvTripFilter.filter(fromSrvTrips));

        //determine output directory
        String srvOutputDirectory = fromSrvTripFilter.adaptOutputDirectory("analysis_srv");
        fromSrvOutputDirectory += srvOutputDirectory;
        new File(fromSrvOutputDirectory).mkdirs();

        //write output
        GeneralTripAnalyzer.analyze(filteredFromSrvTrips, fromSrvOutputDirectory);

        //Gnuplot
        String gnuplotScriptName = "plot_rel_path_run.gnu";
        String relativePathToGnuplotScript = "../../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/gnuplot/" + gnuplotScriptName;
        File file = new File("../../../shared-svn/studies/countries/de/berlin_scenario_2016/analysis/gnuplot/");
        GnuplotUtils.runGnuplotScript(fromMatsimOutputDirectory, relativePathToGnuplotScript,srvOutputDirectory);
    }
}