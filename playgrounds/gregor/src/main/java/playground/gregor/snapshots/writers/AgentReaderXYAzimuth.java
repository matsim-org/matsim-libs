/* *********************************************************************** *
 * project: org.matsim.*
 * AgentReaderXYAzimuth.java
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

import org.apache.log4j.Logger;
import org.matsim.core.utils.misc.ByteBufferUtils;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.interfaces.OTFDataReader;

public class AgentReaderXYAzimuth extends OTFDataReader{


	Logger log = Logger.getLogger(AgentReaderXYAzimuth.class);
//	private Class receiver;
	boolean init = false;
	private float maxX = Float.NEGATIVE_INFINITY;
	private float minX = Float.POSITIVE_INFINITY;
	private float width;

	private int maxAgents  = 0;
	private AgentDrawerXYAzimuth drawer;
	
	public AgentReaderXYAzimuth() {
		super();
	}

	public void readAgent(ByteBuffer in) {
		String id = ByteBufferUtils.getString(in);
		float x = in.getFloat();
		float y = in.getFloat();
		int type = in.getInt();

		int user = in.getInt();
		float speed = in.getFloat();
		float azimuth = in.getFloat();
		
//		if (type >= 255) {
//			return;
//		}
		if (speed > 20) {
//			this.log.error("FIXME - agent should not be part of mvi file!");
			return;
		}
		this.drawer.addAgent(x,y,type,user,speed, id, azimuth);
		
		if (!this.init) {
			this.maxAgents++;
			if (x < this.minX ) {
				this.minX = x;
			} else if (x > this.maxX ) {
				this.maxX = x;
			}
//			if (y < this.minY) {
//				this.minY = y;
//			} else if (y > this.maxY) {
//				this.maxY = y;
//			}
		}
	}


	@Override
	public void readDynData(ByteBuffer in, SceneGraph graph) throws IOException {
		// read additional agent data
		 this.drawer = new AgentDrawerXYAzimuth();
		//			AgentReceiver drawer = (AgentReceiver) graph.newInstance(this.receiver);
//		graph.addItem(agents);
		//			drawer.addItem(agents);
//		graph.addItem(this.drawer);
		int count = in.getInt();
		for(int i= 0; i< count; i++) readAgent(in);
		if (!this.init) {
			this.width = this.maxX - this.minX;
			
			this.init = true;
		}
		this.drawer.init(this.width,this.maxAgents);
	}

	@Override
	public void readConstData(ByteBuffer in) throws IOException {
	}


	@Override
	public void connect(OTFDataReceiver receiver) {
//		if (receiver instanceof AgentDrawer) {
//			this.drawer = (AgentDrawer) receiver;
//		}
		//connect agent receivers
//		if (receiver  instanceof AgentReceiver) {
//			this.receiver = receiver.getClass();
//		}

	}


	@Override
	public void invalidate(SceneGraph graph) {
		this.drawer.invalidate(graph);
	}
}
