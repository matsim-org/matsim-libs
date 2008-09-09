/* *********************************************************************** *
 * project: org.matsim.*
 * MyPersonAgent.java
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
package playground.christoph.mobsim;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.population.Person;

public class MyPersonAgent extends PersonAgent{

	private static final Logger log = Logger.getLogger(MyPersonAgent.class);
	
	public MyPersonAgent(Person p)
	{
		super(p);
	}
	
	public void UpdateCachedNextLink()
	{

	}
	
	public Link chooseNextLink()
	{	
		
		return super.chooseNextLink();
		
		
/*		Funktionalität wird vermutlich nicht mehr benötigt... to be removed...		
 
		// Falls überhaupt ein Wert hinterlegt ist
		if (super.cachedNextLink != null)
		{
//			System.exit(0);
			ArrayList<Node> route = this.getCurrentLeg().getRoute().getRoute();
			
			int index = this.getCurrentNodeIndex();
			
			// Element vorhanden?
			if(route.size() >= index + 1)
			{
				// Hast sich der NextLink verändert?
				if(!cachedNextLink.getToNode().equals(route.get(index + 1)))
				{
					log.error("NextLink has changed! - Update required!");
					
					Node fromNode = route.get(index);
					Node toNode = route.get(index + 1);
					
					for (Link link : fromNode.getOutLinks().values()) 
					{
						if (link.getToNode() == toNode) 
						{
							cachedNextLink = link; //save time in later calls, if link is congested
							log.info("Updated nextLink successfully");
						}				
					}
				}
		
			}
		
//		log.info("Updated nextLink successfully");
		}
		return super.chooseNextLink();
*/	
	}

}
