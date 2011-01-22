/* *********************************************************************** *
 * project: org.matsim.*
 * VirtualNode.java
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

import playground.dressler.network.IndexedNodeI;

public abstract class VirtualNode {
	
   public abstract int getRealTime();
  
   public abstract IndexedNodeI getRealNode();
  
   public abstract boolean equals(VirtualNode other);
   
   public abstract String toString();
   
   // to help with the total order for the BFTask-Comparator ...
   // source = 0
   // normalnode = 1
   // sink = 2
   public abstract int priority();
}
