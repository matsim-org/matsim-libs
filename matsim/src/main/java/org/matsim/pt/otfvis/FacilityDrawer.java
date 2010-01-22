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

package org.matsim.pt.otfvis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.pt.queuesim.TransitStopAgentTracker;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.gl.DrawingUtils;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.gl.InfoTextContainer;
import org.matsim.vis.snapshots.writers.AgentSnapshotInfo;
import org.matsim.vis.snapshots.writers.PositionInfo;


public class FacilityDrawer {
	private static final Logger log = Logger.getLogger(FacilityDrawer.class);
	
	public static class DataWriter_v1_0 extends OTFDataWriter {

		private static final long serialVersionUID = 1L;
		private final transient TransitSchedule schedule;
		private final transient TransitStopAgentTracker agentTracker;
		private final transient Network network ;
		
		public DataWriter_v1_0(final Network network, final TransitSchedule schedule, final TransitStopAgentTracker agentTracker) {
			this.network = network ;
			this.schedule = schedule;
			this.agentTracker = agentTracker;
		}
		
		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putInt(this.schedule.getFacilities().size());
			for (TransitStopFacility facility : this.schedule.getFacilities().values()) {
				ByteBufferUtils.putString(out, facility.getId().toString());
				if (facility.getLinkId() != null) {
					ByteBufferUtils.putString(out, facility.getLinkId().toString());
	
					// yyyy would most probably make sense to have something that generates coordinates for facilities
					Link link = this.network.getLinks().get( facility.getLinkId() ) ;
					if ( link==null ) {
						log.warn( " link not found; linkId: " + facility.getLinkId() ) ;
					}
					AgentSnapshotInfo ps = new PositionInfo(facility.getId(),link) ;
					
					out.putDouble( ps.getEasting() - OTFServerQuad2.offsetEast ) ;
					out.putDouble( ps.getNorthing() - OTFServerQuad2.offsetNorth ) ;
				} else {
					ByteBufferUtils.putString(out,"");
					out.putDouble(facility.getCoord().getX() - OTFServerQuad2.offsetEast);
					out.putDouble(facility.getCoord().getY() - OTFServerQuad2.offsetNorth);
				}
			}

		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			for (TransitStopFacility facility : this.schedule.getFacilities().values()) {
				out.putInt(this.agentTracker.getAgentsAtStop(facility).size());
			}
		}
		
	}
	
	public static class DataReader_v1_0 extends OTFDataReader {

		private DataDrawer drawer = null;
		
		@Override
		public void connect(OTFDataReceiver receiver) {
			if (receiver instanceof DataDrawer) {
				this.drawer = (DataDrawer) receiver;
			}
		}

		@Override
		public void invalidate(SceneGraph graph) {
			if (this.drawer != null) {
				this.drawer.invalidate(graph);
			}
		}

		@Override
		public void readConstData(ByteBuffer in) throws IOException {
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
				if (this.drawer != null) {
//					this.drawer.stops.add(stop);
				}
			}
			drawer.initTexts();
		}

		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
			for (VisBusStop stop : this.drawer.stops) {
				stop.setnOfPeople(in.getInt());
			}
		}
		
	}

	public static class DataDrawer extends OTFGLDrawableImpl {

		/*package*/ final List<VisBusStop> stops = new LinkedList<VisBusStop>();
		
		public void onDraw(GL gl) {
			for (VisBusStop stop : this.stops) {
				DrawingUtils.drawCircle(gl, (float) stop.x, (float) stop.y, 50.0f);			
			}
		}

		public void initTexts() {
			for (VisBusStop stop : this.stops) {
				if ( stop.linkId!=null ) {
					stop.stopText = InfoTextContainer.showTextPermanent(stop.buildText(), (float) stop.x - 100.0f, (float) stop.y + 50.0f, 2.0f);
					stop.stopText.setLinkId(new IdImpl(stop.linkId));
				}
			}
		}

	}
	
	private static class VisBusStop {
		public double x = 0.0;
		public double y = 0.0;
		public String id = null;
		public String linkId;
		private int nOfPeople = 0;
		private InfoText stopText;
		
		public void setnOfPeople(int nOfPeople) {
			int oldnOfPeople = this.nOfPeople;
			this.nOfPeople = nOfPeople;
			if (oldnOfPeople != nOfPeople) {
				updateText();
			}
		}
		
		private void updateText() {
			stopText.setText(buildText());
		}

		private String buildText() {
			return id + ": " + getnOfPeople();
		}
		
		private int getnOfPeople() {
			return nOfPeople;
		}
		
	}
	
}