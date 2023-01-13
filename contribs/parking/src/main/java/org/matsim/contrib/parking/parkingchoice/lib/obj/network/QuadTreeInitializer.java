/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.lib.obj.network;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;

/**
 *
 * @author rashid_waraich
 *
 * @param <T>
 */
public class QuadTreeInitializer<T> {


	public QuadTree<T> getQuadTree(EnclosingRectangle rectagle){
		return new QuadTree<T>(rectagle.getMinX() -1.0, rectagle.getMinY() -1.0, rectagle.getMaxX() + 1.0, rectagle.getMaxY() + 1.0);
	}

}
