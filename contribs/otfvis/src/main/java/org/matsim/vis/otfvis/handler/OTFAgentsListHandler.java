/* *********************************************************************** *
 * project: org.matsim.*
 * OTFAgentsListHandler.java
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

package org.matsim.vis.otfvis.handler;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.PositionInfo;
import org.matsim.vis.snapshotwriters.VisData;

import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

/**
 * OTFAgentsListHandler is responsible for the IO of the
 * agent's data in case of a mvi file converted from an events-file.
 *
 * @author david
 */
public class OTFAgentsListHandler extends OTFDataReader {

	private static final AgentState[] al = AgentState.values();
	private final PositionInfo.DirectBuilder builder = new PositionInfo.DirectBuilder();

	private void readAgent(ByteBuffer in, SceneGraph graph) {
		String id = ByteBufferUtils.getString(in);
		float x = in.getFloat();
		float y = in.getFloat();
		int int1 = in.getInt();
		int int2 = in.getInt();
		float float1 = in.getFloat();

		var agInfo = builder
				.setPersonId(Id.createPersonId(id))
				.setEasting(x)
				.setNorthing(y)
				.setAgentState(al[int1])
				.setUserDefined(int2)
				.setColorValue(float1)
				.build();

		graph.getAgentPointLayer().addAgent(agInfo);
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) {
		int count = in.getInt();
		for (int i = 0; i < count; i++) readAgent(in, graph);
	}

	@Override
	public void readConstData(ByteBuffer in) {
	}

	@Override
	public void invalidate(SceneGraph graph) {
	}

	static public class Writer extends OTFDataWriter<VisData> {

		private static final long serialVersionUID = -6368752578878835954L;

		@Override
		public void writeConstData(ByteBuffer out) {

		}

		@Override
		public void writeDynData(ByteBuffer out) {
			Collection<AgentSnapshotInfo> positions = new ArrayList<>();
			src.addAgentSnapshotInfo(positions);
			out.putInt(positions.size());
			for (AgentSnapshotInfo pos : positions) {
				writeAgent(pos, out);
			}
		}

		private static void writeAgent(AgentSnapshotInfo agInfo, ByteBuffer out) {
			String id = agInfo.getId().toString();
			ByteBufferUtils.putString(out, id);
			Point2D.Double point = OTFServerQuadTree.transform(new Coord(agInfo.getEasting(), agInfo.getNorthing()));
			out.putFloat((float) point.getX());
			out.putFloat((float) point.getY());
			out.putInt(agInfo.getAgentState().ordinal() ) ;
			out.putInt(agInfo.getUserDefined());
			out.putFloat((float)agInfo.getColorValueBetweenZeroAndOne());
		}
	}
}
