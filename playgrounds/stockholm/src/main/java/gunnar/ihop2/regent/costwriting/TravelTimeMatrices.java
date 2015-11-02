package gunnar.ihop2.regent.costwriting;

import static floetteroed.utilities.math.MathHelpers.drawWithoutReplacement;
import static org.matsim.matrices.MatrixUtils.divHadamard;
import static org.matsim.matrices.MatrixUtils.newAnonymousMatrix;
import floetteroed.utilities.Time;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

	// TODO encapsulate
	public final List<Matrix> ttMatrixList_min;

	// -------------------- CONSTRUCTION --------------------

	public TravelTimeMatrices(final Matrices matrices, final int startTime_s,
			final int binSize_s) {
		this.matrices = matrices;
		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.ttMatrixList_min = new ArrayList<Matrix>(this.matrices
				.getMatrices().size());
		for (Matrix matrix : this.matrices.getMatrices().values()) {
			this.ttMatrixList_min.add(matrix);
			System.out.println(matrix.getId());
		}
	}

	public TravelTimeMatrices(final Network network,
			final TravelTime linkTTs,
			final ZonalSystem zonalSystem, final Random rnd,
			final int startTime_s, final int binSize_s, final int binCnt,
			final int sampleCnt) {

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;

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

		this.matrices = new Matrices();
		this.ttMatrixList_min = new ArrayList<Matrix>(binCnt);

		for (int bin = 0; bin < binCnt; bin++) {

			final int time_s = startTime_s + bin * binSize_s + binSize_s / 2;
			final String timeString = Time.strFromSec(time_s, ':');

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Computing travel time matrix for departure time "
							+ timeString + ".");
			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Using " + sampleCnt + " random node(s) per zone.");

			final Matrix ttMatrix_min = this.matrices.createMatrix("TT_"
					+ timeString, "travel time in minutes");
			this.ttMatrixList_min.add(ttMatrix_min);
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
							linkTTs, network, fromNode, time_s,
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
		return this.ttMatrixList_min.size();
	}

	public void writeToFile(final String fileName) {
		final MatricesWriter writer = new MatricesWriter(this.matrices);
		writer.setIndentationString("  ");
		writer.setPrettyPrint(true);
		writer.write(fileName);
	}

	public void writeToScaperFiles(final String prefix) {
		for (int i = 0; i < this.ttMatrixList_min.size(); i++) {

			final Matrix matrix = this.ttMatrixList_min.get(i);
			final Matrices dummyMatrices = new Matrices();
			dummyMatrices.getMatrices().put(matrix.getId(), matrix);

			final MatricesWriter writer = new MatricesWriter(this.matrices);
			writer.setIndentationString("  ");
			writer.setPrettyPrint(true);
			writer.write(prefix + "_"
					+ Time.strFromSec(startTime_s + i * binSize_s, '-') + "_"
					+ Time.strFromSec(startTime_s + (i + 1) * binSize_s, '-')
					+ ".xml");
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
				scenario.getNetwork(), linkTTs,
				zonalSystem, new Random(), startTime_s, binSize_s, binCnt,
				sampleCnt);
		travelTimeMatrices
				.writeToFile("./test/matsim-testrun/traveltimes.xml");

		System.out.println("... DONE");
	}
}