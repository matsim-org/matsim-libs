/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkTransform.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.network.algorithms;

import java.util.Iterator;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordinateTransformation;

public class NetworkTransform {

	private final CoordinateTransformation transformer;

	public NetworkTransform(final CoordinateTransformation transformer) {
		super();
		this.transformer = transformer;
	}

	public void run(final NetworkLayer network) {
		Iterator<? extends Node> n_it = network.getNodes().values().iterator();
		while (n_it.hasNext()) {
			Node n = n_it.next();
			Coord coord = n.getCoord();
			Coord new_coord = transformer.transform(coord);
			coord.setXY(new_coord.getX(), new_coord.getY());
		}
	}
}
