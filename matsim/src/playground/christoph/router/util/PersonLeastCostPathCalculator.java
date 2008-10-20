/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLeastCostPathCalculator.java
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
import org.matsim.network.Node;
import org.matsim.population.Person;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;

public abstract class PersonLeastCostPathCalculator implements LeastCostPathCalculator, Cloneable{
	
	protected Person person;
	protected QueueNetwork queueNetwork;
	
	public Route calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person)
	{
		this.person = person;
		
		return calcLeastCostPath(fromNode, toNode, starttime);
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
	
	@Override
	public PersonLeastCostPathCalculator clone()
	{
		return this;
	}
}
