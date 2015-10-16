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
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.networks.construction.NetworkPostprocessor;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SUMO2VisNetwork implements NetworkPostprocessor<VisNetwork> {

	// -------------------- CONSTANTS --------------------

	public static final String X_ATTRIBUTE = "x";

	public static final String Y_ATTRIBUTE = "y";

	public static final String NOLANES_ATTRIBUTE = "nolanes";

	// -------------------- CONSTRUCTION --------------------

	public SUMO2VisNetwork() {
	}

	// -------------------- IMPLEMENTATION --------------------

	@Override
	public void run(final VisNetwork network) {

		network.setMinEasting(Double.POSITIVE_INFINITY);
		network.setMaxEasting(Double.NEGATIVE_INFINITY);
		network.setMinNorthing(Double.POSITIVE_INFINITY);
		network.setMaxNorthing(Double.NEGATIVE_INFINITY);

		for (VisNode node : network.getNodes()) {
			node.setEasting(parseDouble(node.getAttr(X_ATTRIBUTE)));
			node.setNorthing(parseDouble(node.getAttr(Y_ATTRIBUTE)));
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
					.getAttr(NOLANES_ATTRIBUTE))));
			link.setLength_m(MathHelpers.length(
					link.getFromNode().getEasting(), link.getFromNode()
							.getNorthing(), link.getToNode().getEasting(), link
							.getToNode().getNorthing()));
			link.setVisible(true);
			link.setTransform(NetVis.newLinear2PlaneTransform(link));
		}
	}
}
