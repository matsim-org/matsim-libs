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

import floetteroed.utilities.networks.construction.AbstractNode;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class VisNode extends AbstractNode<VisNode, VisLink> {

	// -------------------- MEMBERS --------------------

	private double easting;
	private double northing;

	// -------------------- CONSTRUCTION --------------------

	VisNode(final String id) {
		super(id);
	}

	// -------------------- WRITE ACCESS --------------------

	void setEasting(final double easting) {
		this.easting = easting;
	}
	
	void setNorthing(final double northing) {
		this.northing = northing;
	}
	
	// -------------------- READ ACCESS --------------------

	double getEasting() {
		return this.easting;
	}
	
	double getNorthing() {
		return this.northing;
	}	
	
}
