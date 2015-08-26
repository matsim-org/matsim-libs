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

import java.awt.geom.AffineTransform;

import floetteroed.utilities.networks.construction.AbstractLink;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class VisLink extends AbstractLink<VisNode, VisLink> {

	// -------------------- MEMBERS --------------------

	private int lanes;

	private double length_m;

	private boolean visible;

	private AffineTransform transform;

	// -------------------- CONSTRUCTION --------------------

	public VisLink(String id) {
		super(id);
	}

	// -------------------- WRITE ACCESS --------------------

	void setLanes(final int lanes) {
		this.lanes = lanes;
	}

	void setLength_m(final double length_m) {
		this.length_m = length_m;
	}

	void setVisible(final boolean visible) {
		this.visible = visible;
	}

	void setTransform(final AffineTransform transform) {
		this.transform = transform;
	}

	// -------------------- READ ACCESS --------------------

	int getLanes() {
		return this.lanes;
	}

	double getLength_m() {
		return this.length_m;
	}

	boolean getVisible() {
		return this.visible;
	}

	AffineTransform getTransform() {
		return this.transform;
	}
}
