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

import floetteroed.utilities.networks.construction.AbstractNetworkFactory;
import floetteroed.utilities.networks.construction.NetworkContainer;
import floetteroed.utilities.networks.containerloaders.MATSimNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.OpenStreetMapNetworkContainerLoader;
import floetteroed.utilities.networks.containerloaders.SUMONetworkContainerLoader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VisNetworkFactory extends
		AbstractNetworkFactory<VisNode, VisLink, VisNetwork> {

	@Override
	protected VisNetwork newNetwork(final String id, final String type) {
		return new VisNetwork(id, type);
	}

	@Override
	protected VisNode newNode(final String id) {
		return new VisNode(id);
	}

	@Override
	protected VisLink newLink(final String id) {
		return new VisLink(id);
	}

//	@Override
//	public VisNetwork newNetwork(final NetworkContainer container) {
//
//		final VisNetwork result = super.newNetwork(container);
//
//		if (MATSimNetworkContainerLoader.MATSIM_NETWORK_TYPE.equals(result.getType())) {
//			(new MATSim2VisNetwork()).run(result);
//		} else if (OpenStreetMapNetworkContainerLoader.OPENSTREETMAP_NETWORK_TYPE
//				.equals(result.getType())) {
//			(new OpenStreetMap2VisNetwork()).run(result);
//		} else if (SUMONetworkContainerLoader.SUMO_NETWORK_TYPE.equals(result
//				.getType())) {
//			(new SUMO2VisNetwork()).run(result);
//		} else {
//			throw new RuntimeException("unknown network type: "
//					+ result.getType());
//		}
//
//		return result;
//	}

}
