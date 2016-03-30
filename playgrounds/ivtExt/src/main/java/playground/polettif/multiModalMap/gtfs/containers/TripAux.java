/* *********************************************************************** *
 * project: org.matsim.*
 * TripAux.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.gtfs.containers;

import java.util.HashMap;
import java.util.Map;

public class TripAux {
	private Map<String,Integer> firsts;
	private Map<String,Integer> lasts;
	private String line;
	/**
	 * 
	 */
	public TripAux() {
		super();
		firsts=new HashMap<String,Integer>();
		lasts=new HashMap<String,Integer>();
	}
	public void addFirst(String first) {
		if(firsts.get(first)==null)
			firsts.put(first, 1);
		else
			firsts.put(first, firsts.get(first)+1);
	}
	public void addLast(String last) {
		if(lasts.get(last)==null)
			lasts.put(last, 1);
		else
			lasts.put(last, lasts.get(last)+1);
	}
	/**
	 * @return the firsts
	 */
	public Map<String, Integer> getFirsts() {
		return firsts;
	}
	/**
	 * @return the lasts
	 */
	public Map<String, Integer> getLasts() {
		return lasts;
	}
	/**
	 * @return the line
	 */
	public String getLine() {
		return line;
	}
	/**
	 * @param line the line to set
	 */
	public void setLine(String line) {
		this.line = line;
	}
	
}
