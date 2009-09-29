/* *********************************************************************** *
 * project: org.matsim.*
 * RandomCompassRoute.java
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

package playground.christoph.network.util;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;

import playground.christoph.network.SubNetwork;

public class SubNetworkTools {

	private final static Logger log = Logger.getLogger(SubNetworkTools.class);
	
	/*
	 * Returns a Map of Nodes, if the Person has Knowledge about known Nodes. 
	 */
	public SubNetwork getSubNetwork(Person person)
	{		
		// Try getting knowledge from the current Person.
		if(person != null)
		{		
			Map<String,Object> customAttributes = person.getCustomAttributes();
					
			if(customAttributes.containsKey("SubNetwork"))
			{
				SubNetwork subNetwork = (SubNetwork)customAttributes.get("SubNetwork");
			
				return subNetwork;
			}
			else
			{
				log.error("SubNetwork Object was not found in Person's Custom Attributes!");
			}
		}
		else
		{
			log.error("person = null!");
		}
		
		return null;
	}
	
	/*
	 * To save memory, some routers may want to reset a Person's SubNetwork after
	 * doing their routing. An Example would be a Random Router that does only an
	 * initial planning before starting the mobsim.
	 * Reset means, that Nodes, Links, etc. are deleted but the SubNetwork Object
	 * still exists. It can be recreated by using the Person's NodeKnowledge.
	 */ 
	public void resetSubNetwork(Person person)
	{		
		// Try getting knowledge from the current Person.
		if(person != null)
		{	
			Map<String,Object> customAttributes = person.getCustomAttributes();
			
			if (customAttributes.get("SubNetwork") != null && customAttributes.get("SubNetwork") instanceof SubNetwork)
			{
				SubNetwork subNetwork = (SubNetwork)customAttributes.get("SubNetwork");
				subNetwork.reset();
			}
		}
		else
		{
			log.error("person = null!");
		}
	}
	
	/*
	 * To save memory, some routers may want to remove a Person's SubNetwork after
	 * doing their routing. An Example would be a Random Router that does only an
	 * initial planning before starting the mobsim.
	 */ 
	public void removeSubNetwork(Person person)
	{		
		// Try getting knowledge from the current Person.
		if(person != null)
		{	
			Map<String,Object> customAttributes = person.getCustomAttributes();
			
			customAttributes.put("SubNetwork", null);
		}
		else
		{
			log.error("person = null!");
		}
	}
}
