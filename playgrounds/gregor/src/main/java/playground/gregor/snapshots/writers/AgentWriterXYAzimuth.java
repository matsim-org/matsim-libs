/* *********************************************************************** *
 * project: org.matsim.*
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;

public class AgentWriterXYAzimuth extends OTFDataWriter {
	private static final long serialVersionUID = 1272762470879799116L;
	public transient Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

	@Override
	public void writeConstData(ByteBuffer out) throws IOException {
	}

	public void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
		String id = pos.getId().toString();
		ByteBufferUtils.putString(out, id);
		out.putFloat((float)(pos.getEasting() - OTFServerQuad2.offsetEast));
		out.putFloat((float)(pos.getNorthing()- OTFServerQuad2.offsetNorth));
		out.putInt(pos.getType());
		out.putInt(pos.getUserDefined());
		out.putFloat((float)pos.getColorValueBetweenZeroAndOne());
		out.putFloat((float)pos.getAzimuth());
	}

	@Override
	public void writeDynData(ByteBuffer out) throws IOException {
		// Write additional agent data
		if (this.src instanceof ArrayList) {
			this.positions = (Collection<AgentSnapshotInfo>) this.src;
		}

		out.putInt(this.positions.size());

		for (AgentSnapshotInfo pos : this.positions) {
			writeAgent(pos, out);
		}
		this.positions.clear();
	}
}
