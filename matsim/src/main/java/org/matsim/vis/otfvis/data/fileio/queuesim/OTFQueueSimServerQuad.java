/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQSimServerQuad
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
package org.matsim.vis.otfvis.data.fileio.queuesim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;


/**
 * @author dgrether
 *
 */
public class OTFQueueSimServerQuad extends OTFServerQuad2 {

	private static final long serialVersionUID = 24L;
	
  private static final Logger log = Logger.getLogger(OTFQueueSimServerQuad.class);
	
  transient private QueueNetwork net;
  /**
	 * 
	 */
	public OTFQueueSimServerQuad(QueueNetwork net) {
		super(net.getNetworkLayer());
		this.net = net;
	}

	@Override
	public void initQuadTree(OTFConnectionManager connect) {
		createFactoriesAndFillQuadTree(connect);
	}

	private void createFactoriesAndFillQuadTree(OTFConnectionManager connect) {
		// Get the writer Factories from connect
		Collection<Class<OTFWriterFactory<QueueLink>>> linkFactories = connect.getQueueLinkEntries();
		List<OTFWriterFactory<QueueLink>> linkWriterFactoryObjects = new ArrayList<OTFWriterFactory<QueueLink>>(linkFactories.size());
		try {
			OTFWriterFactory<QueueLink> linkWriterFac = null;			
			for (Class linkFactory : linkFactories ) {
				if(linkFactory != Object.class) {
					linkWriterFac = (OTFWriterFactory<QueueLink>)linkFactory.newInstance();
					linkWriterFactoryObjects.add(linkWriterFac);
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

    	if(!linkWriterFactoryObjects.isEmpty()) {
    		boolean first = true;
    		for (QueueLink link : this.net.getLinks().values()) {
    			double middleEast = (link.getLink().getToNode().getCoord().getX() + link.getLink().getFromNode().getCoord().getX())*0.5 - this.minEasting;
    			double middleNorth = (link.getLink().getToNode().getCoord().getY() + link.getLink().getFromNode().getCoord().getY())*0.5 - this.minNorthing;

    			for (OTFWriterFactory<QueueLink> fac : linkWriterFactoryObjects) {
    				OTFDataWriter<QueueLink> writer = fac.getWriter();
    				// null means take the default handler
    				if (writer != null) {
    					writer.setSrc(link);
    					if (first) {
    						log.info("Connecting Source QueueLink with " + writer.getClass().getName());
    						first = false;
    					}
    				}
    				this.put(middleEast, middleNorth, writer);
    			}
    		}
   	}
	}

	
}
