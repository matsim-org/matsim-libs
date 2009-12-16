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
import org.matsim.core.mobsim.queuesim.QueueNode;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad;
import org.matsim.vis.otfvis.data.OTFWriterFactory;


/**
 * @author dgrether
 *
 */
public class OTFQueueSimServerQuad extends OTFServerQuad {

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
		Collection<Class> nodeFactories = connect.getToEntries(QueueNode.class);
		Collection<Class> linkFactories =  connect.getToEntries(QueueLink.class);
		List<OTFWriterFactory<QueueLink>> linkWriterFactoryObjects = new ArrayList<OTFWriterFactory<QueueLink>>(linkFactories.size());
		List<OTFWriterFactory<QueueNode>> nodeWriterFractoryObjects = new ArrayList<OTFWriterFactory<QueueNode>>(nodeFactories.size());
		try {
			OTFWriterFactory<QueueLink> linkWriterFac = null;			
			for (Class linkFactory : linkFactories ) {
				if(linkFactory != Object.class) {
					linkWriterFac = (OTFWriterFactory<QueueLink>)linkFactory.newInstance();
					linkWriterFactoryObjects.add(linkWriterFac);
				}
			}
			OTFWriterFactory<QueueNode> nodeWriterFac = null;
			for (Class nodeFactory : nodeFactories) {
				if(nodeFactory != Object.class) {
					nodeWriterFac = (OTFWriterFactory<QueueNode>)nodeFactory.newInstance();
					nodeWriterFractoryObjects.add(nodeWriterFac);
				}
			}
			
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

    	if(!nodeWriterFractoryObjects.isEmpty()) {
    		boolean first = true;
    		for (QueueNode node : this.net.getNodes().values()) {
    			for (OTFWriterFactory<QueueNode> fac : nodeWriterFractoryObjects) {
    				OTFDataWriter<QueueNode> writer = fac.getWriter();
    				if (writer != null) {
    					writer.setSrc(node);
    					if (first){
    						log.info("Connecting Source QueueNode with " + writer.getClass().getName());
    						first = false;
    					}
    				}
    				this.put(node.getNode().getCoord().getX() - this.minEasting, node.getNode().getCoord().getY() - this.minNorthing, writer);
    			}
    		}
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

	public void replaceSrc(QueueNetwork newNet) {
		//int colls = 
		this.execute(0.,0.,this.maxEasting - this.minEasting,this.maxNorthing - this.minNorthing,
				new ReplaceSourceExecutor(newNet));
	}
	
	private static class ReplaceSourceExecutor implements Executor<OTFDataWriter> {
		public final QueueNetwork q;

		public ReplaceSourceExecutor(QueueNetwork newNet) {
			this.q = newNet;
		}

		public void execute(double x, double y, OTFDataWriter writer)  {
			Object src = writer.getSrc();
			if(src instanceof QueueLink) {
				QueueLink link = this.q.getLinks().get(((QueueLink) src).getLink().getId());
				writer.setSrc(link);
//			} else if(src instanceof QueueNode) {
//				
			}
		}
	}
	
}
