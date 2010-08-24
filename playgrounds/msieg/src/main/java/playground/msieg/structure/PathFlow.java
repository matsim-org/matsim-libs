/* *********************************************************************** *
 * project: org.matsim.*
 * PathFlow.java
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

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This is to define a pathbased flow representation of an flow
 * A path flow is expressed as a set of flows for any commodity 
 * instance, that is to any commodity there is an associated set
 * of Paths (given as an ordered set of links) and to every path
 * an associated flow value (most likely steored as double).
 * 
 * 
 * @author msieg
 *
 * @param <V>
 * @param <E>
 */

public interface PathFlow<V,E> {

	public boolean add(V from, V to, List<E> path, double f);
	public boolean add(Commodity<V> c, List<E> path, double f);
	
	public Commodity<V> getCommodity(V from, V to);
	public Set<Commodity<V>> getCommodities();
	
	public Set<List<E>> getFlowPaths(V from, V to);
	public Set<List<E>> getFlowPaths(Commodity<V> c);
	
	public Double getFlowValue(Commodity<V> c, List<E> path);
	
	public Map<E, Double> getArcFlowMap();
	
	public String getArcFlowXMLString(int leadingTabs);
	
	public String getPathFlowXMLString(int leadingTabs);
}
