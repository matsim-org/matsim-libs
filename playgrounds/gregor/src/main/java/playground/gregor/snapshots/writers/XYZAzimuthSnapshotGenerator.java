/* *********************************************************************** *
 * project: org.matsim.*
 * XYZAzimuthSnapshotGenerator.java
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
package playground.gregor.snapshots.writers;

import playground.gregor.sim2d.events.XYZEvent;
import playground.gregor.sim2d.events.XYZEventsHandler;

public class XYZAzimuthSnapshotGenerator implements XYZEventsHandler {

	private MVISnapshotWriter writer;

	public XYZAzimuthSnapshotGenerator(MVISnapshotWriter writer) {
		this.writer = writer;
	}

	@Override
	public void handleXYZEvent(XYZEvent e) {
		XYZAzimuthPositionInfo pos = new XYZAzimuthPositionInfo(e.getId(), e.getC(), e.getAzimuth(), e.getTime());
		this.writer.addVehicle(e.getTime(), pos);
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
