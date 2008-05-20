/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLinkAgentsNoParkingHandler.java
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

package org.matsim.utils.vis.otfivs.handler;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.matsim.mobsim.QueueLink;
import org.matsim.utils.vis.otfivs.data.OTFDataWriter;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.PositionInfo.VehicleState;


public class OTFLinkAgentsNoParkingHandler extends OTFLinkAgentsHandler {
	
	static public class Writer extends  OTFLinkAgentsHandler.Writer {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6541770536927233851L;

		@Override
		protected void writeAllAgents(ByteBuffer out) throws IOException {
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
	        positions.clear();
			src.getVehiclePositions(positions);
			int valid = 0;
			for (PositionInfo pos : positions) {
				if (pos.getVehicleState() != VehicleState.Parking) valid++;
			}
			out.putInt(valid);

			for (PositionInfo pos : positions) {
				if (pos.getVehicleState() != VehicleState.Parking) writeAgent(pos, out);
			}
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
}
