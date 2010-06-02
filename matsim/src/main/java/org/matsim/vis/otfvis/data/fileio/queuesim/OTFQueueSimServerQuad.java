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
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;
import org.matsim.vis.snapshots.writers.VisLink;
import org.matsim.vis.snapshots.writers.VisNetwork;


/**
 * @author dgrether
 *
 */
public class OTFQueueSimServerQuad extends OTFServerQuad2 {

	private static final long serialVersionUID = 24L;

  private static final Logger log = Logger.getLogger(OTFQueueSimServerQuad.class);

  transient private VisNetwork net;
  /**
	 *
	 */
	public OTFQueueSimServerQuad(VisNetwork net) {
		super(net.getNetwork());
		this.net = net;
	}

	@Override
	public void initQuadTree(OTFConnectionManager connect) {
		createFactoriesAndFillQuadTree(connect);
	}

	private void createFactoriesAndFillQuadTree(OTFConnectionManager connect) {
		// Get the writer Factories from connect
		Collection<Class<OTFWriterFactory<VisLink>>> linkFactories = connect.getQueueLinkEntries();
		List<OTFWriterFactory<VisLink>> linkWriterFactoryObjects = new ArrayList<OTFWriterFactory<VisLink>>(linkFactories.size());
		try {
			OTFWriterFactory<VisLink> linkWriterFac = null;
			for (Class linkFactory : linkFactories ) {
				if(linkFactory != Object.class) {
					linkWriterFac = (OTFWriterFactory<VisLink>)linkFactory.newInstance();
					linkWriterFactoryObjects.add(linkWriterFac);
				}
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

    	if(!linkWriterFactoryObjects.isEmpty()) {
    		boolean first = true;
    		for (VisLink link : this.net.getVisLinks().values()) {
    			double middleEast = (link.getLink().getToNode().getCoord().getX() + link.getLink().getFromNode().getCoord().getX())*0.5 - this.minEasting;
    			double middleNorth = (link.getLink().getToNode().getCoord().getY() + link.getLink().getFromNode().getCoord().getY())*0.5 - this.minNorthing;

    			for (OTFWriterFactory<VisLink> fac : linkWriterFactoryObjects) {
    				OTFDataWriter<VisLink> writer = fac.getWriter();
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
