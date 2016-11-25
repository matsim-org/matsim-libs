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

import java.util.Collection;

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

	public QuadTree<T> getLinkQuadTree(Network network) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for (Link link : network.getLinks().values()) {
			if (link.getCoord().getX() < minX) {
				minX = link.getCoord().getX();
			}

			if (link.getCoord().getY() < minY) {
				minY = link.getCoord().getY();
			}

			if (link.getCoord().getX() > maxX) {
				maxX = link.getCoord().getX();
			}

			if (link.getCoord().getY() > maxY) {
				maxY = link.getCoord().getY();
			}
		}

		return new QuadTree<T>(minX, minY, maxX + 1.0, maxY + 1.0);
	}
	
	public QuadTree<T> getQuadTree(EnclosingRectangle rectagle){
		return new QuadTree<T>(rectagle.getMinX() -1.0, rectagle.getMinY() -1.0, rectagle.getMaxX() + 1.0, rectagle.getMaxY() + 1.0);
	}

}
