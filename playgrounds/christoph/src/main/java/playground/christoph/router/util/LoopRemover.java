/* *********************************************************************** *
 * project: org.matsim.*
 * LoopRemover.java
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

package playground.christoph.router.util;

import java.util.List;

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class LoopRemover {

	/**
	 * Removes loops within a route. This is done by searching duplicated 
	 * nodes / links and removing the route between them.
	 */
	public void removeLoops(Path path)
	{
		/*
		 * Compare nodes and not links. 
		 * Maybe a Problem: two nodes could be connected with more than one link.
		 * It's a question of the definition of a "loop" if comparing links would be better...
		 */ 		 	
		List<Node> newNodes = path.nodes;
				
		for(int i = 0; i < newNodes.size(); i++)
		{		
			/*
			 * Start looking for duplicated links from the end -> find the "biggest" loop, if there
			 * is more than one.
			 */
			int loopLength = newNodes.lastIndexOf(newNodes.get(i)) - i;
			if (loopLength != 0)
			{
				for (int j = 0; j < loopLength; j++) newNodes.remove(i);
				
//				log.info("Removed " + loopLength + " nodes form the route!");
			}

		}
	
	}	// removeLoops
	
}
