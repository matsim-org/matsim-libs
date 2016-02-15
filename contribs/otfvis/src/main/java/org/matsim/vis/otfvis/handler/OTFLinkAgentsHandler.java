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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleQuadDrawer;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfo.AgentState;
import org.matsim.vis.snapshotwriters.AgentSnapshotInfoFactory;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;
import org.matsim.vis.snapshotwriters.VisLink;


/**
 * I think this is responsible when agents are visualized "live", i.e. directly from the simulation. kai, nov'15
 */
public class OTFLinkAgentsHandler extends OTFDataReader {

	public static boolean showParked = false;

	private OGLSimpleQuadDrawer quadReceiver = new OGLSimpleQuadDrawer();

	private final SnapshotLinkWidthCalculator linkWidthCalculator = new SnapshotLinkWidthCalculator();
	private final AgentSnapshotInfoFactory snapshotFactory = new AgentSnapshotInfoFactory(linkWidthCalculator);
	
	static public class Writer extends OTFDataWriter<VisLink> implements OTFWriterFactory<VisLink> {

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
		public void writeConstData(ByteBuffer out) throws IOException {
			String id = this.src.getLink().getId().toString();
			ByteBufferUtils.putString(out, id);
			//subtract minEasting/Northing somehow!
			Point2D.Double.Double linkStart = OTFServerQuadTree.transform(this.src.getLink().getFromNode().getCoord());
			Point2D.Double.Double linkEnd = OTFServerQuadTree.transform(this.src.getLink().getToNode().getCoord());

			out.putFloat((float) linkStart.x); 
			out.putFloat((float) linkStart.y);
			out.putFloat((float) linkEnd.x); 
			out.putFloat((float) linkEnd.y);
				if ( OTFVisConfigGroup.NUMBER_OF_LANES.equals(OTFClientControl.getInstance().getOTFVisConfig().getLinkWidthIsProportionalTo()) ) {
					out.putInt(NetworkUtils.getNumberOfLanesAsInt(0, this.src.getLink()));
				} else if ( OTFVisConfigGroup.CAPACITY.equals(OTFClientControl.getInstance().getOTFVisConfig().getLinkWidthIsProportionalTo()) ) {
					out.putInt( 1 + (int)(2.*this.src.getLink().getCapacity()/3600.) ) ;
					// yyyyyy 3600. is a magic number (the default of the capacity period attribute in Network) but I cannot get to the network (where "capacityPeriod" resides).  
					// Please do better if you know better.  kai, jun'11
				} else {
					throw new RuntimeException("I do not understand.  Aborting ..." ) ;
				}
		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			out.putFloat((float)0.) ; 

			positions.clear();
			this.src.getVisData().addAgentSnapshotInfo( positions);

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

		private static void writeAgent(AgentSnapshotInfo pos, ByteBuffer out) {
			String id = pos.getId().toString();
			ByteBufferUtils.putString(out, id);
			Point2D.Double point = OTFServerQuadTree.transform(new Coord(pos.getEasting(), pos.getNorthing()));
			out.putFloat((float) point.getX());
			out.putFloat((float) point.getY());
			out.putInt(pos.getUserDefined());
			out.putFloat((float) pos.getColorValueBetweenZeroAndOne());
			out.putInt(pos.getAgentState().ordinal());
		}

		@Override
		public OTFDataWriter<VisLink> getWriter() {
			return new Writer();
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

		AgentSnapshotInfo agInfo = snapshotFactory.createAgentSnapshotInfo(Id.create(id, Person.class), x, y, 0., 0.);
		agInfo.setColorValueBetweenZeroAndOne(colorValue);
		agInfo.setUserDefined(userdefined);
		agInfo.setAgentState(AgentState.values()[state]);
		sceneGraph.getAgentPointLayer().addAgent(agInfo);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
		String id = ByteBufferUtils.getString(in);
		this.quadReceiver.setQuad(in.getFloat(), in.getFloat(),in.getFloat(), in.getFloat(), in.getInt());
		this.quadReceiver.setId(id.toCharArray());
	}

	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		this.quadReceiver.setColor(in.getFloat());

		int count = in.getInt();
		for (int i = 0; i < count; i++) {
			readAgent(in, graph);
		}
	}

	@Override
	public void invalidate(SceneGraph graph) {
		this.quadReceiver.addToSceneGraph(graph);
	}

	public OGLSimpleQuadDrawer getQuadReceiver() {
		return quadReceiver;
	}

}
