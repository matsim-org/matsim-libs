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

import playground.vsp.bvwp2.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp2.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp2.MultiDimensionalArray.Mode;

/**
 * @author nagel
 *
 */
class Utils {
	static final String FMT_STRING = "%16s || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %12.1f  mio||\n";

	private Utils() {} // not to be instantiated

	static void writePartialSum(Html html, double utils) {
		System.out.printf("--------------------%163.1f mio\n", utils / 1000. / 1000.  ) ;
		html.beginTableMulticolumnRow(); 
		html.beginDivRightAlign();
		html.write("<strong>" + Double.toString(((long)(utils/100./1000.))/10.)+" mio</strong>" ) ;
		html.endDiv() ;
		html.endTableRow(); 
	}

	static void writeSum(Html html, double utils) {
		System.out.printf("%188s\n", "----------------" ) ;
		System.out.printf("bvwp benefit: %169.1f mio\n", utils / 1000. / 1000.  ) ;
		System.out.printf("%188s\n", "================" ) ;
	
		html.beginTableMulticolumnRow() ;
		html.write("Summe") ;
		html.endTableRow() ;
	
		html.bvwpTableRow("Summe", "", "", "", "", "", "", "", "", "<strong>" + Double.toString(   ((long)(utils/100./1000.))/10.   ) + " mio</strong>" ) ;
	}

	static void initializeOutputTables(Html html) {
		System.out.printf( "%16s || %16s | %16s || %16s | %16s || %16s | %16s || %16s | %16s ||\n",
				"Attribut",
				"Attribut Nullf.", "... mal Menge",
				"Attribut Planf.", "... mal Menge",
				"Attribut Diff", "... mal Menge",
				"Nutzen Diff", "... mal Menge") ;
	
		html.beginTableRow() ;
		html.write("<strong>Attribut</strong>") ; html.nextTableEntry() ; 
		html.write("<strong>Attribut Nullfall</strong>") ; html.nextTableEntry() ;
		html.write("<strong>... mal Menge</strong>") ; html.nextTableEntry() ;
		html.write("<strong>Attribut Planfall</strong>") ; html.nextTableEntry() ;
		html.write("<strong>... mal Menge</strong>") ; html.nextTableEntry() ;
		html.write("<strong>Attribut Diff</strong>") ; html.nextTableEntry() ;
		html.write("<strong>... mal Menge</strong>") ; html.nextTableEntry() ;
		html.write("<strong>Nutzen Diff</strong>") ; html.nextTableEntry() ;
		html.write("<strong>... mal Menge</strong>") ; html.nextTableEntry() ;
		html.write(" ");
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
		System.out.printf("====================%16s; %16s; %16s; wechselnder & induzierter Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; wechselnder & induzierter Verkehr: " + deltaAmounts + " Personen/Tonnen" + "</strong>") ;
		html.endTableRow();
	}

	static void writeSubHeaderVerbleibend(Html html, Id id, DemandSegment segm, Mode mode, double amountAltnutzer) {
		System.out.printf("====================%16s; %16s; %16s;             verbleibender Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, amountAltnutzer );
	
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; verbleibender Verkehr: " + amountAltnutzer + " Personen/Tonnen" + "</strong>") ;
		html.endTableRow() ;
	}

	static void writeImplicitUtl(Html html, final double implicitUtlPerItem, final double implicitUtlOverall, final String str) {
		if ( implicitUtlPerItem != 0.  ) {
	
			System.out.printf(Utils.FMT_STRING,
					str,
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtlPerItem, implicitUtlOverall/1000./1000.
					) ;
			html.bvwpTableRow(					str,
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtlPerItem, Double.toString(   ((long)(implicitUtlOverall/100./1000.))/10.   ) + " mio" 
					) ;
	
		}
	}

	static void writeAltnutzerRow(Attributes quantitiesNullfall, Attributes quantitiesPlanfall, double amountAltnutzer, Html html,
			Attribute attribute, double deltaQuantities, final double utlChangePerItem, final double utlChange) {
		System.out.printf(Utils.FMT_STRING,
				attribute,
				quantitiesNullfall.getByEntry(attribute), 
				quantitiesNullfall.getByEntry(attribute) * amountAltnutzer,
				quantitiesPlanfall.getByEntry(attribute), 
				quantitiesPlanfall.getByEntry(attribute) * amountAltnutzer,
				deltaQuantities , deltaQuantities * amountAltnutzer ,
				utlChangePerItem , 
				utlChange/1000./1000.
				) ;
		html.bvwpTableRow(
				attribute.toString(),
				quantitiesNullfall.getByEntry(attribute), 
				quantitiesNullfall.getByEntry(attribute) * amountAltnutzer,
				quantitiesPlanfall.getByEntry(attribute), 
				quantitiesPlanfall.getByEntry(attribute) * amountAltnutzer,
				deltaQuantities , deltaQuantities * amountAltnutzer ,
				utlChangePerItem , 
				Double.toString(   ((long)(utlChange/100./1000.))/10.   ) + " mio" 
				) ;
	}

	static void writeAufnehmendRow(Html html, final double deltaAmounts, Attribute attribute, final double attributeValuePlanfall,
			UtlChangesData utlChangesPerItem, final double utlChange) {
		System.out.printf(FMT_STRING,attribute,
				0., 0., 
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				utlChangesPerItem.utl , utlChange/1000./1000.
				) ;
		html.bvwpTableRow(attribute.toString(),
				0., 0., 
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				utlChangesPerItem.utl , Double.toString(   ((long)(utlChange/100./1000.))/10.   ) + " mio" ) ;
	}

	static void writeAbgebendRow(Html html, final double deltaAmounts, Attribute attribute, final double attributeValuePlanfall,
			final double attributeValueNullfall, UtlChangesData utlChangesPerItem, final double utlChange) {
		System.out.printf(FMT_STRING, attribute,
				attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				0., 0., 
				-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
				// Nullfall)
				utlChangesPerItem.utl , utlChange/1000./1000.
				) ;
		html.bvwpTableRow(attribute.toString(),
				attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				attributeValuePlanfall, 0., 
				-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
				// Nullfall)
				utlChangesPerItem.utl , Double.toString(   ((long)(utlChange/100./1000.))/10.   ) + " mio" ) ;
	}

}
