/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.visualization;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.networks.construction.AbstractNetwork;
import floetteroed.utilities.networks.construction.AbstractNode;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;


/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class OpenStreetMap2VisNetwork implements NetworkPostprocessor<VisNetwork> {

	// -------------------- CONSTANTS --------------------

	public static final double EARTHRADIUS_M = 6378137;

	public static final String NODE_LONGITUDE_ATTRIBUTE = "lon";

	public static final String NODE_LATITUDE_ATTRIBUTE = "lat";

	// -------------------- CONSTRUCTION --------------------

	public OpenStreetMap2VisNetwork() {
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void run(final VisNetwork network) {

		/*
		 * (1) extract Euclidean node coordinates
		 */
		final Map<VisNode, Point2D.Double> node2xy = node2xy(network);

		/*
		 * (2) add node attributes and identify network size
		 */
		double minEasting = Double.POSITIVE_INFINITY;
		double maxEasting = Double.NEGATIVE_INFINITY;
		double minNorthing = Double.POSITIVE_INFINITY;
		double maxNorthing = Double.NEGATIVE_INFINITY;
		for (Map.Entry<VisNode, Point2D.Double> entry : node2xy.entrySet()) {
			final double easting = entry.getValue().x;
			final double northing = entry.getValue().y;
			final VisNode node = entry.getKey();
			node.setEasting(easting);
			node.setNorthing(northing);
			minEasting = Math.min(minEasting, easting);
			maxEasting = Math.max(maxEasting, easting);
			minNorthing = Math.min(minNorthing, northing);
			maxNorthing = Math.max(maxNorthing, northing);
		}
		network.setMinEasting(minEasting);
		network.setMaxEasting(maxEasting);
		network.setMinNorthing(minNorthing);
		network.setMaxNorthing(maxNorthing);

		/*
		 * (3) add link attributes
		 */
		for (VisLink link : network.getLinks()) {
			final VisNode from = link.getFromNode();
			final VisNode to = link.getToNode();

			link.setLength_m(MathHelpers.length(from.getEasting(),
					from.getNorthing(), to.getEasting(), to.getNorthing()));
			link.setLanes(1);
			link.setVisible(true);
			link.setTransform(NetVis.newLinear2PlaneTransform(link));
		}
	}

	// ----- STATIC IMPLEMENTATION OF GENERIC OPENSTREETMAP FUNCTIONALITY -----

	public static <N extends AbstractNode<N, ?>, NET extends AbstractNetwork<N, ?>> Map<N, Point2D.Double> node2LonLat(
			final NET network) {
		final Map<N, Point2D.Double> result = new HashMap<N, Point2D.Double>();
		for (N node : network.getNodes()) {
			final double lon = Double.parseDouble(node
					.getAttr(NODE_LONGITUDE_ATTRIBUTE));
			final double lat = Double.parseDouble(node
					.getAttr(NODE_LATITUDE_ATTRIBUTE));
			result.put(node, new Point2D.Double(lon, lat));
		}
		return result;
	}

	public static <N extends AbstractNode<N, ?>> Point2D.Double min(
			final Map<N, Point2D.Double> node2pnt) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		for (Map.Entry<N, Point2D.Double> entry : node2pnt.entrySet()) {
			minX = Math.min(minX, entry.getValue().x);
			minY = Math.min(minY, entry.getValue().y);
		}
		return new Point2D.Double(minX, minY);
	}

	public static <N extends AbstractNode<N, ?>> Point2D.Double max(
			final Map<N, Point2D.Double> node2pnt) {
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Map.Entry<N, Point2D.Double> entry : node2pnt.entrySet()) {
			maxX = Math.max(maxX, entry.getValue().x);
			maxY = Math.max(maxY, entry.getValue().y);
		}
		return new Point2D.Double(maxX, maxY);
	}

	public static <N extends AbstractNode<N, ?>> Map<N, Point2D.Double> node2xy(
			final Map<N, Point2D.Double> node2LonLat) {
		final Map<N, Point2D.Double> result = new HashMap<N, Point2D.Double>();
		/*
		 * (1) find extreme latitude and longitude
		 */
		final Point2D.Double minLonLat = min(node2LonLat);
		final double minLon = minLonLat.x;
		final double minLat = minLonLat.y;
		/*
		 * (2) project node coordinates onto plane; find extreme coordinates
		 */
		for (N node : node2LonLat.keySet()) {
			final double easting = EARTHRADIUS_M
					* Math.tan((node2LonLat.get(node).x - minLon) * Math.PI
							/ 180.0);
			final double northing = EARTHRADIUS_M
					* Math.tan((node2LonLat.get(node).y - minLat) * Math.PI
							/ 180.0);
			result.put(node, new Point2D.Double(easting, northing));
		}
		return result;
	}

	public static Map<VisNode, Point2D.Double> node2xy(final VisNetwork network) {
		return node2xy(node2LonLat(network));
	}
}
