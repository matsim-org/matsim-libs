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

package org.matsim.utils.vis.otfvis.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.data.OTFDataSimpleAgent;
import org.matsim.utils.vis.otfvis.data.OTFDataWriter;
import org.matsim.utils.vis.otfvis.data.OTFServerQuad;
import org.matsim.utils.vis.otfvis.data.OTFWriterFactory;
import org.matsim.utils.vis.otfvis.data.OTFData.Receiver;
import org.matsim.utils.vis.otfvis.interfaces.OTFDataReader;
import org.matsim.utils.vis.snapshots.writers.PositionInfo;
import org.matsim.utils.vis.snapshots.writers.PositionInfo.VehicleState;


public class OTFLinkAgentsHandler extends OTFDefaultLinkHandler {
	
	static boolean prevV1_1 = OTFDataReader.setPreviousVersion(OTFLinkAgentsHandler.class.getCanonicalName() + "V1.1", ReaderV1_1.class);
	
	private final Logger log = Logger.getLogger(OTFLinkAgentsHandler.class);

	private Class agentReceiverClass = null;

	public static boolean showParked = false;
	
	protected List<OTFDataSimpleAgent.Receiver> agents = new LinkedList<OTFDataSimpleAgent.Receiver>();
	
	static public class Writer extends  OTFDefaultLinkHandler.Writer implements  OTFWriterFactory<QueueLink>{

		/**
		 * 
		 */
		private static final long serialVersionUID = -7916541567386865404L;
		protected static final transient Collection<PositionInfo> positions = new ArrayList<PositionInfo>();

		@Override
		public void writeConstData(ByteBuffer out) throws IOException {
			super.writeConstData(out);
		}


		public void writeAgent(PositionInfo pos, ByteBuffer out) throws IOException {
			String id = pos.getAgentId().toString();
			out.putInt(id.length());
			for (int i=0; i<id.length(); i++) out.putChar(id.charAt(i));
			out.putFloat((float)(pos.getEasting() - OTFServerQuad.offsetEast));
			out.putFloat((float)(pos.getNorthing()- OTFServerQuad.offsetNorth));
			out.putInt(pos.getVehicleState()== VehicleState.Parking ? 1:0);
			out.putFloat((float)pos.getSpeed());
		}
		
		protected void writeAllAgents(ByteBuffer out) throws IOException {
			// Write additional agent data
	        /*
	         * (4) write agents
	         */
	        positions.clear();
			src.getVehiclePositions(positions);

			if(showParked) {
				out.putInt(positions.size());

				for (PositionInfo pos : positions) {
					writeAgent(pos, out);
				}
			}else {
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
	
	static char[] idBuffer = new char[100];
	
	public void readAgent(ByteBuffer in, SceneGraph graph) throws IOException {
		try{
		int length = in.getInt();
		if(length > 100)throw new Exception();
		idBuffer = new char[length];
		for(int i=0;i<length;i++) idBuffer[i] = in.getChar();
		float x = in.getFloat();
		float y = in.getFloat();
		int state = in.getInt();
		// Convert to km/h 
		float color = in.getFloat()*3.6f;
		// No agent receiver given, then we are finished
		if (agentReceiverClass == null) return;

			OTFDataSimpleAgent.Receiver drawer = null;
			try {
				drawer = (org.matsim.utils.vis.otfvis.data.OTFDataSimpleAgent.Receiver) graph.newInstance(agentReceiverClass);
				drawer.setAgent(idBuffer, x, y, 0, state, color);
				agents.add(drawer);
			} catch (InstantiationException e) {
				log.warn("Agent drawer could not be instanciated");
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} //factoryAgent.getOne();

		}catch (Exception e){
			log.warn("Agent could not be read fully from stream");
		}
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
	public void readConstData(ByteBuffer in) throws IOException {
		super.readConstData(in);
	}



	@Override
	public void connect(Receiver receiver) {
		super.connect(receiver);
		//connect agent receivers
		if (receiver  instanceof OTFDataSimpleAgent.Receiver) {
			this.agentReceiverClass = receiver.getClass();
		}

	}

	@Override
	public void invalidate(SceneGraph graph) {
		super.invalidate(graph);
		// invalidate agent receivers
		for(OTFDataSimpleAgent.Receiver agent : agents) agent.invalidate(graph);
	}

	
	/***
	 * PREVIOUS VERSION of the reader
	 * @author dstrippgen
	 *
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
