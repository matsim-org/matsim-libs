/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDefaultNetWriterFactoryImpl.java
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

package org.matsim.utils.vis.otfivs.data;

import java.io.Serializable;

import org.matsim.utils.vis.otfivs.handler.OTFDefaultNodeHandler;
import org.matsim.utils.vis.otfivs.handler.OTFLinkAgentsNoParkingHandler;


public class OTFDefaultNetWriterFactoryImpl implements Serializable, OTFNetWriterFactory {

	private  OTFWriterFactory agentWriterFac = null;
	private  OTFWriterFactory nodeWriterFac = new OTFDefaultNodeHandler.Writer();
	private  OTFWriterFactory linkWriterFac = new OTFLinkAgentsNoParkingHandler.Writer();
	
	public OTFDataWriter getAgentWriter() {
		if(agentWriterFac != null) return agentWriterFac.getWriter();
		return null;
	}

	public OTFDataWriter getLinkWriter() {
		return linkWriterFac.getWriter();
	}

	public OTFDataWriter getNodeWriter() {
		return nodeWriterFac.getWriter();
	}

	public void setAgentWriterFac(OTFWriterFactory agentWriterFac) {
		this.agentWriterFac = agentWriterFac;
	}

	public void setNodeWriterFac(OTFWriterFactory nodeWriterFac) {
		this.nodeWriterFac = nodeWriterFac;
	}

	public void setLinkWriterFac(OTFWriterFactory linkWriterFac) {
		this.linkWriterFac = linkWriterFac;
	}

}
