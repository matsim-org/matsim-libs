/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesTest.java
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

package playground.kai.bvwp;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class BvwpTest extends MatsimTestCase {
	/**
	 * Design comments:<ul>
	 * <li> One could have these also in a (renamed) "OutputFromPrognosis" data structure.  Probably
	 * more flexible, but more difficult to read.
	 * </ul>
	 *
	 */
	static class EconomicValues {
		double valueOfTimePV_Eu_h = 6. ;
		double valueOfDistancePV_Eu_km = 0.2 ;

		double valueOfTimeGV_Eu_h = 20. ;
		double valueOfDistanceGV_Eu_km = 1. ;
		
	}
	
	enum Entry { amount, km, hrs, mon } ;
	
	static class ValuesByType {
		Map<Entry,Double> quantities = new TreeMap<Entry,Double>() ;
		ValuesByType() {
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
		ValuesByType createDeepCopy() {
			ValuesByType newValues = new ValuesByType() ;
			for ( Entry entry : Entry.values() ) {
				newValues.setByEntry( entry, this.getByEntry(entry) ) ;
			}
			return newValues ;
		}
		
	}
	
	enum Type { GV, PV } ;
	
	static class ValuesByMode {
		Map<Type,ValuesByType> valuesByType = new TreeMap<Type,ValuesByType>() ;
		ValuesByMode createDeepCopy( ) {
			ValuesByMode planfall = new ValuesByMode() ;
			for ( Type mode : Type.values() ) {
				ValuesByType old = this.getByType(mode) ;
				ValuesByType tmp2 = old.createDeepCopy() ;
				planfall.valuesByType.put( mode, tmp2 ) ;
			}
			return planfall ; 
		}
		ValuesByMode() {
			for ( Type mode : Type.values() ) {
				ValuesByType vals = new ValuesByType() ;
				valuesByType.put( mode, vals ) ;
			}
		}
		ValuesByType getByType( Type type ) {
				return valuesByType.get(type) ;
		}
		void setValuesForType( Type type, ValuesByType values ) {
			valuesByType.put( type, values ) ;
		}
	}

	enum Mode { road, rail } ;

	static class Values {
		Map<Mode,ValuesByMode> valuesByMode = new TreeMap<Mode,ValuesByMode>() ;
		Values() {
			for ( Mode mode : Mode.values() ) {
				ValuesByMode vals = new ValuesByMode() ;
				valuesByMode.put( mode, vals ) ;
			}
		}
		Values createDeepCopy( ) {
			Values planfall = new Values() ;
			for ( Mode mode : Mode.values() ) {
				ValuesByMode oldValues = this.getByMode(mode) ;
				ValuesByMode newValues = oldValues.createDeepCopy() ;
				planfall.valuesByMode.put( mode, newValues ) ;
			}
			return planfall ; 
		}
		ValuesByMode getByMode( Mode mode ) {
				return valuesByMode.get(mode) ;
		}
		void setValuesForMode( Mode mode, ValuesByMode values ) {
			valuesByMode.put( mode, values ) ;
		}
	}
	
	static class ValuesByODRelation {
		Map<Id,Values> values = new TreeMap<Id,Values>();
		ValuesByODRelation() {
//			for ( Id id : values.keySet() ) {
//				Values vals = new Values() ;
//				values.put( id, vals ) ;
//			}
		}
		ValuesByODRelation createDeepCopy() {
			ValuesByODRelation nnn = new ValuesByODRelation() ;
			for ( Id id : values.keySet() ) {
				Values oldValues = this.getByODRelation(id) ;
				Values newValues = oldValues.createDeepCopy() ;
				nnn.values.put( id, newValues ) ;
			}
			return nnn ;
		}
		Values getByODRelation( Id id ) {
			return values.get(id) ;
		}
		void setValuesForODRelation( Id id , Values tmp ) {
			values.put( id, tmp ) ;
		}
	}


	public void testOne() {
		// yy if the following refers to one relation, it is fine.  if it refers to "everything", then we somehow need
		// to differentiate between old and new users.  kai, dec'11
		// yy are we communicating pkm, or p and km separately?? kai, dec'11
		
		Values economicValues = new Values() ;
		{
			ValuesByMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesByType pvValues = roadValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.km, 0.23 ) ;
				pvValues.setByEntry( Entry.hrs, 5.00 ) ;
			}
			{
				ValuesByType gvValues = roadValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.km, 1.00 ) ;
				gvValues.setByEntry( Entry.hrs, 20.00 ) ;
			}
			
			economicValues.setValuesForMode( Mode.rail, roadValues.createDeepCopy() ) ;
		}
		
		ValuesByODRelation nullfall = new ValuesByODRelation() ;

		Values nullfallForOD = new Values() ;
		{
			ValuesByMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				ValuesByType pvValues = roadValues.getByType(Type.PV) ;
				pvValues.setByEntry( Entry.amount, 1000. ) ;
				pvValues.setByEntry( Entry.km, 10. ) ;
				pvValues.setByEntry( Entry.hrs, 1. ) ;
			}
			{
				ValuesByType gvValues = roadValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.amount, 1000. ) ;
				gvValues.setByEntry( Entry.km, 10. ) ;
				gvValues.setByEntry( Entry.hrs, 1. ) ;
			}				

			ValuesByMode railValues = roadValues.createDeepCopy() ;
			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		nullfall.setValuesForODRelation(new IdImpl("AB"), nullfallForOD ) ;
		
		ValuesByODRelation planfall = nullfall.createDeepCopy() ;
		Values planfallForOD = planfall.getByODRelation(new IdImpl("AB")) ;
		{
			ValuesByMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByType(Type.PV).setByEntry( Entry.hrs, 0.9 ) ;
			railValues.getByType(Type.GV).setByEntry( Entry.hrs, 0.9 ) ;
		}
		
	}
	
	static void railImprovementBVWP03( Values nullfall, Values planfall ) {
		
	}

	static void railImprovementNew( Values economicValues, ValuesByODRelation nullfall, ValuesByODRelation planfall ) {
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )
		
		double utils = 0. ;
		
		// GK*x:
		for ( Id id : nullfall.values.keySet() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.values.get(id) ;
			for ( Mode mode : Mode.values() ) { // for all modes
				ValuesByMode econValues = economicValues.getByMode(mode) ;
				ValuesByMode quantities = nullfallForODRelation.getByMode(mode) ;
				for ( Type type : Type.values() ) { // for all types (e.g. PV or GV)
					ValuesByType econValues2 = econValues.getByType(type) ;
					ValuesByType quantities2 = quantities.getByType(type) ;
					for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
						if ( entry != Entry.amount ) {
							utils += quantities2.getByEntry(Entry.amount) // amount (e.g. number of persons on OD relation) 
							       * quantities2.getByEntry(entry)        // quantity (e.g. number of km)
							       * econValues2.getByEntry(entry) ;      // econ eval (e.g. valuePerKm) ;
						}
					}
				}
			}
		}
				
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
