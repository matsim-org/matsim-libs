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

import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;

import playground.christoph.mobsim.MyQueueNetwork;

public abstract class PersonLeastCostPathCalculator implements LeastCostPathCalculator, Cloneable{
	
	protected PersonImpl person;
	protected MyQueueNetwork myQueueNetwork;
	protected double time;
	
	public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, PersonImpl person)
	{
		this.person = person;
		
		return calcLeastCostPath(fromNode, toNode, starttime);
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
	
	public void setTime(double time)
	{
		this.time = time;
	}
	
	public double getTime()
	{
		return this.time;
	}
	
	public static int getErrorCounter()
	{
		return 0;
	}
	
	public static void setErrorCounter(int i)
	{
		
	}
	
	@Override
	public PersonLeastCostPathCalculator clone()
	{
		return this;
	}
}
