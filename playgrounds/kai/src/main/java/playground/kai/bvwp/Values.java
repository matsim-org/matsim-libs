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

import playground.kai.bvwp.Values.Type;
import playground.kai.bvwp.Values.Entry;

class Values {
	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "ODAttribute".  "Mengen" sind vielleicht nicht wirklich "Attribute" im Sinne der BVWP-Nomenklatur,
	 * aber abgesehen davon spricht eigentlich nichts dagegen.  kai,benjamin, sep'12
	 * </ul>
	 */
	enum Entry { XX, km, hrs, mon }

	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "DemandSegment".  But refactoring does not seem to work.  Try with other eclipse (this one was
	 * Galileo).  kai/benjamin, sep'12
	 *</ul>
	 */
	enum Type { GV, PV_NON_COMMERCIAL, PV_COMMERCIAL }

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

class ValuesForAMode {
	Map<Type,ValuesForAUserType> valuesByType = new TreeMap<Type,ValuesForAUserType>() ;
	ValuesForAMode createDeepCopy( ) {
		ValuesForAMode planfall = new ValuesForAMode() ;
		for ( Type mode : Type.values() ) {
			ValuesForAUserType old = this.getByDemandSegment(mode) ;
			ValuesForAUserType tmp2 = old.createDeepCopy() ;
			planfall.valuesByType.put( mode, tmp2 ) ;
		}
		return planfall ; 
	}
	ValuesForAMode() {
		for ( Type mode : Type.values() ) {
			ValuesForAUserType vals = new ValuesForAUserType() ;
			valuesByType.put( mode, vals ) ;
		}
	}
	ValuesForAUserType getByDemandSegment( Type type ) {
			return valuesByType.get(type) ;
	}
	void setValuesForType( Type type, ValuesForAUserType values ) {
		valuesByType.put( type, values ) ;
	}
}

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
