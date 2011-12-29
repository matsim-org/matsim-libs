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

import org.matsim.testcases.MatsimTestCase;

public class BvwpTest extends MatsimTestCase {
	static class EconomicValues {
		double valueOfTimePV_Eu_h = 6. ;
		double valueOfDistancePV_Eu_km = 0.2 ;

		double valueOfTimeGV_Eu_h = 20. ;
		double valueOfDistanceGV_Eu_km = 1. ;
		
	}
	
	enum Entry { persons, tons, km, hrsPV, hrsGV, monPV, monGV } ;
	
	static class OutputFromPrognosisByMode {
		Map<Entry,Double> quantities = new TreeMap<Entry,Double>() ;
		OutputFromPrognosisByMode() {
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
		static OutputFromPrognosisByMode createDeepCopy( OutputFromPrognosisByMode oldValues ) {
			OutputFromPrognosisByMode newValues = new OutputFromPrognosisByMode() ;
			for ( Entry entry : Entry.values() ) {
				newValues.setByEntry( entry, oldValues.getByEntry(entry) ) ;
			}
			return newValues ;
		}
		
	}
	
	enum Mode { road, rail } ;

	static class OutputFromPrognosis {
		Map<Mode,OutputFromPrognosisByMode> entries = new TreeMap<Mode,OutputFromPrognosisByMode>() ;
		static OutputFromPrognosis createDeepCopy( OutputFromPrognosis nullfall ) {
			OutputFromPrognosis planfall = new OutputFromPrognosis() ;
			for ( Mode mode : Mode.values() ) {
				OutputFromPrognosisByMode oldValues = nullfall.getByMode(mode) ;
				OutputFromPrognosisByMode newValues = OutputFromPrognosisByMode.createDeepCopy( oldValues ) ;
				planfall.entries.put( mode, newValues ) ;
			}
			return planfall ; 
		}
		OutputFromPrognosis() {
			for ( Mode mode : Mode.values() ) {
				OutputFromPrognosisByMode vals = new OutputFromPrognosisByMode() ;
				entries.put( mode, vals ) ;
			}
		}
		OutputFromPrognosisByMode getByMode( Mode mode ) {
				return entries.get(mode) ;
		}
		void setValuesForMode( Mode mode, OutputFromPrognosisByMode values ) {
			entries.put( mode, values ) ;
		}
	}


	public void testOne() {
		// yy if the following refers to one relation, it is fine.  if it refers to "everything", then we somehow need
		// to differentiate between old and new users.  kai, dec'11
		// yy are we communicating pkm, or p and km separately?? kai, dec'11
		
		OutputFromPrognosis nullfall = new OutputFromPrognosis() ;
		{
			OutputFromPrognosisByMode roadValues = nullfall.getByMode(Mode.road) ;
			roadValues.setByEntry( Entry.persons, 1000. ) ;
			roadValues.setByEntry( Entry.tons, 1000. ) ;
			roadValues.setByEntry( Entry.km, 10. ) ;
			roadValues.setByEntry( Entry.hrsGV, 1. ) ;
			roadValues.setByEntry( Entry.hrsPV, 1. ) ;

			OutputFromPrognosisByMode railValues = OutputFromPrognosisByMode.createDeepCopy( roadValues ) ;
			nullfall.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		OutputFromPrognosis planfall = OutputFromPrognosis.createDeepCopy(nullfall) ;
		{
			OutputFromPrognosisByMode railValues = planfall.getByMode( Mode.rail ) ;
			railValues.setByEntry( Entry.hrsGV, 0.9 ) ;
			railValues.setByEntry( Entry.hrsPV, 0.9 ) ;
		}
		
	}
	
	static void railImprovementBVWP03( OutputFromPrognosis nullfall, OutputFromPrognosis planfall ) {
		
	}

	static void railImprovementNew( OutputFromPrognosis nullfall, OutputFromPrognosis planfall ) {
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )
		
		EconomicValues vals = new EconomicValues() ;
		double gk = 0. ;
		
		// GK*x:
		{
			OutputFromPrognosisByMode railvalues = nullfall.getByMode(Mode.rail) ;
			gk += 
				vals.valueOfDistancePV_Eu_km * railvalues.getByEntry(Entry.km) *  railvalues.getByEntry(Entry.persons) ;
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
