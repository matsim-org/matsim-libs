/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelTime.java
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

import org.matsim.core.api.network.Link;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.mobsim.MyQueueNetwork;

public abstract class KnowledgeTravelTime implements TravelTime, Cloneable {

	protected PersonImpl person;
	protected MyQueueNetwork myQueueNetwork;
	
	public double getLinkTravelTime(Link link, double time, PersonImpl person)
	{
		this.person = person;
		return getLinkTravelTime(link, time);
	}
	
	public void setPerson(PersonImpl person)
	{
		this.person = person;
	}
	
	public PersonImpl getPerson()
	{
		return this.person;
	}
	
	public void setMyQueueNetwork(MyQueueNetwork myQueueNetwork)
	{
		this.myQueueNetwork = myQueueNetwork;
	}
	
	public MyQueueNetwork getMyQueueNetwork()
	{
		return this.myQueueNetwork;
	}
	
	@Override
	public KnowledgeTravelTime clone()
	{
		return this;
	}
}