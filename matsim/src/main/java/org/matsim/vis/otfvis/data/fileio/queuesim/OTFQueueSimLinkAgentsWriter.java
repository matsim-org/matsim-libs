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

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;

/**
 * @author dgrether
 */
public class OTFQueueSimLinkAgentsWriter extends OTFDataWriter<QueueLink> implements OTFWriterFactory<QueueLink> {

	private static final long serialVersionUID = -7916541567386865404L;

	public static final boolean showParked = false;

	protected static final transient Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

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
				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY)
					valid++;
			}
			out.putInt(valid);

			for (AgentSnapshotInfo pos : positions) {
				if (pos.getAgentState() != AgentState.PERSON_AT_ACTIVITY)
					writeAgent(pos, out);
			}
		}

	}

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
		String id = this.src.getLink().getId().toString();
		ByteBufferUtils.putString(out, id);
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

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		out.putFloat((float)this.src.getVisData().getDisplayableTimeCapValue(QSimTimer.getTime()));
		writeAllAgents(out);
	}

	@Override
	public OTFDataWriter<QueueLink> getWriter() {
		return new OTFQueueSimLinkAgentsWriter();
	}

	public void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
		String id = pos.getId().toString();
		ByteBufferUtils.putString(out, id);
		out.putFloat((float) (pos.getEasting() - OTFServerQuad2.offsetEast));
		out.putFloat((float) (pos.getNorthing() - OTFServerQuad2.offsetNorth));
		out.putInt(pos.getUserDefined());
		out.putFloat((float) pos.getColorValueBetweenZeroAndOne());
		out.putInt(pos.getAgentState().ordinal());
	}

}
