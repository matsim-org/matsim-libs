/* *********************************************************************** *
 * project: org.matsim.*
 * HashPathFlow.java
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


public class HashPathFlow<V, E> implements PathFlow<V,E> {

	protected Map<Commodity<V>, Map<List<E>, Double>> pathFlow;

	public HashPathFlow()
	{
		this.pathFlow = new HashMap<Commodity<V>, Map<List<E>, Double>>();
	}
	
	public HashPathFlow(Map<Commodity<V>, Map<List<E>, Double>> pFlow){
		this.pathFlow = pFlow;
	}

	public boolean add(V from, V to, List<E> path, double f){
		for(Commodity<V> c: this.pathFlow.keySet()){
			if(c.origin.equals(from) && c.destination.equals(to))
				return this.add(c, path, f);
		}
		Commodity<V> com = new Commodity<V>(from, to , f);
		return this.add(com, path, f);
	}
	
	public boolean add(Commodity<V> c, List<E> path, double f)
	{
		if(this.pathFlow.get(c) == null)
			this.pathFlow.put(c, new HashMap<List<E>, Double>());
		Double d = this.pathFlow.get(c).get(path);
		return this.pathFlow.get(c).put(path, d == null ? f : f + d) != null;
	}

	/**
	 * Returns null if an Link e carries no flow
	 */
	public Map<E, Double> getArcFlowMap() {
		Map<E, Double> arcFlow = new HashMap<E,Double>();
		for(Commodity<V> c : this.pathFlow.keySet())
		{
			for(List<E> path : this.pathFlow.get(c).keySet())
			{
				for(E e: path)
				{
					Double d = arcFlow.get(e);
					if(d==null)
					{
						arcFlow.put(e, pathFlow.get(c).get(path));
					}
					else
					{
						arcFlow.put(e, d + pathFlow.get(c).get(path));
					}
				}
			}
		}
		return arcFlow;
	}

	public Commodity<V> getCommodity(V from, V to){
		for(Commodity<V> c: this.getCommodities()){
			if(c.getOrigin() == from && c.getDestination() == to)
				return c;
		}
		return null;
	}
	
	public Set<Commodity<V>> getCommodities() {
		return this.pathFlow.keySet();
	}

	public Set<List<E>> getFlowPaths(V from, V to) {
		Commodity<V> c = this.getCommodity(from, to);
		return c==null ? null: this.getFlowPaths(c);
	}
	
	public Set<List<E>> getFlowPaths(Commodity<V> c) {
		return this.pathFlow.get(c)==null ? null : this.pathFlow.get(c).keySet();
	}

	public Double getFlowValue(Commodity<V> c, List<E> path) {
		return this.pathFlow.get(c).get(path);
	}

	public String getArcFlowXMLString(int leadingTabs)
	{
		int otab = leadingTabs;
		String tab="";
		while(otab-- > 0)
			tab+='\t';
		StringBuilder res = new StringBuilder();
		res.append(tab+"<arcflow>\n");
		Map<E, Double> flow = this.getArcFlowMap();
		for(E l : flow.keySet())
		{
			res.append(	tab+"\t<link id=\""+l+"\">\n" +
					tab+"\t\t<flow>"+flow.get(l)+"</flow>\n" +
					tab+"\t</link>\n");
		}
		res.append(tab+"</arcflow>\n");
		return res.toString();
	}

	public String getPathFlowXMLString(int leadingTabs){
		int otabs = leadingTabs;
		String tab = "";
		while(otabs-- > 0)
			tab+='\t';
		StringBuilder res = new StringBuilder();
		res.append(	tab+"<pathflow>\n");
		for(Commodity<V> c : this.getCommodities())
		{
			res.append(tab+"\t<commodity origin=\""+c.getOrigin()+"\" "+
					"destination=\""+c.getDestination()+"\" " +
					"demand=\""+c.getDemand()+"\">\n");
			for(List<E> path : this.getFlowPaths(c))
			{
				res.append(	tab+"\t\t<path flow=\""+this.getFlowValue(c, path)+"\">\n" +
						tab+"\t\t\t"+path+"\n"+
						tab+"\t\t</path>\n");
			}
			res.append(tab+"\t</commodity>\n");
		}
		res.append( tab +"</pathflow>\n" );
		return res.toString();
	}

	public static HashPathFlow<String, String> parseCMCFFlow(String file)	throws IOException
	{
		HashPathFlow<String, String> pFlow = new HashPathFlow<String, String>();
		BufferedReader in = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			/***
			 * Example of a line which has to be extracted:
			 * Flow 7.5 on path 1: 2 -> 12 (15000, 2): 7 16
			 */
			if(line.startsWith("Flow")){
				line = line.substring(5);
				Double flow = new Double(line.substring(0, line.indexOf(' ')));
				line = line.substring(line.indexOf(':')+2);
				String fromID = line.substring(0, line.indexOf(" ->"));
				line = line.substring(line.indexOf("-> ")+3);
				String toID = line.substring(0,line.indexOf(" ("));
				String pathString = line.substring(line.indexOf("): ")+3).trim();

				List<String> path = new LinkedList<String>();
				StringTokenizer st = new StringTokenizer(pathString);
				while(st.hasMoreTokens()){
					path.add(st.nextToken());
				}
				pFlow.add(fromID, toID, path, flow);
			}
		}

		return pFlow;
	}
}
