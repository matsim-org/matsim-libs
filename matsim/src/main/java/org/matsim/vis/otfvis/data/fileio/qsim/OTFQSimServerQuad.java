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
package org.matsim.vis.otfvis.data.fileio.qsim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QNode;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDataWriter;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.data.OTFWriterFactory;


/**
 * @author dgrether
 *
 */
public class OTFQSimServerQuad extends OTFServerQuad2 {

	private static final long serialVersionUID = 23L;
	
  private static final Logger log = Logger.getLogger(OTFQSimServerQuad.class);
	
  transient private QNetwork net;
  /**
	 * 
	 */
	public OTFQSimServerQuad(QNetwork net) {
		super(net.getNetworkLayer());
		this.net = net;
	}

	@Override
	public void initQuadTree(OTFConnectionManager connect) {
		createFactoriesAndFillQuadTree(connect);
	}

	private void createFactoriesAndFillQuadTree(OTFConnectionManager connect) {
		// Get the writer Factories from connect
		Collection<Class> nodeFactories = connect.getToEntries(QNode.class);
		Collection<Class> linkFactories =  connect.getToEntries(QLink.class);
		List<OTFWriterFactory<QLink>> linkWriterFactoryObjects = new ArrayList<OTFWriterFactory<QLink>>(linkFactories.size());
		List<OTFWriterFactory<QNode>> nodeWriterFractoryObjects = new ArrayList<OTFWriterFactory<QNode>>(nodeFactories.size());
		try {
			OTFWriterFactory<QLink> linkWriterFac = null;			
			for (Class linkFactory : linkFactories ) {
				if(linkFactory != Object.class) {
					linkWriterFac = (OTFWriterFactory<QLink>)linkFactory.newInstance();
					linkWriterFactoryObjects.add(linkWriterFac);
				}
			}
			OTFWriterFactory<QNode> nodeWriterFac = null;
			for (Class nodeFactory : nodeFactories) {
				if(nodeFactory != Object.class) {
					nodeWriterFac = (OTFWriterFactory<QNode>)nodeFactory.newInstance();
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
    		for (QNode node : this.net.getNodes().values()) {
    			for (OTFWriterFactory<QNode> fac : nodeWriterFractoryObjects) {
    				OTFDataWriter<QNode> writer = fac.getWriter();
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
    		for (QLink link : this.net.getLinks().values()) {
    			double middleEast = (link.getLink().getToNode().getCoord().getX() + link.getLink().getFromNode().getCoord().getX())*0.5 - this.minEasting;
    			double middleNorth = (link.getLink().getToNode().getCoord().getY() + link.getLink().getFromNode().getCoord().getY())*0.5 - this.minNorthing;

    			for (OTFWriterFactory<QLink> fac : linkWriterFactoryObjects) {
    				OTFDataWriter<QLink> writer = fac.getWriter();
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

	public void replaceSrc(QNetwork newNet) {
		//int colls = 
		this.execute(0.,0.,this.easting, this.northing,
				new ReplaceSourceExecutor(newNet));
	}
	
	private static class ReplaceSourceExecutor implements Executor<OTFDataWriter> {
		public final QNetwork q;

		public ReplaceSourceExecutor(QNetwork newNet) {
			this.q = newNet;
		}

		public void execute(double x, double y, OTFDataWriter writer)  {
			Object src = writer.getSrc();
			if(src instanceof QLink) {
				QLink link = this.q.getLinks().get(((QLink) src).getLink().getId());
				writer.setSrc(link);
//			} else if(src instanceof QueueNode) {
//				
			}
		}
	}
	
}
