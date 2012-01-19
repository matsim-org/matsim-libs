/* *********************************************************************** *
 * project: org.matsim.*
 * ValuesByType.java
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
package playground.kai.bvwp;

import java.util.Map;
import java.util.TreeMap;

import playground.kai.bvwp.Values.Entry;

class ValuesForAUserType {
	Map<Entry,Double> quantities = new TreeMap<Entry,Double>() ;
	ValuesForAUserType() {
		for ( Entry entry : Entry.values() ) {
			this.setByEntry( entry, 0. ) ;
		}
	}
	double getByEntry( Entry entry ) {
		return quantities.get(entry) ;
	}
	void setByEntry( Entry entry, double dbl ) {
		quantities.put( entry, dbl ) ;
	}
	void incByEntry( Entry entry, double dbl ) {
		double tmp = quantities.get( entry ) ;
		quantities.put( entry, tmp + dbl ) ;
	}
	ValuesForAUserType createDeepCopy() {
		ValuesForAUserType newValues = new ValuesForAUserType() ;
		for ( Entry entry : Entry.values() ) {
			newValues.setByEntry( entry, this.getByEntry(entry) ) ;
		}
		return newValues ;
	}
	
}