/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.vsp.bvwp2;

import org.matsim.api.core.v01.Id;

import playground.vsp.bvwp2.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp2.MultiDimensionalArray.Mode;

/**
 * @author nagel
 *
 */
class Utils {
	private Utils() {} // not to be instantiated

	static void writePartialSum(Html html, double utils) {
		System.out.printf("--------------------%163.1f mio\n", utils / 1000. / 1000.  ) ;
		html.bvwpTableRow("Zwischensumme", "", "", "", "", "", "", "", Double.toString(utils) ) ;
	}

	static void writeSum(Html html, double utils) {
		System.out.printf("%188s\n", "----------------" ) ;
		System.out.printf("bvwp benefit: %169.1f mio\n", utils / 1000. / 1000.  ) ;
		System.out.printf("%188s\n", "================" ) ;
	
		html.beginTableMulticolumnRow() ;
		html.write("Summe") ;
		html.endTableRow() ;
	
		html.bvwpTableRow("Summe", "", "", "", "", "", "", "", Double.toString(utils) ) ;
	}

	static void initializeOutputTables(Html html) {
		System.out.printf( "%16s || %16s | %16s || %16s | %16s || %16s | %16s || %16s | %16s ||\n",
				"Attribut",
				"Attribut Nullf.", "... mal Menge",
				"Attribut Planf.", "... mal Menge",
				"Attribut Diff", "... mal Menge",
				"Nutzen Diff", "... mal Menge") ;
	
		html.beginTable() ;
		html.beginTableRow() ;
		html.write("Attribut") ; html.nextTableEntry() ; 
		html.write("Attribut Nullfall") ; html.nextTableEntry() ;
		html.write("... mal Menge") ; html.nextTableEntry() ;
		html.write("Attribut Planfall") ; html.nextTableEntry() ;
		html.write("... mal Menge") ; html.nextTableEntry() ;
		html.write("Attribut Diff") ; html.nextTableEntry() ;
		html.write("... mal Menge") ; html.nextTableEntry() ;
		html.write("Nutzen Diff") ; html.nextTableEntry() ;
		html.write("... mal Menge") ; 
		html.endTableRow() ;
	}

	static void writeRohAndEndOutput(Html html, double utilsUserFromRoH, double operatorProfit) {
		System.out.printf("RoH: utl gain users: %16.1f ; operator profit gain: %16.1f ; sum: %16.1f\n", 
				utilsUserFromRoH, operatorProfit, utilsUserFromRoH+operatorProfit ) ; 
	
		html.beginTableMulticolumnRow() ;
		html.write("Zum Vergleich: RoH-Rechnung") ;
		html.endTableRow() ;
	
		html.beginTableMulticolumnRow() ;
		html.write("RoH: utl gain users: " + utilsUserFromRoH + "; operator profit gain: " + operatorProfit
				+ "; sum: " + (utilsUserFromRoH+operatorProfit) ) ;
		html.endTableRow() ;
	
		html.endTable() ;
	
		html.endBody() ;
		html.endHtml() ;
	}

	static void writeSubHeaderWechselnd(Html html, Id id, DemandSegment segm, Mode mode, final double deltaAmounts) {
		System.out.printf("%16s; %16s; %16s; wechselnder & induzierter Verkehr: %16.1f Personen/Tonnen\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( id + "; " + segm + "; " + mode + "; wechselnder & induzierter Verkehr: " + deltaAmounts + " Personen/Tonnen") ;
		html.endTableRow();
	}

	static void writeSubHeaderVerbleibend(Html html, Id id, DemandSegment segm, Mode mode, double amountAltnutzer) {
		System.out.printf("%16s; %16s; %16s; verbleibender Verkehr: %16.1f Personen/Tonnen\n", 
				id, mode, segm, amountAltnutzer );
	
		html.beginTableMulticolumnRow() ;
		html.write( id + "; " + segm + "; " + mode + "; verbleibender Verkehr: " + amountAltnutzer + " Personen/Tonnen") ;
		html.endTableRow() ;
	}

}
