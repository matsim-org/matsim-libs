/* *********************************************************************** *
 * project: org.matsim.*
 * Commodities.java
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

package playground.msieg.structure;

import java.util.HashSet;

public class Commodities<V> extends HashSet<Commodity<V>> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6401857368450594778L;

	public Commodities(){
		super();
	}
	
	public Commodity<V> add(V s, V t, Number d){
		//check if there is already one commodity
		for(Commodity<V> c: this)
		{
			if(c.origin == s && c.destination == t)
			{
				c.demand = c.demand.doubleValue() + d.doubleValue();
				return c;
			}
		}
		Commodity<V> c = new Commodity<V>(s,t,d);
		add(c);
		return c;
	}

	public V getDest(Commodity<V> c){
		return c.destination;
	}
	
	public V getOrigin(Commodity<V> c){
		return c.origin;
	}
	
	public Number getDemand(Commodity<V> c){
		return c.demand;
	}
}
