package playground.vsp.zzArchive.bvwpOld;
///* *********************************************************************** *
// * project: org.matsim.*
// * UtilityChangesBVWP2003.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2012 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// * 
// */
//package playground.kai.bvwp;
//
//import playground.kai.bvwp.Values.Entry;
//
//
///**
// * @author Ihab
// *
// */
//public class UtilityChangesBVWP2015Old extends UtilityChanges {
////	UtilityChangesBVWP2015() {
////		System.out.println("\nSetting utility computation method to " + this.getClass() ) ;
////	}
//	
//	@Override
//	UtlChangesData utlChangePerItem(double deltaAmount, double quantityNullfall, 
//			double quantityPlanfall, double econVal) {
//		
//		UtlChangesData utlChanges = new UtlChangesData() ;
//		
//		double personenXNull = quantitiesNullfall.getByEntry(Entry.XX) * quantitiesNullfall.getByEntry(entry);
//		double personenXPlan = quantitiesPlanfall.getByEntry(Entry.XX) * quantitiesPlanfall.getByEntry(entry);
//		double diff = personenXPlan - personenXNull; // e.g. pkm
//		
//		utlChanges.utl = diff * econValues.getByEntry(entry);	
//		
//		// "halbe" Verbesserung:
//		double attributeForHalfUser = 0.5 * (quantitiesPlanfall.getByEntry(entry) + quantitiesNullfall.getByEntry(entry)) ;
//		double numberOfSwitchers = quantitiesPlanfall.getByEntry(Entry.XX) - quantitiesNullfall.getByEntry(Entry.XX) ;
//		utlChanges.utl -= numberOfSwitchers * attributeForHalfUser * econValues.getByEntry(entry);
//		// the sign of this is a miracle.  I did reverse engineering of the implicit utl calculation
//		// (see the ``alternative Rechnung'' table in ab200.tex in the ``Verlagerung'' subsubsection), followed
//		// by trial-and-error.  kai, feb'11
//		// This seems to have the curious consequence that, if there is not attribute change,
//		// the switchers add exactly the generalized costs that they would have
//		// used if they had stayed ... rendering the utility contribution of this entry exactly zero. kai, feb'11
//		
//		return utlChanges;
//	}
//
//}
