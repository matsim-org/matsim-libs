/* *********************************************************************** *
 * project: org.matsim.*
 * DisplayNet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.netvis.visNet;

import java.util.Iterator;

import org.matsim.basic.v01.BasicLinkSet;
import org.matsim.basic.v01.BasicNodeSet;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.interfaces.networks.basicNet.BasicLinkSetI;
import org.matsim.interfaces.networks.basicNet.BasicNetI;
import org.matsim.interfaces.networks.basicNet.BasicNodeI;
import org.matsim.interfaces.networks.basicNet.BasicNodeSetI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

/**
 * @author gunnar
 * 
 */
public class DisplayNet implements BasicNetI {

    // -------------------- MEMBER VARIABLES --------------------

    private double minEasting;
    private double maxEasting;
    private double minNorthing;
    private double maxNorthing;

    private final BasicNodeSet nodes = new BasicNodeSet();
    private final BasicLinkSet links = new BasicLinkSet();
 
    // -------------------- CONSTRUCTION --------------------

    public DisplayNet(NetworkLayer layer) {
    	// first create nodes
    	for (Iterator it = layer.getNodes().iterator(); it.hasNext();) {
  			BasicNodeI node = (BasicNodeI) it.next();

 				DisplayNode node2 = new DisplayNode(node.getId(), this);
 				node2.setCoord(((Node)node).getCoord());

 				nodes.add(node2);
  		}
    	
    	// second, create links
    	for (Iterator it = layer.getLinks().iterator(); it.hasNext();) {
  			BasicLinkI link = (BasicLinkI) it.next();
 				DisplayLink link2 = new DisplayLink(link.getId(), this);

  				BasicNodeI from = (BasicNodeI) this.getNodes().get(link.getFromNode().getId());
  				from.addOutLink(link2);
  				link2.setFromNode(from);

  				BasicNodeI to = (BasicNodeI) this.getNodes().get(link.getToNode().getId());
  				to.addInLink(link2);
  				link2.setToNode(to);
  				
          link2.setLength_m(((Link)link).getLength());
          link2.setLanes(((Link)link).getLanes());

 					links.add(link2);
  		}
    	
    	// third, build/complete the network
    	this.build();
    }
    
    public void clear() {
        Iterator it = getLinks().iterator();
        while (it.hasNext())
            ((DisplayLink) it.next()).clear();
    }

 // -------------------- IMPLEMENTATION OF BasicNetworkI --------------------

    public void connect() {
    }

    public BasicNodeSetI getNodes() {
        return nodes;
    }

    public BasicLinkSetI getLinks() {
        return links;
    }
    
    // -------------------- OVERRIDING OF TrafficNet --------------------

    public void build() {
      for (Iterator it = getLinks().iterator(); it.hasNext();)
        ((DisplayLink) it.next()).build();

        minEasting = Double.POSITIVE_INFINITY;
        maxEasting = Double.NEGATIVE_INFINITY;
        minNorthing = Double.POSITIVE_INFINITY;
        maxNorthing = Double.NEGATIVE_INFINITY;

        for (Iterator it = getNodes().iterator(); it.hasNext();) {
            DisplayNode node = (DisplayNode) it.next();
            minEasting = Math.min(minEasting, node.getEasting());
            maxEasting = Math.max(maxEasting, node.getEasting());
            minNorthing = Math.min(minNorthing, node.getNorthing());
            maxNorthing = Math.max(maxNorthing, node.getNorthing());
        }
    }

    public double minEasting() {
        return minEasting;
    }

    public double maxEasting() {
        return maxEasting;
    }

    public double minNorthing() {
        return minNorthing;
    }

    public double maxNorthing() {
        return maxNorthing;
    }

}