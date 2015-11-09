package gunnar.ihop2.regent.costwriting;

import static floetteroed.utilities.math.MathHelpers.drawWithoutReplacement;
import static org.matsim.matrices.MatrixUtils.divHadamard;
import static org.matsim.matrices.MatrixUtils.newAnonymousMatrix;
import floetteroed.utilities.Time;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;
import gunnar.ihop2.utils.LexicographicallyOrderedPositiveNumberStrings;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import saleem.stockholmscenario.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TravelTimeMatrices {

	// -------------------- MEMBERS --------------------

	private final int startTime_s;

	private final int binSize_s;

	private final Matrices matrices;

	private final LexicographicallyOrderedPositiveNumberStrings numberStrings;

	// -------------------- CONSTRUCTION --------------------

	public TravelTimeMatrices(final Matrices matrices, final int startTime_s,
			final int binSize_s) {
		this.matrices = matrices;
		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.numberStrings = new LexicographicallyOrderedPositiveNumberStrings(
				matrices.getMatrices().size() - 1);
	}

	public TravelTimeMatrices(final Network network, final TravelTime linkTTs,
			final ZonalSystem zonalSystem, final Random rnd,
			final int startTime_s, final int binSize_s, final int binCnt,
			final int sampleCnt) {

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.numberStrings = new LexicographicallyOrderedPositiveNumberStrings(
				binCnt - 1);

		/*
		 * Identify all zones that are relevant and contain at least one node.
		 */

		final Map<Zone, Set<Node>> zone2sampledNodes = new LinkedHashMap<>();
		for (Zone zone : zonalSystem) {
			final Set<Node> nodes = drawWithoutReplacement(sampleCnt,
					zonalSystem.getNodes(zone), rnd);
			if ((nodes != null) && (nodes.size() > 0)) {
				zone2sampledNodes.put(zone, nodes);
			}
		}

		/*
		 * Create one travel time matrix per time bin.
		 */

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Using " + sampleCnt + " random node(s) per zone.");

		this.matrices = new Matrices();

		for (int bin = 0; bin < binCnt; bin++) {

			final String timeIntervalString = "["
					+ Time.strFromSec(startTime_s + bin * binSize_s, ':') + ","
					+ Time.strFromSec(startTime_s + (bin + 1) * binSize_s, ':')
					+ ")";

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Computing travel time matrix for time interval "
							+ timeIntervalString + ".");

			final Matrix ttMatrix_min = this.matrices.createMatrix(
					this.numberStrings.toString(bin),
					"travel time in minutes when starting in the middle of time interval "
							+ timeIntervalString);
			final Matrix cntMatrix = newAnonymousMatrix();

			final int threadCnt = Runtime.getRuntime().availableProcessors();
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Running " + threadCnt + " threads.");
			final ExecutorService threadPool = Executors
					.newFixedThreadPool(threadCnt);

			// go through all origin zones
			for (Zone fromZone : zone2sampledNodes.keySet()) {
				// go through a sample of origin nodes
				for (Node fromNode : zone2sampledNodes.get(fromZone)) {
					final LeastCostMatrixUpdater matrixUpdater = new LeastCostMatrixUpdater(
							linkTTs, network, fromNode, startTime_s + bin
									* binSize_s + binSize_s / 2,
							zone2sampledNodes, ttMatrix_min, cntMatrix,
							fromZone.getId());
					threadPool.execute(matrixUpdater);
				}
			}

			threadPool.shutdown();
			try {
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new RuntimeException();
			}

			divHadamard(ttMatrix_min, cntMatrix);
		}
	}

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.matrices.getMatrices().size();
	}

	public Matrix getMatrix_min(final int bin) {
		return this.matrices.getMatrix(this.numberStrings.toString(bin));
	}

	public void writeToFile(final String fileName) {
		final MatricesWriter writer = new MatricesWriter(this.matrices);
		writer.setIndentationString("  ");
		writer.setPrettyPrint(true);
		writer.write(fileName);
	}

	public void writeToScaperFiles(final String prefix) {

		for (int bin = 0; bin < this.getBinCnt(); bin++) {
			final String binString = this.numberStrings.toString(bin);

			final Matrix matrix = this.matrices.getMatrix(binString);
			final Matrices dummyMatrices = new Matrices();
			dummyMatrices.getMatrices().put(matrix.getId(), matrix);

			final MatricesWriter writer = new MatricesWriter(this.matrices);
			writer.setIndentationString("  ");
			writer.setPrettyPrint(true);
			writer.write(prefix + this.numberStrings.toString(bin) + ".xml");
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String networkFileName = "./test/matsim-testrun/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final String eventsFileName = "./test/matsim-testrun/matsim-output/ITERS/it.0/0.events.xml.gz";

		final int startTime_s = 6 * 3600;
		final int binSize_s = 3600;
		final int binCnt = 16;
		final int sampleCnt = 1;

		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), 15 * 60, 24 * 3600 - 1, scenario
						.getConfig().travelTimeCalculator());
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ttcalc);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);
		final TravelTime linkTTs = ttcalc.getLinkTravelTimes();

		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				scenario.getNetwork(), linkTTs, zonalSystem, new Random(),
				startTime_s, binSize_s, binCnt, sampleCnt);
		travelTimeMatrices.writeToFile("./test/matsim-testrun/traveltimes.xml");

		System.out.println("... DONE");
	}
}