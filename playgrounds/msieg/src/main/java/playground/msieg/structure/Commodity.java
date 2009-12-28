/* *********************************************************************** *
 * project: org.matsim.*
 * Commodity.java
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

public class Commodity<V> {

	protected final V origin, destination;
	protected Number demand;
	
	public Commodity(V origin, V destination, Number demand) {
		super();
		this.origin = origin;
		this.destination = destination;
		this.demand = demand;
	}

	public Number getDemand() {
		return demand;
	}

	public void setDemand(Number demand) {
		this.demand = demand;
	}

	public V getDestination() {
		return destination;
	}

	public V getOrigin() {
		return origin;
	}
	
	public String toXMLString(String id, int leadingTabs){
		String tab="";
		while(leadingTabs-- > 0)
			tab += '\t';
		String res = 	tab+"<commodity id=\""+id+"\">\n"+
						tab+"\t<from>"+origin.toString()+"</from>\n" +
						tab+"\t<to>"+destination.toString()+"</to>\n" +
						tab+"\t<demand>"+demand.toString()+"</demand>\n" +
						tab+"</commodity>\n";
		return res;
	}
	
	@Override
	public String toString(){
		return "Commodity: "+this.origin+" -> "+this.demand+" ("+this.demand+")";
	}

}
