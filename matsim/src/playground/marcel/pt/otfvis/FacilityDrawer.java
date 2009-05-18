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

package playground.marcel.pt.otfvis;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.transitSchedule.TransitStopFacility;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFData.Receiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLDrawableImpl;
import org.matsim.vis.otfvis.opengl.gl.InfoText;
import org.matsim.vis.otfvis.opengl.queries.QueryAgentPlan;

import playground.marcel.pt.integration.TransitStopAgentTracker;
import playground.marcel.pt.transitSchedule.TransitSchedule;

public class FacilityDrawer {
	
	public static class DataWriter_v1_0 extends OTFDataWriter {

		private static final long serialVersionUID = 1L;
		private final transient TransitSchedule schedule;
		private final transient TransitStopAgentTracker agentTracker;
		
		public DataWriter_v1_0(final TransitSchedule schedule, final TransitStopAgentTracker agentTracker) {
			this.schedule = schedule;
			this.agentTracker = agentTracker;
		}
		
		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			out.putInt(this.schedule.getFacilities().size());
			for (TransitStopFacility facility : this.schedule.getFacilities().values()) {
				ByteBufferUtils.putString(out, facility.getId().toString());
				out.putDouble(facility.getCoord().getX());
				out.putDouble(facility.getCoord().getY());
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
		public void connect(Receiver receiver) {
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
				stop.x = in.getDouble();
				stop.y = in.getDouble();
				System.out.println("Facility " + stop.id + " at " + stop.x + "/" + stop.y);
				if (this.drawer != null) {
					this.drawer.stops.add(stop);
				}
			}
		}

		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
			for (VisBusStop stop : this.drawer.stops) {
				stop.nOfPeople = in.getInt();
			}
		}
		
	}

	public static class DataDrawer extends OTFGLDrawableImpl {

		/*package*/ final List<VisBusStop> stops = new LinkedList<VisBusStop>();
		
		public void onDraw(GL gl) {
			for (VisBusStop stop : this.stops) {
				QueryAgentPlan.drawCircle(gl, (float) stop.x, (float) stop.y, 50.0f);
				InfoText.showTextOnce(stop.id + ": " + stop.nOfPeople, (float) stop.x - 100.0f, (float) stop.y + 50.0f, 2.0f);
			}
		}
	}
	
	protected static class VisBusStop {
		public double x = 0.0;
		public double y = 0.0;
		public String id = null;
		public int nOfPeople = 0;
	}
	
}