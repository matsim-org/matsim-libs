/* *********************************************************************** *
 * project: org.matsim.*
 * Values.java
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

class Values {
	enum Entry { amount, km, hrs, mon }

	enum Type { GV, PV }

	enum Mode { road, rail }
	
	Map<Mode,ValuesForAMode> valuesByMode = new TreeMap<Mode,ValuesForAMode>() ;
	Values() {
		for ( Mode mode : Mode.values() ) {
			ValuesForAMode vals = new ValuesForAMode() ;
			valuesByMode.put( mode, vals ) ;
		}
	}
	Values createDeepCopy( ) {
		Values planfall = new Values() ;
		for ( Mode mode : Mode.values() ) {
			ValuesForAMode oldValues = this.getByMode(mode) ;
			ValuesForAMode newValues = oldValues.createDeepCopy() ;
			planfall.valuesByMode.put( mode, newValues ) ;
		}
		return planfall ; 
	}
	ValuesForAMode getByMode( Mode mode ) {
			return valuesByMode.get(mode) ;
	}
	void setValuesForMode( Mode mode, ValuesForAMode values ) {
		valuesByMode.put( mode, values ) ;
	}
}

