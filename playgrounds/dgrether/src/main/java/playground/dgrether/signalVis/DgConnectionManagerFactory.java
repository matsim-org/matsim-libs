/* *********************************************************************** *
 * project: org.matsim.*
 * DgConnectionManager
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
package playground.dgrether.signalVis;

import org.matsim.lanes.otfvis.drawer.OTFLaneSignalDrawer;
import org.matsim.lanes.otfvis.io.OTFLaneReader;
import org.matsim.lanes.otfvis.io.OTFLaneWriter;
import org.matsim.lanes.otfvis.layer.OTFLaneLayer;
import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.signalsystems.otfvis.io.OTFSignalWriter;
import org.matsim.signalsystems.otfvis.io.OTFSignalReader;
import org.matsim.signalsystems.otfvis.layer.OTFSignalLayer;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;



/**
 * @author dgrether
 *
 */
public class DgConnectionManagerFactory {

	public OTFConnectionManager createConnectionManager() {
		OTFConnectionManager connectionManager = new OTFConnectionManager();
		boolean drawLanes = false;
		boolean drawSignals = true;
		
		if (drawLanes){
			// data source to writer
			connectionManager.add(QueueLink.class, OTFLaneWriter.class);
			// writer -> reader: from server to client
			connectionManager
			.add(OTFLaneWriter.class, OTFLaneReader.class);
			// reader to drawer (or provider to receiver)
			connectionManager.add(OTFLaneReader.class, OTFLaneSignalDrawer.class);
			// drawer -> layer
			connectionManager.add(OTFLaneSignalDrawer.class, OTFLaneLayer.class);
		}
		else if (drawSignals) {
			// data source to writer
			connectionManager.add(QueueLink.class, OTFSignalWriter.class);
			// writer -> reader: from server to client
			connectionManager
			.add(OTFSignalWriter.class, OTFSignalReader.class);
			// reader to drawer (or provider to receiver)
			connectionManager.add(OTFSignalReader.class, OTFLaneSignalDrawer.class);
			// drawer -> layer
			connectionManager.add(OTFLaneSignalDrawer.class, OTFSignalLayer.class);
		}

		//agent code
		// reader -> drawer
		connectionManager.add(OTFLinkLanesAgentsNoParkingHandler.class, AgentPointDrawer.class);
		//drawer -> layer
		connectionManager.add(AgentPointDrawer.class, OGLAgentPointLayer.class);

		
		//default network code
  	// data source to writer
		connectionManager.add(QueueLink.class, OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		//writer -> reader
		connectionManager
		.add(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		//reader -> drawer
		connectionManager.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		//drawer -> layer
		connectionManager.add(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		
		
		

		
		
		// //writer -> reader
		// this.add(OTFDefaultLinkHandler.Writer.class,
		// OTFDefaultLinkHandler.class);
		// this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		// this.add(OTFLinkAgentsNoParkingHandler.Writer.class,
		// OTFLinkAgentsHandler.class);
		//	
		// this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
		// this.add(OTFDefaultNodeHandler.Writer.class,
		// OTFDefaultNodeHandler.class);
		//
		// //reader -> drawer
		// this.add(OTFLinkAgentsHandler.class,
		// SimpleStaticNetLayer.SimpleQuadDrawer.class);
		//	
		//	
		// this.add(OTFLinkAgentsHandler.class, AgentPointDrawer.class);
		// this.add(OTFAgentsListHandler.Writer.class, OTFAgentsListHandler.class);
		return connectionManager;
	}
		
		
}
