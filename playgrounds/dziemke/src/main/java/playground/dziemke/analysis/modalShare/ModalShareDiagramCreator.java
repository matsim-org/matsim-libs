package playground.dziemke.analysis.modalShare;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import playground.dziemke.analysis.GnuplotUtils;
import playground.dziemke.analysis.Trip;
import playground.dziemke.analysis.TripHandler;
import playground.dziemke.analysis.modalShare.ModalShareDistanceBinContainer.Mode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gthunig on 21.03.2017.
 */
public class ModalShareDiagramCreator {

    public static final Logger log = Logger.getLogger(ModalShareDiagramCreator.class);

    private static final int DISTANCE_BIN_SIZE = 250; // metres
    private static final String RUN_ID = "be_117l";
    private static final String RUN_DIR = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/";
    private static final String EVENTS_FILE = RUN_DIR + RUN_ID + ".output_events.xml.gz";
//    private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network.xml.gz";
    private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds.xml.gz";
    private static final String OUTPUT_DIR = RUN_DIR + "modalShare/";
    private static final String GNUPLOT_SCRIPT_NAME = "plot-modal-share.gnu";
    private static final String RELATIVE_PATH_TO_GNUPLOT_SCRIPT = "../../../../shared-svn/projects/cemdapMatsimCadyts/analysis/gnuplot/" + GNUPLOT_SCRIPT_NAME;
    // remember: this path leads relatively from your OutputDir(GNUPLOT_OUTPUT_PATH) to the gnuplot script file(GNUPLOT_SCRIPT_NAME)
    private static final String MODALSHARE_PATH = OUTPUT_DIR + "modal-share.txt";
    private static final String MODALSHARE_CUMULATIVE_PATH = OUTPUT_DIR + "modal-share_cumulative.txt";
    private static final String GNUPLOT_OUTPUT_PATH = OUTPUT_DIR;

    private Network network;
    private ModalShareDistanceBinContainer container;
    private List<ModeTuple> consideredModes = new ArrayList<>();

    private class ModeTuple{
        public final Mode mode;
        public final String identifier;

        ModeTuple(Mode mode, String identifier) {
            this.mode = mode;
            this.identifier = identifier;
        }
    }

    public static void main(String[] args) {


        ModalShareDiagramCreator creator = new ModalShareDiagramCreator();
        creator.addMode(Mode.CAR, TransportMode.car);
        creator.addMode(Mode.PT, TransportMode.pt);
//        creator.addMode(Mode.PT_SLOW, "ptSlow");
        creator.addMode(Mode.PT_SLOW, "slowPt");
        creator.createModalSplitDiagram(EVENTS_FILE, NETWORK_FILE, DISTANCE_BIN_SIZE);
    }

    private void addMode(Mode mode, String identifier) {
        consideredModes.add(new ModeTuple(mode, identifier));
    }

    private void createModalSplitDiagram(String eventsFile, String networkFile, int distanceBinSize) {

        /* Events infrastructure and reading the events file */
        EventsManager eventsManager = EventsUtils.createEventsManager();
        TripHandler tripHandler = new TripHandler();
        eventsManager.addHandler(tripHandler);
        MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
        eventsReader.readFile(eventsFile);
        log.info("Events file read!");

        /* Get network, which is needed to calculate distances */
        network = NetworkUtils.createNetwork();
        MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
        networkReader.readFile(networkFile);

        List<Trip> trips = new ArrayList<>(tripHandler.getTrips().values());

        container = new ModalShareDistanceBinContainer(distanceBinSize);

        for (Trip trip : trips) {

            boolean distanceEntered = false;
            for (ModeTuple modeTuple : consideredModes)
                distanceEntered = checkModeAndEnterDistance(trip, modeTuple) || distanceEntered;
            if (!distanceEntered)
                log.error("Unknown legMode: " + trip.getMode());

        }

        makeDir(OUTPUT_DIR);
        writeModalShare(container, MODALSHARE_PATH);
        writeModalShareCummulative(container, MODALSHARE_CUMULATIVE_PATH);

        int factor = 250 / DISTANCE_BIN_SIZE;
        int xRange = 30; // has to be divisible by 10
        String modes = "";
        for (ModeTuple modeTuple : consideredModes)
            modes += modeTuple.identifier + "\t";
        GnuplotUtils.runGnuplotScript(GNUPLOT_OUTPUT_PATH, RELATIVE_PATH_TO_GNUPLOT_SCRIPT, String.valueOf(factor), String.valueOf(xRange), modes);
    }

    private boolean checkModeAndEnterDistance(Trip trip, ModeTuple modeTuple) {
        if (trip.getMode().equals(modeTuple.identifier)) {
            container.enterDistance((int)trip.getDistanceBeelineByCalculation_m(network), modeTuple.mode);
            return true;
        } else
            return false;
    }

    private boolean makeDir(String dir) {
        File file = new File(dir);
        return file.mkdir();
    }

    private void writeModalShare(ModalShareDistanceBinContainer container, String path) {
        CSVWriter writer = new CSVWriter(path, "\t");

        String line = "binNumbers";
        for (ModeTuple mode : consideredModes)
            line += "\t" + mode.identifier;
        writer.writeLine(line);
        for (DistanceBin bin : container.getDistanceBins()) {
            writer.writeField(String.valueOf(bin.getBinNumer()));

            int[] values = new int[consideredModes.size()];
            double[] percentages = new double[consideredModes.size()];
            double denominator = 0;
            for (int i = 0; i < consideredModes.size(); i++) {
                Mode mode = consideredModes.get(i).mode;
                values[i] = ModalShareDistanceBinContainer.getModeValue(bin, mode);
                denominator += values[i];
            }
            if (denominator > 0) {
                for (int i = 0; i < consideredModes.size(); i++) {
                    percentages[i] = (values[i]/denominator)*100;
                }
            } else {
                for (int i = 0; i < consideredModes.size(); i++) {
                    percentages[i] = 0;
                }
            }
            for (double percentage : percentages) {
                writer.writeField(String.valueOf(percentage));
            }
            writer.writeNewLine();
        }
        writer.close();
    }

    private void writeModalShareCummulative(ModalShareDistanceBinContainer container, String path) {
        CSVWriter writer = new CSVWriter(path, "\t");

        String line = "binNumbers";
        List<ModeTuple> reverseConsideredModes = Lists.reverse(consideredModes);
        for (ModeTuple mode : reverseConsideredModes)
            line += "\t" + mode.identifier;
        writer.writeLine(line);
        for (DistanceBin bin : container.getDistanceBins()) {
            writer.writeField(String.valueOf(bin.getBinNumer()));

            int[] values = new int[reverseConsideredModes.size()];
            double[] percentages = new double[reverseConsideredModes.size()];
            double denominator = 0;
            for (int i = 0; i < reverseConsideredModes.size(); i++) {
                Mode mode = reverseConsideredModes.get(i).mode;
                values[i] = ModalShareDistanceBinContainer.getModeValue(bin, mode);
                denominator += values[i];
            }
            if (denominator > 0) {
                for (int i = 0; i < reverseConsideredModes.size(); i++) {
                    percentages[i] = (values[i]/denominator)*100;
                }
            } else {
                for (int i = 0; i < reverseConsideredModes.size(); i++) {
                    percentages[i] = 0;
                }
            }
            for (int i = 0; i < percentages.length; i++) {
                double cumulativeValue = 0;
                for (int e = 0; e <= i; e++)
                    cumulativeValue += percentages[e];
                writer.writeField(String.valueOf(cumulativeValue));
            }
            writer.writeNewLine();
        }
        writer.close();
    }

}
