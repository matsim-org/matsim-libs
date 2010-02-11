/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultConnectionManagerFactory
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
package org.matsim.vis.otfvis.data;

import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.SimpleStaticNetLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer.AgentPointDrawer;


/**
 * 
 * Creates the default OTFVis ConnectionManager
 * @author dgrether
 *
 */
public class DefaultConnectionManagerFactory implements OTFConnectionManagerFactory {
	
	/**
	 * @see org.matsim.vis.otfvis.data.OTFConnectionManagerFactory#createConnectionManager()
	 */
	public OTFConnectionManager createConnectionManager(){
		OTFConnectionManager connect = new OTFConnectionManager();
		connect.connectQLinkToWriter(OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		connect.connectWriterToReader(OTFLinkLanesAgentsNoParkingHandler.Writer.class, OTFLinkLanesAgentsNoParkingHandler.class);
		connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, SimpleStaticNetLayer.SimpleQuadDrawer.class);
		connect.connectReceiverToLayer(SimpleStaticNetLayer.SimpleQuadDrawer.class, SimpleStaticNetLayer.class);
		connect.connectReaderToReceiver(OTFLinkLanesAgentsNoParkingHandler.class, AgentPointDrawer.class);
		connect.connectReceiverToLayer(AgentPointDrawer.class, OGLAgentPointLayer.class);
		return connect;
	}
	
}
