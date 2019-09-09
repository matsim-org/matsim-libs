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
package playground.vsp.zzArchive.bvwpOld;

import java.util.Map;
import java.util.TreeMap;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;

@Deprecated
class Values {
	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "ODAttribute".  "Mengen" sind vielleicht nicht wirklich "Attribute" im Sinne der BVWP-Nomenklatur,
	 * aber abgesehen davon spricht eigentlich nichts dagegen.  kai,benjamin, sep'12
	 * </ul>
	 */
	enum Attribute { XX, km, hrs, priceUser, costOfProduction, excess_hrs }

	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "DemandSegment".  But refactoring does not seem to work.  Try with other eclipse (this one was
	 * Galileo).  kai/benjamin, sep'12
	 *</ul>
	 */
	enum DemandSegment { GV, PV_NON_COMMERCIAL, PV_COMMERCIAL }

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
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder() ;
		for ( Mode mode : Mode.values() ) {
			ValuesForAMode valForMode = this.getByMode(mode) ;
			for ( DemandSegment demandSegment : DemandSegment.values() ) {
				str.append( "--> " + mode + "; " + demandSegment + " : " ) ;
				Attributes valByDemandSegment = valForMode.getByDemandSegment(demandSegment);
				for ( Attribute attribute : Attribute.values() ) {
					str.append( attribute.toString() + ": " + valByDemandSegment.getByEntry(attribute) + "; " ) ;
				}
				str.append( "\n" ) ;
			}
			
		}
		return str.toString() ;
	}
}

@Deprecated
class ValuesForAMode {
	Map<DemandSegment,Attributes> valuesByType = new TreeMap<DemandSegment,Attributes>() ;
	ValuesForAMode createDeepCopy( ) {
		ValuesForAMode planfall = new ValuesForAMode() ;
		for ( DemandSegment mode : DemandSegment.values() ) {
			Attributes old = this.getByDemandSegment(mode) ;
			Attributes tmp2 = old.createDeepCopy() ;
			planfall.valuesByType.put( mode, tmp2 ) ;
		}
		return planfall ; 
	}
	ValuesForAMode() {
		for ( DemandSegment mode : DemandSegment.values() ) {
			Attributes vals = new Attributes() ;
			valuesByType.put( mode, vals ) ;
		}
	}
	Attributes getByDemandSegment( DemandSegment demandSegment ) {
			return valuesByType.get(demandSegment) ;
	}
	void setValuesForType( DemandSegment demandSegment, Attributes values ) {
		valuesByType.put( demandSegment, values ) ;
	}
}

@Deprecated
class Attributes {
	Map<Attribute,Double> quantities = new TreeMap<Attribute,Double>() ;
	Attributes() {
		for ( Attribute attribute : Attribute.values() ) {
			this.setByEntry( attribute, 0. ) ;
		}
	}
	double getByEntry( Attribute attribute ) {
		return quantities.get(attribute) ;
	}
	void setByEntry( Attribute attribute, double dbl ) {
		quantities.put( attribute, dbl ) ;
	}
	void incByEntry( Attribute attribute, double dbl ) {
		double tmp = quantities.get( attribute ) ;
		quantities.put( attribute, tmp + dbl ) ;
	}
	Attributes createDeepCopy() {
		Attributes newValues = new Attributes() ;
		for ( Attribute attribute : Attribute.values() ) {
			newValues.setByEntry( attribute, this.getByEntry(attribute) ) ;
		}
		return newValues ;
	}
	
}
