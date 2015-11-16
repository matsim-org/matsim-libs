package gunnar.ihop2.regent.costwriting;

import static floetteroed.utilities.math.MathHelpers.drawWithoutReplacement;
import floetteroed.utilities.Time;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;
import gunnar.ihop2.utils.LexicographicallyOrderedPositiveNumberStrings;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatrixUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TripCostMatrices {

	// -------------------- MEMBERS --------------------

	private final int startTime_s;

	private final int binSize_s;

	private final Map<String, Matrices> costType2matrices;

	private final LexicographicallyOrderedPositiveNumberStrings numberStrings;

	// -------------------- CONSTRUCTION --------------------

	// public TripCostMatrices(final Map<String, Matrices> costType2matrices,
	// final int startTime_s, final int binSize_s) {
	// this.costType2matrices = costType2matrices;
	// this.startTime_s = startTime_s;
	// this.binSize_s = binSize_s;
	// this.numberStrings = new LexicographicallyOrderedPositiveNumberStrings(
	// costType2matrices.values().iterator().next().getMatrices()
	// .size() - 1);
	// }

	public TripCostMatrices(final TravelTime linkTTs,
			final TravelDisutility linkCostsForTreeCalculation,
			final Network network, final ZonalSystem zonalSystem,
			final int startTime_s, final int binSize_s, final int binCnt,
			final Random rnd, final int sampleCnt,
			final Map<String, TravelDisutility> linkCostsForMatrixCalculation) {

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.numberStrings = new LexicographicallyOrderedPositiveNumberStrings(
				binCnt - 1);

		final int threadCnt = Runtime.getRuntime().availableProcessors();
		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Using " + threadCnt + " threads.");

		/*
		 * Identify all zones that are relevant and contain at least one node.
		 */

		Logger.getLogger(MATSimDummy.class.getName()).info(
				"Using " + sampleCnt + " random node(s) per zone.");

		final Map<Zone, Set<Node>> zone2sampledNodes = new LinkedHashMap<>();
		for (Zone zone : zonalSystem) {
			final Set<Node> nodes = drawWithoutReplacement(sampleCnt,
					zonalSystem.getNodes(zone), rnd);
			if ((nodes != null) && (nodes.size() > 0)) {
				zone2sampledNodes.put(zone, nodes);
			}
		}

		/*
		 * Create one travel time matrix per time bin and per cost type.
		 */

		this.costType2matrices = new LinkedHashMap<>();
		for (String costType : linkCostsForMatrixCalculation.keySet()) {
			this.costType2matrices.put(costType, new Matrices());
		}

		for (int bin = 0; bin < binCnt; bin++) {

			final String timeIntervalString = "["
					+ Time.strFromSec(startTime_s + bin * binSize_s, ':') + ","
					+ Time.strFromSec(startTime_s + (bin + 1) * binSize_s, ':')
					+ ")";

			Logger.getLogger(MATSimDummy.class.getName()).info(
					"Computing cost matrices for time interval "
							+ timeIntervalString + ".");

			final Map<String, Matrix> costType2currentMatrix = new LinkedHashMap<>();
			{
				for (Map.Entry<String, Matrices> costType2matricesEntry : this.costType2matrices
						.entrySet()) {
					final String costType = costType2matricesEntry.getKey();
					final Matrix currentMatrix = costType2matricesEntry
							.getValue().createMatrix(
									this.numberStrings.toString(bin),
									costType + " when starting in "
											+ "the middle of time interval "
											+ timeIntervalString);
					costType2currentMatrix.put(costType, currentMatrix);
				}
			}

			final ExecutorService threadPool = Executors
					.newFixedThreadPool(threadCnt);

			// go through all origin zones
			for (Zone fromZone : zone2sampledNodes.keySet()) {
				// go through a sample of origin nodes
				for (Node fromNode : zone2sampledNodes.get(fromZone)) {
					final LeastCostMatrixUpdater matrixUpdater = new LeastCostMatrixUpdater(
							linkTTs, linkCostsForTreeCalculation, network,
							fromNode, fromZone.getId(), zone2sampledNodes,
							startTime_s + bin * binSize_s + binSize_s / 2,
							linkCostsForMatrixCalculation,
							costType2currentMatrix);
					threadPool.execute(matrixUpdater);
				}
			}

			threadPool.shutdown();
			try {
				threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new RuntimeException();
			}
		}

		/*
		 * All matrices now contain the *sum* of costs over all from/to-node
		 * combinations per OD pair and hence need to be normalized.
		 * 
		 * TODO This could probably be parallelized.
		 */

		final Matrix cntMatrix = (new MatrixUtils()).newAnonymousMatrix();
		for (Map.Entry<Zone, Set<Node>> fromZone2sampledNodesEntry : zone2sampledNodes
				.entrySet()) {
			for (Zone toZone : zone2sampledNodes.keySet()) {
				cntMatrix.createEntry(fromZone2sampledNodesEntry.getKey()
						.getId(), toZone.getId(), fromZone2sampledNodesEntry
						.getValue().size()
						* zone2sampledNodes.get(toZone).size());
			}
		}
		for (Matrices matrices : costType2matrices.values()) {
			for (Matrix matrix : matrices.getMatrices().values()) {
				(new MatrixUtils()).divHadamard(matrix, cntMatrix);
			}
		}
	}

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.costType2matrices.values().iterator().next().getMatrices()
				.size();
	}

	public Set<String> getCostTypes() {
		return Collections.unmodifiableSet(this.costType2matrices.keySet());
	}

	public Matrix getMatrix(final String costType, final int bin) {
		return this.costType2matrices.get(costType).getMatrix(
				this.numberStrings.toString(bin));
	}

	public void writeSummaryToFile(final String summaryFileName) {
		try {
			Logger.getLogger(this.getClass().getName()).info(
					"writing travel cost summary file " + summaryFileName);
			final PrintWriter writer = new PrintWriter(summaryFileName);

			writer.print("TIMEBIN");
			for (String costType : this.costType2matrices.keySet()) {
				writer.print("\t");
				writer.print("MIN(" + costType + ")");
				writer.print("\t");
				writer.print("AVG(" + costType + ")");
				writer.print("\t");
				writer.print("STDDEV(" + costType + ")");
				writer.print("\t");
				writer.print("MAX(" + costType + ")");
			}
			writer.println();

			for (int bin = 0; bin < this.getBinCnt(); bin++) {
				writer.print(bin);
				for (String costType : this.costType2matrices.keySet()) {
					final List<Double> minAvgStdMax = (new MatrixUtils())
							.get_min_avg_stddev_max(this.costType2matrices.get(
									costType).getMatrix(
									this.numberStrings.toString(bin)));
					writer.print("\t");
					writer.print(minAvgStdMax.get(0));
					writer.print("\t");
					writer.print(minAvgStdMax.get(1));
					writer.print("\t");
					writer.print(minAvgStdMax.get(2));
					writer.print("\t");
					writer.print(minAvgStdMax.get(3));
				}
				writer.println();
			}

			writer.flush();
			writer.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).severe(
					"Failed to write summary to file: " + e.getMessage());
		}
	}

	public void writeToFile(final Map<String, String> costType2fileName) {
		for (Map.Entry<String, String> costType2fileNameEntry : costType2fileName
				.entrySet()) {
			final MatricesWriter writer = new MatricesWriter(
					this.costType2matrices.get(costType2fileNameEntry.getKey()));
			writer.setIndentationString("  ");
			writer.setPrettyPrint(true);
			writer.write(costType2fileNameEntry.getValue());
		}
	}

	public void writeToScaperFiles(final String prefix) {
		// for (int bin = 0; bin < this.getBinCnt(); bin++) {
		// final String binString = this.numberStrings.toString(bin);
		//
		// final Matrix matrix = this.matrices.getMatrix(binString);
		// final Matrices dummyMatrices = new Matrices();
		// dummyMatrices.getMatrices().put(matrix.getId(), matrix);
		//
		// final MatricesWriter writer = new MatricesWriter(this.matrices);
		// writer.setIndentationString("  ");
		// writer.setPrettyPrint(true);
		// writer.write(prefix + this.numberStrings.toString(bin) + ".xml");
		// }
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		// System.out.println("STARTED ...");
		//
		// final String networkFileName =
		// "./test/matsim-testrun/input/network-plain.xml";
		// final Config config = ConfigUtils.createConfig();
		// config.setParam("network", "inputNetworkFile", networkFileName);
		// final Scenario scenario = ScenarioUtils.loadScenario(config);
		//
		// final String zonesShapeFileName =
		// "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		// final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
		// StockholmTransformationFactory.WGS84_EPSG3857);
		// zonalSystem.addNetwork(scenario.getNetwork(),
		// StockholmTransformationFactory.WGS84_SWEREF99);
		//
		// final String eventsFileName =
		// "./test/matsim-testrun/matsim-output/ITERS/it.0/0.events.xml.gz";
		//
		// final int startTime_s = 6 * 3600;
		// final int binSize_s = 3600;
		// final int binCnt = 16;
		// final int sampleCnt = 1;
		//
		// final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
		// scenario.getNetwork(), 15 * 60, 24 * 3600 - 1, scenario
		// .getConfig().travelTimeCalculator());
		// final EventsManager events = EventsUtils.createEventsManager();
		// events.addHandler(ttcalc);
		// final MatsimEventsReader reader = new MatsimEventsReader(events);
		// reader.readFile(eventsFileName);
		// final TravelTime linkTTs = ttcalc.getLinkTravelTimes();
		//
		// final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
		// scenario.getNetwork(), linkTTs,
		// new OnlyTimeDependentTravelDisutility(linkTTs), zonalSystem,
		// new Random(), startTime_s, binSize_s, binCnt, sampleCnt);
		// travelTimeMatrices.writeToFile("./test/matsim-testrun/traveltimes.xml");

		System.out.println("... DONE");
	}
}