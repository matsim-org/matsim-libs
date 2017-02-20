package gunnar.ihop2.regent;

import gunnar.ihop2.regent.demandreading.ZonalSystem;
import gunnar.ihop2.regent.demandreading.Zone;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import saleem.stockholmmodel.utils.StockholmTransformationFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CreateScatterPlots {

	private CreateScatterPlots() {
	}

	public static void run(final String file1, final String matrixID1,
			final String file2, final String matrixID2, final String scatterFile)
			throws FileNotFoundException {

		final boolean plotAll = true;
		
		final Matrix xMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile(file1);
			if (matrixID1 == null) {
				xMatrix = m1.getMatrices().values().iterator().next();
			} else {
				xMatrix = m1.getMatrix(matrixID1);
			}
		}

		final Matrix yMatrix;
		{
			final Matrices m2 = new Matrices();
			final MatsimMatricesReader r2 = new MatsimMatricesReader(m2, null);
			r2.readFile(file2);
			if (matrixID2 == null) {
				yMatrix = m2.getMatrices().values().iterator().next();
			} else {
				yMatrix = m2.getMatrix(matrixID2);
			}
		}

		// >>> TODO NEW >>>

		final double xMin = Math.min(669756, 677953);
		final double xMax = Math.max(669756, 677953);

		final double yMin = Math.min(6587129, 6577267);
		final double yMax = Math.max(6587129, 6577267);

		final String networkFileName = "./test/matsim-testrun/input/network-plain.xml";
		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final String zonesShapeFileName = "./test/regentmatsim/input/sverige_TZ_EPSG3857.shp";
		final ZonalSystem zonalSystem = new ZonalSystem(zonesShapeFileName,
				StockholmTransformationFactory.WGS84_EPSG3857);
		zonalSystem.addNetwork(scenario.getNetwork(),
				StockholmTransformationFactory.WGS84_SWEREF99);

		final CoordinateTransformation zone2netTrafo = StockholmTransformationFactory
				.getCoordinateTransformation(
						StockholmTransformationFactory.WGS84_EPSG3857,
						StockholmTransformationFactory.WGS84_SWEREF99);

		final Set<String> centralZoneIDs = new LinkedHashSet<>();
		for (Zone zone : zonalSystem.getId2zoneView().values()) {

			final Point centroid;
			{
				final Coordinate zoneCentroidCoordinate = zone.getGeometry()
						.getCentroid().getCoordinate();
				centroid = MGC.coord2Point(zone2netTrafo.transform(new Coord(
						zoneCentroidCoordinate.x, zoneCentroidCoordinate.y)));
			}

			if (plotAll || (centroid.getCoordinate().x >= xMin
					&& centroid.getCoordinate().x <= xMax
					&& centroid.getCoordinate().y >= yMin
					&& centroid.getCoordinate().y <= yMax)) {
				centralZoneIDs.add(zone.getId());
			}
			
			
			
		}

		// / <<< TODO NEW <<<

		final PrintWriter writer = new PrintWriter(scatterFile);
		for (List<Entry> row1 : xMatrix.getFromLocations().values()) {
			for (Entry entry1 : row1) {

				if (centralZoneIDs.contains(entry1.getFromLocation())
						|| centralZoneIDs.contains(entry1.getToLocation())) {

					final Entry entry2 = yMatrix.getEntry(
							entry1.getFromLocation(), entry1.getToLocation());
					if (entry2 != null) {
						writer.println(entry1.getValue() + ","
								+ entry2.getValue());
					}

				}
			}
		}
		writer.flush();
		writer.close();
	}

	public static void main(String[] args) throws FileNotFoundException {

//		run("./test/referencedata/EMME_traveltimes_WORK_mf8.xml", null,
//				"./test/matsim-testrun/tourtts.xml", "WORK",
//				"./test/emme-vs-matsim_WORK_scatter.txt");

		run("./test/referencedata/EMME_traveltimes_OTHER_mf9.xml", null,
				"./test/matsim-testrun/tourtts.xml", "OTHER",
				"./test/emme-vs-matsim_OTHER_scatter.txt");

		// run("./test/10percentCarNetworkPlain/travelTimeMatrices.xml",
		// "TT_06:30:00", "./test/original/EMME_traveltimes_WORK_mf8.xml",
		// "./test/original/EMME_traveltimes_WORK_mf8.txt",
		// "./test/matsim07-00_vs_EMME.csv");
		// run("./test/10percentCarNetworkPlain/tourTravelTimeMatrices.xml",
		// "WORK", "./test/original/EMME_traveltimes_WORK_mf8.xml",
		// "./test/original/EMME_traveltimes_WORK_mf8.txt",
		// "./test/matsimWORK_vs_EMME.csv");
	}

}
