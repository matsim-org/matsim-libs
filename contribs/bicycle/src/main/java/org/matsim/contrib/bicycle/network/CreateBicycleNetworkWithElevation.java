package org.matsim.contrib.bicycle.network;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.osm.networkReader.OsmBicycleReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.Set;

/// **
// * Reads an OSM file, builds a bicycle network, and attaches elevation information
// * at two levels:
// * <ul>
// *   <li>Each Node gets a Z coordinate from the DEM.</li>
// *   <li>Each Link gets five elevation KPIs sampled every {@value #SAMPLE_STEP_M} m
// *       along its straight-line geometry: average elevation, mean gradient (signed,
// *       in the direction of travel), max gradient, cumulative climb and cumulative
// *       descent.</li>
// * </ul>
// * The per-link KPIs are computed <em>after</em> the network is fully built, so that
// * {@link NetworkUtils#cleanNetwork} has a chance to drop disconnected components
// * before we spend effort sampling them.
// */


/**
 * @deprecated Prototype for testing DEM-based elevation enrichment only.
 * Superseded by {@link BicycleNetworkPipeline}, which provides the full
 * end-to-end pipeline including OSM reading, infrastructure classification,
 * and bicycle-aware network simplification.
 * This class will be removed in a future version.
 */
@Deprecated
public class CreateBicycleNetworkWithElevation {


	/**
	 * Distance between elevation samples along a link, in meters. Lower = finer
	 * detail (better max-gradient, more accurate gain/loss) but more DEM queries.
	 * Rough guidance:
	 * 10 m  — very fine, catches short ramps; ~5x the work of 50 m
	 * 50 m  — matches the DEM resolution here; good default
	 * 100 m  — coarse, may miss short steep sections
	 * A link shorter than SAMPLE_STEP_M still gets 2 samples (both endpoints).
	 */
	private static final double SAMPLE_STEP_M = 10.0;

	/**
	 * Douglas-Peucker tolerance for smoothing the sampled profile, in meters.
	 * Intermediate samples whose elevation deviates less than this from the
	 * straight line between their neighbours are dropped before the KPIs are
	 * computed. Smaller values keep more detail (and more noise); larger
	 * values smooth more aggressively.
	 * 0.5 m  — barely filters anything; only kills pure quantisation
	 * 2.0 m  — conservative, kills most DEM noise while keeping real hills
	 * 5.0 m  — GraphHopper's default; aggressive
	 * 10.0 m  — only keeps big hills
	 * Set to 0 to disable smoothing entirely.
	 */
	private static final double NOISE_TOLERANCE_M = 10.0;

	// ---- link attribute keys ---------------------------------------------------
	public static final String LINK_ATTR_GRADIENT = "gradient";
	public static final String LINK_ATTR_MAX_GRADIENT = "maxGradient";
	public static final String LINK_ATTR_ELEVATION_GAIN = "elevationGain";
	public static final String LINK_ATTR_ELEVATION_LOSS = "elevationLoss";


	private static final String inputCRS = "EPSG:4326"; // WGS84 -- Update if needed, but usually OSM is in WGS84
    private static final String outputCRS = "EPSG:XXXXX"; // Update accordingly
	private static final String demCRS = "EPSG:XXXXX";	// Update accordingly
    private static final String inputOsmFile = "path/to/your/input/file.osm.pbf";
    private static final String inputTiffFile = "path/to/your/elevation/tiff-file.tif";
    private static final String outputFile = "path/to/your/output/network.xml.gz";

	public static void main(String[] args) {

		var elevationParser = new ElevationDataParser(inputTiffFile, outputCRS, demCRS);
		var transformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, outputCRS);

		var network = new OsmBicycleReader.Builder()
			.setCoordinateTransformation(transformation)
			.setAfterLinkCreated((link, tags, direction) -> {

				addElevationIfNecessary(link.getFromNode(), elevationParser);
				addElevationIfNecessary(link.getToNode(), elevationParser);
			})
			.build()
			.read(inputOsmFile);

		NetworkUtils.cleanNetwork(network, Set.of(TransportMode.car, TransportMode.bike));

		// Attach per-link KPIs only to what survived cleanNetwork().
		int counted = 0;
		for (Link link : network.getLinks().values()) {
			attachLinkElevationKpis(link, elevationParser);
			counted++;
		}
		System.out.println("Attached elevation KPIs to " + counted + " links "
			+ "(sample step = " + SAMPLE_STEP_M + " m, noise tolerance = " + NOISE_TOLERANCE_M + " m).");

		new NetworkWriter(network).write(outputFile);
	}

	private static synchronized void addElevationIfNecessary(Node node, ElevationDataParser elevationParser) {

		if (!node.getCoord().hasZ()) {
			var elevation = elevationParser.getElevation(node.getCoord());
			var newCoord = CoordUtils.createCoord(node.getCoord().getX(), node.getCoord().getY(), elevation);
			// I think it should work to replace the coord on the node reference, since the network only stores references
			// to the node and the internal quad tree only references the x,y-values and the node. janek 4.2020
			node.setCoord(newCoord);
		}
	}

	private static void attachLinkElevationKpis(Link link, ElevationDataParser elevationParser) {
		LinkElevationProfile.Kpis k = LinkElevationProfile.compute(
			link, SAMPLE_STEP_M, NOISE_TOLERANCE_M, elevationParser);

		// Meters (DEM vertical resolution is 0.1 m) → round to 1 decimal.
		link.getAttributes().putAttribute(BicycleUtils.AVERAGE_ELEVATION, round(k.averageElevation(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_GAIN, round(k.elevationGain(), 1));
		link.getAttributes().putAttribute(LINK_ATTR_ELEVATION_LOSS, round(k.elevationLoss(), 1));

		// Dimensionless ratios — 5 decimals = 0.01 % resolution, well below DEM noise.
		link.getAttributes().putAttribute(LINK_ATTR_GRADIENT, round(k.gradient(), 4));
		link.getAttributes().putAttribute(LINK_ATTR_MAX_GRADIENT, round(k.maxGradient(), 4));
	}

	private static double round(double v, int decimals) {
		double factor = Math.pow(10, decimals);
		return Math.round(v * factor) / factor;
	}
}
