/* *********************************************************************** *
 * project: org.matsim.*
 * OTFLinkAgentsHandler.java
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshots.writers.VisLink;

/**
 * OTFLinkAgentsHandler transfers basic agent data as well as the default data
 * for links. It is not commonly used, but some older mvi files might contain
 * it. [[If this is correct, then only the reader needs to be salvaged.  kai, jan'11]]
 *
 * @author david
 *
 */
public class OTFLinkAgentsHandler extends OTFDefaultLinkHandler {

	private static final Logger log = Logger.getLogger(OTFLinkAgentsHandler.class);

	private Class<? extends OTFDataReceiver> agentReceiverClass = null;

	public static boolean showParked = false;

	protected List<OTFDataSimpleAgentReceiver> agents = new LinkedList<OTFDataSimpleAgentReceiver>();

	static public class Writer extends OTFDefaultLinkHandler.Writer {

		private static final long serialVersionUID = -7916541567386865404L;

		/** 
		 * Hui, warum ist das denn statisch? Damit nicht fuer jeden Writer und damit fuer jeden Link eine eigene
		 * Collection angelegt wird, die ohnehin fuer jeden Link geleert und wieder gefuellt wird, nehme ich an.
		 * Das ist ein bisschen gefaehrlich, weil man ja denken koennte, dass so ein linkspezifischer Writer eventuell
		 * threadsafe und damit parallelisierbar sein koennte.
		 * 
		 * michaz feb 2011
		 */
		private static final transient Collection<AgentSnapshotInfo> positions = new ArrayList<AgentSnapshotInfo>();

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			super.writeDynData(out);

			positions.clear();
			this.src.getVisData().getVehiclePositions( positions);
			
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
		public OTFDataWriter<VisLink> getWriter() {
			return new Writer();
		}

		/**The API method is writeDynData.  writeDynData calls writeAgent.  Could make it "protected", but I don't
		 * want proliferation of inheritance all over the project.  Still leaving it package-private to allow
		 * inheritance inside the package.  kai, jan'11  
		 */
		private void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
			// making this private; I don't see any reason to make it more public.  kai, jan'11

			String id = pos.getId().toString();
			ByteBufferUtils.putString(out, id);
			out.putFloat((float) (pos.getEasting() - OTFServerQuad2.offsetEast));
			out.putFloat((float) (pos.getNorthing() - OTFServerQuad2.offsetNorth));
			out.putInt(pos.getUserDefined());
			out.putFloat((float) pos.getColorValueBetweenZeroAndOne());
			out.putInt(pos.getAgentState().ordinal());
		}
	}

	private void readAgent(ByteBuffer in, SceneGraph sceneGraph) {
		// yyyy there is a very similar method in OTFAgentsListHandler.  with a more robust format, they should be united.  kai, apr'10

		String id = ByteBufferUtils.getString(in);
		float x = in.getFloat();
		float y = in.getFloat();
		int userdefined = in.getInt();
		float colorValue = in.getFloat();
		int state = in.getInt();

		AgentSnapshotInfo agInfo = AgentSnapshotInfoFactory.staticCreateAgentSnapshotInfo(new IdImpl(id), x, y, 0., 0.);
		agInfo.setColorValueBetweenZeroAndOne(colorValue);
		agInfo.setUserDefined(userdefined);
		agInfo.setAgentState(AgentState.values()[state]);

		if (this.agentReceiverClass == null)
			return;

		try {
			OTFDataSimpleAgentReceiver drawer = (OTFDataSimpleAgentReceiver) sceneGraph.newInstanceOf(this.agentReceiverClass);
			drawer.setAgent(agInfo);
			this.agents.add(drawer);
		} catch (InstantiationException e) {
			log.warn("Agent drawer could not be instanciated");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		super.readDynData(in, graph);

		// read additional agent data
		this.agents.clear();

		int count = in.getInt();
		for (int i = 0; i < count; i++) {
			readAgent(in, graph);
		}
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		super.connect(receiver);
		// connect agent receivers
		if (receiver instanceof OTFDataSimpleAgentReceiver) {
			this.agentReceiverClass = receiver.getClass();
		}

	}

	@Override
	public void invalidate(SceneGraph graph) {
		super.invalidate(graph);
		// invalidate agent receivers
		for (OTFDataSimpleAgentReceiver agent : this.agents) {
			agent.invalidate(graph);
		}
	}

}
