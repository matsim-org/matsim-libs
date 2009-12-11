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

import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;

import playground.dgrether.signalVis.drawer.DgLaneSignalDrawer;
import playground.dgrether.signalVis.io.DgOtfLaneReader;
import playground.dgrether.signalVis.io.DgOtfLaneWriter;
import playground.dgrether.signalVis.io.DgOtfSignalWriter;
import playground.dgrether.signalVis.io.DgSignalReader;
import playground.dgrether.signalVis.layer.DgOtfLaneLayer;
import playground.dgrether.signalVis.layer.DgOtfSignalLayer;


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
			connectionManager.add(QueueLink.class, DgOtfLaneWriter.class);
			// writer -> reader: from server to client
			connectionManager
			.add(DgOtfLaneWriter.class, DgOtfLaneReader.class);
			// reader to drawer (or provider to receiver)
			connectionManager.add(DgOtfLaneReader.class, DgLaneSignalDrawer.class);
			// drawer -> layer
			connectionManager.add(DgLaneSignalDrawer.class, DgOtfLaneLayer.class);
		}
		else if (drawSignals) {
			// data source to writer
			connectionManager.add(QueueLink.class, DgOtfSignalWriter.class);
			// writer -> reader: from server to client
			connectionManager
			.add(DgOtfSignalWriter.class, DgSignalReader.class);
			// reader to drawer (or provider to receiver)
			connectionManager.add(DgSignalReader.class, DgLaneSignalDrawer.class);
			// drawer -> layer
			connectionManager.add(DgLaneSignalDrawer.class, DgOtfSignalLayer.class);
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
