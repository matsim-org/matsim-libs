/* *********************************************************************** *
 * project: org.matsim.*
 * OTFServerQuadI
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.data;

import java.nio.ByteBuffer;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.interfaces.OTFServerRemote;

public interface OTFServerQuadI {

	public void initQuadTree(OTFConnectionManager connect);

	public void addAdditionalElement(OTFDataWriter element);

	public OTFClientQuad convertToClient(String id, final OTFServerRemote host, final OTFConnectionManager connect);

	public void writeConstData(ByteBuffer out);

	public void writeDynData(QuadTree.Rect bounds, ByteBuffer out);

	// Internally we hold the coordinates from 0,0 to max -min .. to optimize use of float in visualizer
	public double getMaxEasting();

	public double getMaxNorthing();

	public double getMinEasting();

	public double getMinNorthing();

}