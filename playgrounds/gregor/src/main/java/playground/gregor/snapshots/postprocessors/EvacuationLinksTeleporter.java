/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationLinksTeleporter.java
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

package playground.gregor.snapshots.postprocessors;

import playground.gregor.snapshots.writers.PositionInfo;

public class EvacuationLinksTeleporter implements PostProcessorI{

	private final static double TELEPORTATION_SPEED = 100.0;
	protected final static String TELEPORTATION_X = "0.0";
	protected final static String TELEPORTATION_Y = "0.0";
	protected final static double D_TELEPORTATION_X = 0.0;
	protected final static double D_TELEPORTATION_Y = 0.0;
	public String[] processEvent(String[] event) {
		double  velocity = Double.parseDouble(event[6]);
		if (velocity >= TELEPORTATION_SPEED){
			event[11] = TELEPORTATION_X;
			event[12] = TELEPORTATION_Y;
		}
		return event;
	}

	public void processPositionInfo(PositionInfo pos) {
		if (pos.getColorValueBetweenZeroAndOne() > TELEPORTATION_SPEED) {
			pos.setEasting(D_TELEPORTATION_X);
			pos.setNorthing(D_TELEPORTATION_Y);
		}
		
		
	}
}
