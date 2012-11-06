/* *********************************************************************** *
 * project: org.matsim.*
 * ValueComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.utils.weightedRandom;

import java.util.Comparator;
import java.util.Map;


/**
 * @author Ihab
 *
 */
public class ValueComparator implements Comparator {
	private Map map;
	
	public ValueComparator (Map map){
		this.map = map;
	}
	
	public int compare(Object o1, Object o2) {
		
		if ((Double)this.map.get(o1) > (Double)this.map.get(o2)) return 1;
		if ((Double)this.map.get(o1) < (Double)this.map.get(o2)) return -1;
		return 0;
	}
}
