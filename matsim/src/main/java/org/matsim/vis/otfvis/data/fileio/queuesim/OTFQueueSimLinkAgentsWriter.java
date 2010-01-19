/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueueSimLinkAgentsWriter
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
package org.matsim.vis.otfvis.data.fileio.queuesim;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;


/**
 * @author dgrether
 *
 */
public class OTFQueueSimLinkAgentsWriter extends OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink> {

	private static final long serialVersionUID = 6541770536927245851L;

	public static boolean showParked = false;
	
	protected static final transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

	public void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
		String id = pos.getId().toString();
		ByteBufferUtils.putString(out, id);
		out.putFloat((float)(pos.getEasting() - OTFServerQuad2.offsetEast));
		out.putFloat((float)(pos.getNorthing()- OTFServerQuad2.offsetNorth));
		if (pos.getAgentState()== AgentState.AGENT_AT_ACTIVITY) {
			// What is the next legs mode?
			QueueVehicle veh = src.getVehicle(pos.getId());
			if (veh == null) {
				out.putInt(1);
			} else {
				DriverAgent driver = veh.getDriver(); 
				Leg leg = driver.getCurrentLeg();
				if (leg != null) {
					if(leg.getMode() == TransportMode.pt) {
						out.putInt(2);
					} else if(leg.getMode() == TransportMode.bus) {
						out.putInt(3);
					} else {
						out.putInt(1);
					}
				} else {						
					out.putInt(1);
				}
			}
		} else {
			out.putInt(0);
		}
		out.putFloat((float)pos.getSpeed());
	}
	
	protected void writeAllAgents(ByteBuffer out) {
		// Write additional agent data

		positions.clear();
		src.getVisData().getVehiclePositions(positions);

		if (showParked) {
			out.putInt(positions.size());

			for (AgentSnapshotInfo pos : positions) {
				writeAgent(pos, out);
			}
		} else {
			int valid = 0;
			for (AgentSnapshotInfo pos : positions) {
				if (pos.getAgentState() != AgentState.AGENT_AT_ACTIVITY) valid++;
			}
			out.putInt(valid);

			for (AgentSnapshotInfo pos : positions) {
				if (pos.getAgentState() != AgentState.AGENT_AT_ACTIVITY) writeAgent(pos, out);
			}
		}

	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		out.putFloat((float)this.src.getVisData().getDisplayableTimeCapValue());
		writeAllAgents(out);
	}
	
		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			String id = this.src.getLink().getId().toString();
			ByteBufferUtils.putString(out, id);
			//subtract minEasting/Northing somehow!
			Point2D.Double.Double linkStart = new Point2D.Double.Double(this.src.getLink().getFromNode().getCoord().getX() - OTFServerQuad2.offsetEast, 
					this.src.getLink().getFromNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			Point2D.Double.Double linkEnd = new Point2D.Double.Double(this.src.getLink().getToNode().getCoord().getX() - OTFServerQuad2.offsetEast,
					this.src.getLink().getToNode().getCoord().getY() - OTFServerQuad2.offsetNorth);
			
			out.putFloat((float) linkStart.x); 
			out.putFloat((float) linkStart.y);
			out.putFloat((float) linkEnd.x); 
			out.putFloat((float) linkEnd.y);

			out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
		}

		public OTFDataWriter<QueueLink> getWriter() {
			return new OTFQueueSimLinkAgentsWriter();
		}
	
}
