/* *********************************************************************** *
 * project: org.matsim.*
 * AllAgentsTeleporter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

public class AllAgentsTeleporter extends EvacuationLinksTeleporter {
	
	@Override
	public String[] processEvent(String[] event) {
			event[11] = TELEPORTATION_X;
			event[12] = TELEPORTATION_Y;
			return event;
	}
	
	@Override
	public void processPositionInfo(PositionInfo pos) {
		
			pos.setEasting(D_TELEPORTATION_X);
			pos.setNorthing(D_TELEPORTATION_Y);
		
		
		
	}

}
