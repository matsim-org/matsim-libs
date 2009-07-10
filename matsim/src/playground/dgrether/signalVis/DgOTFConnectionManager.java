/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFthisionManager
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

import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;


/**
 * @author dgrether
 *
 */
public class DgOTFConnectionManager extends OTFConnectionManager {

	/**
	 * 
	 */
	public DgOTFConnectionManager() {
		//data source to writer
		this.add(QueueLink.class, DgOtfLinkLanesAgentsNoParkingHandler.Writer.class);
		//writer -> reader: from server to client
		this.add(DgOtfLinkLanesAgentsNoParkingHandler.Writer.class, DgOtfLinkLanesAgentsNoParkingHandler.class);
		
		//reader to drawer (or provider to receiver)
//		this.add(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		this.add(DgOtfLinkLanesAgentsNoParkingHandler.class, DgSimpleQuadDrawer.class);
		this.add(OTFLinkLanesAgentsNoParkingHandler.class,  AgentPointDrawer.class);
		this.add(AgentPointDrawer.class, OGLAgentPointLayer.class);
		//this line seems to do nothing
		this.add(DgSimpleQuadDrawer.class, DgSimpleStaticNetLayer.class);
		
		

//		//writer -> reader
//		this.add(OTFDefaultLinkHandler.Writer.class, OTFDefaultLinkHandler.class);
//		this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
//		this.add(OTFLinkAgentsNoParkingHandler.Writer.class, OTFLinkAgentsHandler.class);
//		
//		this.add(OTFLinkAgentsHandler.Writer.class, OTFLinkAgentsHandler.class);
//		this.add(OTFDefaultNodeHandler.Writer.class, OTFDefaultNodeHandler.class);
//
//		//reader -> drawer
//		this.add(OTFLinkAgentsHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
//		
//		
//		this.add(OTFLinkAgentsHandler.class,  AgentPointDrawer.class);
//		this.add(OTFAgentsListHandler.Writer.class,  OTFAgentsListHandler.class);
//		this.add(OTFAgentsListHandler.class,  AgentPointDrawer.class);
		
	}

}
