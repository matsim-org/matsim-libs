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
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueVehicle;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.vis.snapshots.writers.PositionInfo;
import org.matsim.vis.snapshots.writers.PositionInfo.VehicleState;

/**
 * OTFLinkAgentsHandler transfers basic agent data as well as the default data for links.
 * It is not commonly used, but some older mvi files might contain it.
 * 
 * 
 * @author david
 *
 */
public class OTFLinkAgentsHandler extends OTFDefaultLinkHandler {
	
	static {
		OTFDataReader.setPreviousVersion(OTFLinkAgentsHandler.class.getCanonicalName() + "V1.1", ReaderV1_1.class);
	}
	
	private final Logger log = Logger.getLogger(OTFLinkAgentsHandler.class);

	private Class agentReceiverClass = null;

	public static boolean showParked = false;
	
	protected List<OTFDataSimpleAgentReceiver> agents = new LinkedList<OTFDataSimpleAgentReceiver>();
	
	static public class Writer extends  OTFDefaultLinkHandler.Writer {

		private static final long serialVersionUID = -7916541567386865404L;

		protected static final transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

		public void writeAgent(PositionInfo pos, ByteBuffer out) {
			String id = pos.getAgentId().toString();
			ByteBufferUtils.putString(out, id);
			out.putFloat((float)(pos.getEasting() - OTFServerQuad2.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad2.offsetNorth));
			if (pos.getVehicleState()== VehicleState.Parking) {
				// What is the next legs mode?
				QueueVehicle veh = src.getVehicle(pos.getAgentId());
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

				for (PositionInfo pos : positions) {
					writeAgent(pos, out);
				}
			} else {
				int valid = 0;
				for (PositionInfo pos : positions) {
					if (pos.getVehicleState() != VehicleState.Parking) valid++;
				}
				out.putInt(valid);

				for (PositionInfo pos : positions) {
					if (pos.getVehicleState() != VehicleState.Parking) writeAgent(pos, out);
				}
			}

		}

		@Override
		public void writeDynData(ByteBuffer out) throws IOException {
			super.writeDynData(out);
			
			writeAllAgents(out);
		}

		@Override
		public OTFDataWriter<QueueLink> getWriter() {
			return new Writer();
		}

	}
	
	
	public void readAgent(ByteBuffer in, SceneGraph graph) {
		String id = ByteBufferUtils.getString(in);
//		int length = in.getInt();
//		if(length > 100) {
//			log.warn("Agent could not be read fully from stream");
//			return;
//		}
//		
//		char[] idBuffer = new char[length];
//		for(int i=0;i<length;i++) idBuffer[i] = in.getChar();
		float x = in.getFloat();
		float y = in.getFloat();
		int state = in.getInt();
		// Convert to km/h 
		float color = in.getFloat()*3.6f;
		// No agent receiver given, then we are finished
		if (agentReceiverClass == null) return;

		OTFDataSimpleAgentReceiver drawer = null;
		try {
			drawer = (org.matsim.vis.otfvis.data.OTFDataSimpleAgentReceiver) graph.newInstance(agentReceiverClass);
			drawer.setAgent(id.toCharArray(), x, y, 0, state, color);
			agents.add(drawer);
		} catch (InstantiationException e) {
			log.warn("Agent drawer could not be instanciated");
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} //factoryAgent.getOne();

	}
	
	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		super.readDynData(in, graph);
		// read additional agent data
		agents.clear();
		
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in, graph);
	}

	@Override
	public void connect(OTFDataReceiver receiver) {
		super.connect(receiver);
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgentReceiver) {
			this.agentReceiverClass = receiver.getClass();
		}

	}

	@Override
	public void invalidate(SceneGraph graph) {
		super.invalidate(graph);
		// invalidate agent receivers
		for(OTFDataSimpleAgentReceiver agent : agents) agent.invalidate(graph);
	}

	
	/***
	 * PREVIOUS VERSION of the reader
	 * 
	 * @author dstrippgen
	 */
	public static final class ReaderV1_1 extends OTFLinkAgentsHandler {
		@Override
		public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
			agents.clear();
			
			int count = in.getInt();
			for(int i= 0; i< count; i++) readAgent(in, graph);
		}
	}
}
