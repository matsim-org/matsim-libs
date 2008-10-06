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

import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.Link;
import org.matsim.population.Person;
import org.matsim.router.util.TravelTime;

public abstract class KnowledgeTravelTime implements TravelTime {

	protected Person person;
	protected QueueNetwork queueNetwork;
	
	public double getLinkTravelTime(Link link, double time, Person person)
	{
		this.person = person;
		return getLinkTravelTime(link, time);
	}
	
	public void setPerson(Person person)
	{
		this.person = person;
	}
	
	public Person getPerson()
	{
		return this.person;
	}
	
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}
	
	public QueueNetwork getQueueNetwork()
	{
		return this.queueNetwork;
	}
}