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

import static java.lang.Double.parseDouble;
import static java.lang.Math.max;
import static java.lang.Math.min;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MATSim2VisNetwork implements NetworkPostprocessor<VisNetwork> {

	// -------------------- CONSTANTS --------------------

	public static final String EASTING_ATTRIBUTE = "x";

	public static final String NORTHING_ATTRIBUTE = "y";

	public static final String LANES_ATTRIBUTE = "permlanes";

	public static final String LENGTH_ATTRIBUTE = "length";

	public static final String CAPPERIOD_ATTRIBUTE = "capperiod";

	public static final String CAPACITY_PER_CAPPERIOD_ATTRIBUTE = "capacity";

	// -------------------- CONSTRUCTION --------------------

	public MATSim2VisNetwork() {
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void run(final VisNetwork network) {

		// final double capperiod_s = Time.secFromStr(network
		// .getLinksAttr(CAPPERIOD_ATTRIBUTE));

		network.setMinEasting(Double.POSITIVE_INFINITY);
		network.setMaxEasting(Double.NEGATIVE_INFINITY);
		network.setMinNorthing(Double.POSITIVE_INFINITY);
		network.setMaxNorthing(Double.NEGATIVE_INFINITY);

		for (VisNode node : network.getNodes()) {
			node.setEasting(parseDouble(node.getAttr(EASTING_ATTRIBUTE)));
			node.setNorthing(parseDouble(node.getAttr(NORTHING_ATTRIBUTE)));
			network.setMinEasting(min(network.getMinEasting(),
					node.getEasting()));
			network.setMaxEasting(max(network.getMaxEasting(),
					node.getEasting()));
			network.setMinNorthing(min(network.getMinNorthing(),
					node.getNorthing()));
			network.setMaxNorthing(max(network.getMaxNorthing(),
					node.getNorthing()));
		}

		for (VisLink link : network.getLinks()) {
			link.setLanes((int) Math.round(Double.parseDouble(link
					.getAttr(LANES_ATTRIBUTE))));
			link.setLength_m(Double.parseDouble(link.getAttr(LENGTH_ATTRIBUTE)));
			// final double cap_veh_s = Double.parseDouble(link
			// .getAttr(CAPACITY_PER_CAPPERIOD_ATTRIBUTE)) / capperiod_s;
			// link.setVisible(true);
			link.setVisible(link.getLength_m() > 1);
			// linkData.setTransform(NetVis.newLinear2PlaneTransform(link,
			// result.getVisNodeData(link.getFromNode()),
			// result.getVisNodeData(link.getToNode()),
			// linkData.getLength_m()));
			link.setTransform(NetVis.newLinear2PlaneTransform(link));
		}
	}
}
