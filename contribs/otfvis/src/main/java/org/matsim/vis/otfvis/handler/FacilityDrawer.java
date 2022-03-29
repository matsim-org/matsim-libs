/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityDrawer.java
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

package org.matsim.vis.otfvis.handler;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.AgentTracker;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuadTree;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawableReceiver;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.gl.GLUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.snapshotwriters.PositionInfo;

import java.awt.*;
import java.awt.geom.Point2D;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class FacilityDrawer {
	private static final Logger log = Logger.getLogger(FacilityDrawer.class);

	public static class Writer extends OTFDataWriter<Void> {

		private static final long serialVersionUID = 1L;
		private final transient TransitSchedule schedule;
		private final transient AgentTracker agentTracker;
		private final transient Network network;
		private final transient PositionInfo.LinkBasedBuilder builder;

		public Writer(final Network network, final TransitSchedule schedule, final AgentTracker agentTracker, PositionInfo.LinkBasedBuilder positionInfoBuilder) {
			this.network = network;
			this.schedule = schedule;
			this.agentTracker = agentTracker;
			this.builder = positionInfoBuilder;
		}

		@Override
		public void writeConstData(ByteBuffer out) {
			out.putInt(this.schedule.getFacilities().size());
			for (TransitStopFacility facility : this.schedule.getFacilities().values()) {
				ByteBufferUtils.putString(out, facility.getId().toString());
				if (facility.getLinkId() != null) {
					// yyyy would most probably make sense to have something that generates coordinates for facilities
					Link link = this.network.getLinks().get(facility.getLinkId());
					if (link == null) {
						log.warn(" link not found; linkId: " + facility.getLinkId());
						ByteBufferUtils.putString(out, "");
						Point2D.Double point = OTFServerQuadTree.transform(facility.getCoord());
						out.putDouble(point.getX());
						out.putDouble(point.getY());
					} else {
						ByteBufferUtils.putString(out, facility.getLinkId().toString());
						var ps = builder
								.setPersonId(Id.createPersonId(facility.getId()))
								.setLinkId(link.getId())
								.setFromCoord(link.getFromNode().getCoord())
								.setToCoord(link.getToNode().getCoord())
								.setLinkLength(link.getLength())
								.setDistanceOnLink(0.9 * link.getLength())
								.setLane(0)
								.build();

						Point2D.Double point = OTFServerQuadTree.transform(new Coord(ps.getEasting(), ps.getNorthing()));
						out.putDouble(point.getX());
						out.putDouble(point.getY());
					}
				} else {
					ByteBufferUtils.putString(out,"");
					Point2D.Double point = OTFServerQuadTree.transform(facility.getCoord());
					out.putDouble(point.getX());
					out.putDouble(point.getY());
				}
			}

		}

		@Override
		public void writeDynData(ByteBuffer out) {
			for (TransitStopFacility facility : this.schedule.getFacilities().values()) {
				out.putInt(this.agentTracker.getAgentsAtFacility(facility.getId()).size());
			}
		}

	}

	public static class Reader extends OTFDataReader {

		private final DataDrawer drawer = new DataDrawer();

		@Override
		public void invalidate(SceneGraph graph) {
			this.drawer.addToSceneGraph(graph);
		}

		@Override
		public void readConstData(ByteBuffer in) {
			int numberOfEntries = in.getInt();
			for (int i = 0; i < numberOfEntries; i++) {
				VisBusStop stop = new VisBusStop();
				stop.id = ByteBufferUtils.getString(in);
				String linkIdString = ByteBufferUtils.getString(in);
				if (!linkIdString.isEmpty()) {
					stop.linkId = linkIdString;
				}
				stop.x = in.getDouble();
				stop.y = in.getDouble();
				this.drawer.stops.add(stop);
			}
		}

		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) {
			for (VisBusStop stop : this.drawer.stops) {
				stop.setnOfPeople(in.getInt());
			}
		}

	}

	public static class DataDrawer extends OTFGLAbstractDrawableReceiver {

		/*package*/ final List<VisBusStop> stops = new LinkedList<>();

		@Override
		public void onDraw(GL2 gl) {
			if (OTFClientControl.getInstance().getOTFVisConfig().isDrawTransitFacilities()) {
				TextRenderer textRenderer = new TextRenderer(new Font("SansSerif", Font.PLAIN, 32), true, false);

				for (VisBusStop stop : this.stops) {
					GLUtils.drawCircle(gl, (float) stop.x, (float) stop.y, 50.0f);
				}
				for (VisBusStop stop : this.stops) {
					if ( stop.linkId!=null ) {
						InfoText stopText = new InfoText(stop.buildText(), (float) stop.x - 100.0f, (float) stop.y + 50.0f);
						OTFOGLDrawer drawer = OTFClientControl.getInstance().getMainOTFDrawer();
						stopText.draw(textRenderer, OTFClientControl.getInstance().getMainOTFDrawer().getCanvas(), drawer.getViewBoundsAsQuadTreeRect());
					}
				}
			}
		}

		@Override
		public void addToSceneGraph(SceneGraph graph) {	
			graph.addItem(this);
		}


	}



	private static class VisBusStop {
		public double x = 0.0;
		public double y = 0.0;
		public String id = null;
		public String linkId;
		private int nOfPeople = 0;

		void setnOfPeople(int nOfPeople) {
			this.nOfPeople = nOfPeople;
		}

		private String buildText() {
			if (OTFClientControl.getInstance().getOTFVisConfig().isDrawTransitFacilityIds()) 
				return id + ": " + getnOfPeople();
			else 
				return Integer.toString(getnOfPeople());
		}

		private int getnOfPeople() {
			return nOfPeople;
		}

	}

}