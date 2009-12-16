/* *********************************************************************** *
 * project: org.matsim.*
 * OTFFileWriterConnectionManagerFactory
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
package org.matsim.vis.otfvis.data.fileio;

import org.matsim.ptproject.qsim.QueueLink;
import org.matsim.ptproject.qsim.QueueNode;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFConnectionManagerFactory;
import org.matsim.vis.otfvis.handler.OTFDefaultNodeHandler;
import org.matsim.vis.otfvis.handler.OTFLinkLanesAgentsNoParkingHandler;


/**
 * @author dgrether
 *
 */
public class OTFFileWriterConnectionManagerFactory implements OTFConnectionManagerFactory{

	public OTFConnectionManager createConnectionManager() {
		OTFConnectionManager c = new OTFConnectionManager();
		c.add(QueueLink.class,
				OTFLinkLanesAgentsNoParkingHandler.Writer.class);
		c.add(QueueNode.class, OTFDefaultNodeHandler.Writer.class);
		return c;
	}

}
