package playground.dressler.ea_flow;

/* *********************************************************************** *
 * project: org.matsim.*
 * VirtualSink.java
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

import org.matsim.api.core.v01.network.Node;

public class VirtualSink extends VirtualNode {
   //public int time;
   public Node node;
   
   VirtualSink (Node node) {
	   this.node = node;
   }
   
   @Override
   public int getRealTime() {
	   return 0; //this.time;
   }
   
   @Override
   public Node getRealNode() {
	   return this.node;
   }
  
   @Override
   public boolean equals(VirtualNode other) {
	   if (other instanceof VirtualSource) {
		   VirtualSource o = (VirtualSource) other;
		   //if (this.time != o.time) return false;
		   if (this.node != null) {
			   return this.node.equals(o.node);
		   } else {
			  return (o.node == null);
		   }			   		  
	   }
	   return false;
   }
   
   @Override
   public String toString() {
     return "Virtual Sink " + node.getId().toString();	   
   }
}


