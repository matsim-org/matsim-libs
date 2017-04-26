package gunnar.ihop2.regent;

import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class IdentifyCongestedZones {

	IdentifyCongestedZones() {
	}

	public static void main(String[] args) {

		final String freeFlowMatrixFile = "./test/matsim-testrun/freeflow-traveltimes.xml";
		final String congestedMatrixFile = "./test/matsim-testrun/tourtts.xml";
		final String freeFlowMatrixId = null;
		final String congestedMatrixID = "WORK";

		final Matrix freeFlowMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile(freeFlowMatrixFile);
			if (freeFlowMatrixId == null) {
				freeFlowMatrix = m1.getMatrices().values().iterator().next();
			} else {
				freeFlowMatrix = m1.getMatrix(freeFlowMatrixId);
			}
		}

		final Matrix congestedMatrix;
		{
			final Matrices m2 = new Matrices();
			final MatsimMatricesReader r2 = new MatsimMatricesReader(m2, null);
			r2.readFile(congestedMatrixFile);
			if (congestedMatrixID == null) {
				congestedMatrix = m2.getMatrices().values().iterator().next();
			} else {
				congestedMatrix = m2.getMatrix(congestedMatrixID);
			}
		}

		final String networkFileName = "./test/matsim-testrun/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);
		// final CoordinateTransformation zone2netTrafo =
		// StockholmTransformationFactory
		// .getCoordinateTransformation(
		// StockholmTransformationFactory.WGS84_EPSG3857,
		// StockholmTransformationFactory.WGS84_SWEREF99);

		for (Map.Entry<String, ArrayList<Entry>> cellId2column : congestedMatrix
				.getToLocations().entrySet()) {
			double ratioSum = 0;
			int posValCnt = 0;
			for (Entry entry1 : cellId2column.getValue()) {
				final double cong = entry1.getValue();
				final double free = freeFlowMatrix.getEntry(
						entry1.getFromLocation(), entry1.getToLocation())
						.getValue();
				if (cong > 0 && free > 0) {
					// System.out.println(cong + ", " + free);
					ratioSum += entry1.getValue()
							/ freeFlowMatrix.getEntry(entry1.getFromLocation(),
									entry1.getToLocation()).getValue();
					posValCnt++;
				}
			}
			if (posValCnt > 0) {
				final double avgRatio = ratioSum / posValCnt;
				if (avgRatio >= 5.0) {
					// System.out.println(cellId2row.getKey() + "\t" +
					// avgRatio);
					 final Zone zone =
					 zonalSystem.getZone(cellId2column.getKey());
					// System.out.println(zone.getId() + "\t" + avgRatio);
					System.out.println(zone.getGeometry().getCentroid().getX()
							+ ";" + zone.getGeometry().getCentroid().getY());
				}
			}
		}
	}
}
