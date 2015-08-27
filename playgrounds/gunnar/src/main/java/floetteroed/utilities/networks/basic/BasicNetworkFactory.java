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
package floetteroed.utilities.networks.basic;

import floetteroed.utilities.networks.construction.AbstractNetworkFactory;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class BasicNetworkFactory extends
		AbstractNetworkFactory<BasicNode, BasicLink, BasicNetwork> {

	@Override
	protected BasicNetwork newNetwork(final String id, final String type) {
		return new BasicNetwork(id, type);
	}

	@Override
	protected BasicNode newNode(String id) {
		return new BasicNode(id);
	}

	@Override
	protected BasicLink newLink(String id) {
		return new BasicLink(id);
	}

}
