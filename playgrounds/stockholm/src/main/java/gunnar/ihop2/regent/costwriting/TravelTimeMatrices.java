package gunnar.ihop2.regent.costwriting;

import static org.matsim.matrices.MatrixUtils.add;
import static org.matsim.matrices.MatrixUtils.divHadamard;
import static org.matsim.matrices.MatrixUtils.inc;
import static org.matsim.matrices.MatrixUtils.newAnonymousMatrix;
import floetteroed.utilities.Time;
import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;
import gunnar.ihop2.integration.MATSimDummy;
import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

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
		// TODO CONTINUE HERE
	}

	public TravelTimeMatrices(final Network network,
			final TravelTime linkTTs,
			// final Set<String> relevantZoneIDs,
			final ZonalSystem zonalSystem, final Random rnd,
			final int startTime_s, final int binSize_s, final int binCnt,
			final int sampleCnt) {

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;

		/*
		 * Identify all zones that are relevant and contain at least one node.
		 */

		final Set<String> relevantAndFeasibleZoneIDs = new LinkedHashSet<>();
		// if (relevantZoneIDs == null) {
		// if no relevant zone IDs were defined then identify them self
		// Logger.getLogger(MATSimDummy.class.getName()).warning(
		// "no relevant zone ids given, using all zones in "
		// + "zonal system that contain at least one node");
		for (Zone zone : zonalSystem) {
			if (zonalSystem.getNodes(zone).size() > 0) {
				relevantAndFeasibleZoneIDs.add(zone.getId());
			}
		}
		// } else {
		// // make sure that all relevant zones exist and contain nodes
		// for (String zoneId : relevantZoneIDs) {
		// final Zone zone = zonalSystem.getZone(zoneId);
		// if (zone == null) {
		// Logger.getLogger(MATSimDummy.class.getName()).warning(
		// "zonal system does not contain zone id " + zoneId);
		// } else {
		// if (zonalSystem.getNodes(zone).size() > 0) {
		// relevantAndFeasibleZoneIDs.add(zoneId);
		// } else {
		// Logger.getLogger(MATSimDummy.class.getName()).warning(
		// "zone with id " + zoneId
		// + " does not contain any nodes");
		// }
		// }
		// }
		// }

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

			// go through all origin zones
			for (String fromZoneID : relevantAndFeasibleZoneIDs) {
				// go through a sample of nodes in the origin zone
				for (Node fromNode : MathHelpers.drawWithoutReplacement(
						sampleCnt, zonalSystem.getNodes(fromZoneID), rnd)) {
					final LeastCostPathTree lcpt = new LeastCostPathTree(
							linkTTs, new OnlyTimeDependentTravelDisutility(
									linkTTs));
					lcpt.calculate(network, fromNode, time_s);
					// go through all destination zones
					for (String toZoneID : relevantAndFeasibleZoneIDs) {
						// go through a sample of nodes in the destination zone
						for (Node toNode : MathHelpers.drawWithoutReplacement(
								sampleCnt, zonalSystem.getNodes(toZoneID), rnd)) {
							add(ttMatrix_min, fromZoneID, toZoneID, lcpt
									.getTree().get(toNode.getId()).getCost()
									* Units.MIN_PER_S);
							inc(cntMatrix, fromZoneID, toZoneID);
						}
					}
				}
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

		final String networkFileName = "./test/regentmatsim/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		int timeBinSize = 15 * 60;
		int endTime = 12 * 3600;

		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final String eventsFileName = "./test/it.580/580.events.xml.gz";

		final int startTime_s = 18 * 3600;
		final int binSize_s = 1800;
		final int binCnt = 8;
		final int sampleCnt = 5;

		final TravelTimeCalculator ttcalc = new TravelTimeCalculator(
				scenario.getNetwork(), timeBinSize, endTime, scenario
						.getConfig().travelTimeCalculator());
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(ttcalc);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);
		final TravelTime linkTTs = ttcalc.getLinkTravelTimes();

		final TravelTimeMatrices travelTimeMatrices = new TravelTimeMatrices(
				scenario.getNetwork(), linkTTs,
				// null,
				zonalSystem, new Random(), startTime_s, binSize_s, binCnt,
				sampleCnt);
		travelTimeMatrices.writeToFile("./test/travelTimeMatrices_18-22.xml");

		System.out.println("... DONE");
	}
}