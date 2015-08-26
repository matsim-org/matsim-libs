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

import floetteroed.utilities.DynamicDataXMLFileIO;
import floetteroed.utilities.networks.construction.AbstractLink;
import floetteroed.utilities.networks.construction.AbstractNetwork;

/**
 * <u><b>The entire utilitis.visualization package is experimental!</b></u>
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class LinkDataIO<L extends AbstractLink<?, L>, NET extends AbstractNetwork<?, L>>
		extends DynamicDataXMLFileIO<L> {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private final NET net;

	// -------------------- CONSTRUCTION --------------------

	public LinkDataIO(final NET net) {
		this.net = net;
	}

	// ---------- IMPLEMENTATION OF DynamicDataXMLFileIO INTERFACE ----------

	@Override
	protected L attrValue2key(final String linkID) {
		return this.net.getLink(linkID);
	}

	@Override
	protected String key2attrValue(final L link) {
		return link.getId().toString();
	}
}
