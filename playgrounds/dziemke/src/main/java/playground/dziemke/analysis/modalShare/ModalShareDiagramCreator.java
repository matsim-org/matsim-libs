package playground.dziemke.analysis.modalShare;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
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

/**
 * @author gthunig on 21.03.2017.
 */
public class ModalShareDiagramCreator {

    public static final Logger log = Logger.getLogger(ModalShareDiagramCreator.class);

    private static final int DISTANCE_BIN_SIZE = 250; // metres
    private static final String RUN_ID = "be_117l";
    private static final String EVENTS_FILE = "../../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/" + RUN_ID + ".output_events.xml.gz";
//    private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network.xml.gz";
    private static final String NETWORK_FILE = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/network_shortIds.xml.gz";
    private static final String OUTPUT_DIR = "C:\\Users\\gthunig\\Desktop/";
    private static final String MODALSHARE_PATH = OUTPUT_DIR + "modal-share.txt";
    private static final String MODALSHARE_CUMULATIVE_PATH = OUTPUT_DIR + "modal-share_cumulative.txt";
    private static final String GNUPLOT_OUTPUT_PATH = OUTPUT_DIR;
    private static final String GNUPLOT_SCRIPT_NAME = "plot-modal-share.gnu";

    public static void main(String[] args) {


        ModalShareDiagramCreator creator = new ModalShareDiagramCreator();
        creator.createModalSplitDiagram(EVENTS_FILE, NETWORK_FILE, DISTANCE_BIN_SIZE);
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
        Network network = NetworkUtils.createNetwork();
        MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
        networkReader.readFile(networkFile);

        List<Trip> trips = new ArrayList<>(tripHandler.getTrips().values());

        ModalShareDistanceBinContainer container = new ModalShareDistanceBinContainer(distanceBinSize);

        for (Trip trip : trips) {

            switch (trip.getMode()) {
                case "car":
                    container.enterDistance((int)trip.getDistanceRoutedByCalculation_m(network), Mode.CAR);
                    break;
                case "pt":
                    container.enterDistance((int)trip.getDistanceBeelineByCalculation_m(network), Mode.PT);
                    break;
                case "slowPt":
                    container.enterDistance((int)trip.getDistanceBeelineByCalculation_m(network), Mode.SLOW_PT);
                    break;
                default:
                    log.error("Unknown legMode: " + trip.getMode());
                    break;
            }
        }

        writeModalShare(container, MODALSHARE_PATH, Mode.CAR, Mode.PT, Mode.SLOW_PT);
        writeModalShareCummulative(container, MODALSHARE_CUMULATIVE_PATH, Mode.PT, Mode.SLOW_PT, Mode.CAR);

        String relativePathToGnuplotScript = "../VSP/shared-svn/projects/cemdapMatsimCadyts/analysis/gnuplot/" + GNUPLOT_SCRIPT_NAME;

        int factor = 1000 / DISTANCE_BIN_SIZE;
        int xRange = 30; // has to be divisible by 10
        GnuplotUtils.runGnuplotScript(GNUPLOT_OUTPUT_PATH, relativePathToGnuplotScript, String.valueOf(factor), String.valueOf(xRange));
    }

    private void writeModalShare(ModalShareDistanceBinContainer container, String path, Mode... modes) {
        CSVWriter writer = new CSVWriter(path, "\t");

        String line = "binNumberns";
        for (Mode mode : modes)
            line += "\t" + mode.toString();
        writer.writeLine(line);
        for (DistanceBin bin : container.getDistanceBins()) {
            writer.writeField(String.valueOf(bin.getBinNumer()));

            int[] values = new int[modes.length];
            double[] percentages = new double[modes.length];
            double denominator = 0;
            for (int i = 0; i < modes.length; i++) {
                Mode mode = modes[i];
                values[i] = ModalShareDistanceBinContainer.getModeValue(bin, mode);
                denominator += values[i];
            }
            if (denominator > 0) {
                for (int i = 0; i < modes.length; i++) {
                    percentages[i] = (values[i]/denominator)*100;
                }
            } else {
                for (int i = 0; i < modes.length; i++) {
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

    private void writeModalShareCummulative(ModalShareDistanceBinContainer container, String path, Mode... modes) {
        CSVWriter writer = new CSVWriter(path, "\t");

        String line = "binNumbers";
        for (Mode mode : modes)
            line += "\t" + mode.toString();
        writer.writeLine(line);
        for (DistanceBin bin : container.getDistanceBins()) {
            writer.writeField(String.valueOf(bin.getBinNumer()));

            int[] values = new int[modes.length];
            double[] percentages = new double[modes.length];
            double denominator = 0;
            for (int i = 0; i < modes.length; i++) {
                Mode mode = modes[i];
                values[i] = ModalShareDistanceBinContainer.getModeValue(bin, mode);
                denominator += values[i];
            }
            if (denominator > 0) {
                for (int i = 0; i < modes.length; i++) {
                    percentages[i] = (values[i]/denominator)*100;
                }
            } else {
                for (int i = 0; i < modes.length; i++) {
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
