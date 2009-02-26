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


import org.apache.log4j.Logger;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.mobsim.queuesim.PersonAgent;

public class MyPersonAgent extends PersonAgent{
	
	private static final Logger log = Logger.getLogger(MyPersonAgent.class);
	
	public MyPersonAgent(Person p)
	{
		super(p);
	}

	public void UpdateCachedNextLink()
	{

	}
	
	@Override
	public Link chooseNextLink()
	{	
		/*
		 * Delete cached Link. If a Person is in the Waiting Queue to leave a
		 * Link he/she may replan his/her Route so the cached Link would be wrong.
		 */
		super.cachedNextLink = null;
		
		return super.chooseNextLink();
		
	}
	
}
