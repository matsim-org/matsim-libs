/* *********************************************************************** *
 * project: org.matsim.*
 * ValuesByMode.java
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

class ValuesForAMode {
	Map<Type,ValuesForAUserType> valuesByType = new TreeMap<Type,ValuesForAUserType>() ;
	ValuesForAMode createDeepCopy( ) {
		ValuesForAMode planfall = new ValuesForAMode() ;
		for ( Type mode : Type.values() ) {
			ValuesForAUserType old = this.getByType(mode) ;
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
	ValuesForAUserType getByType( Type type ) {
			return valuesByType.get(type) ;
	}
	void setValuesForType( Type type, ValuesForAUserType values ) {
		valuesByType.put( type, values ) ;
	}
}