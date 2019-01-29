/* *********************************************************************** *
 * project: org.matsim.*
 * OTFServerRemote.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.interfaces;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;

/**
 * OTFServerRemote is the one most important interface for the
 * communication between client and server. All methods necessary for data
 * exchange are bundled in this interface.
 *
 * @author dstrippgen
 */
public interface OTFServer {

	void requestNewTime(int time);

	OTFServerQuadTree getQuad(OTFConnectionManager connect);

	byte[] getQuadConstStateBuffer();

	byte[] getQuadDynStateBuffer(QuadTree.Rect bounds);

	int getLocalTime();

	/**
	 * @return If the server is "live" (i.e. has user control) or not.
	 */
	boolean isLive();

	Collection<Double> getTimeSteps();

	void setShowNonMovingItems(boolean showNonMovingItems);

	boolean isFinished();

	OTFVisConfigGroup getOTFVisConfig();

}

