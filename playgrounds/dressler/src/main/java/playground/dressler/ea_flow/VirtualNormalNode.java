/* *********************************************************************** *
 * project: org.matsim.*
 * VirtualNormalNode.java
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

package playground.dressler.ea_flow;

import org.matsim.api.core.v01.network.Node;

public class VirtualNormalNode extends VirtualNode {
   public int time;
   public Node node;
   
   VirtualNormalNode (Node node, int time) {
	   this.time = time;
	   this.node = node;
   }
   
   @Override
   public int getRealTime() {
	   return this.time;
   }
   
   @Override
   public Node getRealNode() {
	   return this.node;
   }
  
   @Override
   public boolean equals(VirtualNode other) {
	   if (other instanceof VirtualNormalNode) {
		   VirtualNormalNode o = (VirtualNormalNode) other;
		   if (this.time != o.time) return false;
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
     return "Normal node " + node.getId().toString();	   
   }
}
