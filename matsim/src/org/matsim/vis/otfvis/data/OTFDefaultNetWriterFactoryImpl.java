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

package org.matsim.vis.otfvis.data;

import java.io.Serializable;

import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;


/**
 * @author dstrippgen
 *
 */
@Deprecated
public class OTFDefaultNetWriterFactoryImpl implements Serializable, OTFNetWriterFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8558382907215410103L;
	
	private  OTFWriterFactory agentWriterFac = null;
	private  OTFWriterFactory nodeWriterFac = new OTFDefaultNodeHandler.Writer();
	private  OTFWriterFactory linkWriterFac = new OTFLinkLanesAgentsNoParkingHandler.Writer();
	
	@Deprecated
	public OTFDataWriter getAgentWriter() {
		if(agentWriterFac != null) return agentWriterFac.getWriter();
		return null;
	}

	@Deprecated
	public OTFDataWriter getLinkWriter() {
		return linkWriterFac.getWriter();
	}

	@Deprecated
	public OTFDataWriter getNodeWriter() {
		return nodeWriterFac.getWriter();
	}

	@Deprecated
	public void setAgentWriterFac(OTFWriterFactory agentWriterFac) {
		this.agentWriterFac = agentWriterFac;
	}

	@Deprecated
	public void setNodeWriterFac(OTFWriterFactory nodeWriterFac) {
		this.nodeWriterFac = nodeWriterFac;
	}

	@Deprecated
	public void setLinkWriterFac(OTFWriterFactory linkWriterFac) {
		this.linkWriterFac = linkWriterFac;
	}

}
