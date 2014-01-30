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
package playground.vsp.bvwp;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;

/**
 * @author nagel
 *
 */
class Utils {
	static final String FMT_STRING = "%16s || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %12.1e  mio||\n";

	private Utils() {} // not to be instantiated

	static void writePartialSum(Html html, String text, double utils) {
		System.out.printf("--------------------%163.1e mio\n", utils / 1000. / 1000.  ) ;
		html.beginTableMulticolumnRow(); 
		html.beginDivRightAlign();
		if ( text!=null ) {
			html.write( text );
		}
		html.write("<strong>" + convertToMillions(utils) +"</strong>" ) ;
		html.endDiv() ;
		html.endTableRow(); 
	}

	static void writeSum(Html html, double utils) {
		System.out.printf("%188s\n", "----------------" ) ;
		System.out.printf("bvwp benefit: %169.1e mio\n", utils / 1000. / 1000.  ) ;
		System.out.printf("%188s\n", "================" ) ;
	
		html.beginTableMulticolumnRow() ;
		html.write("<strong>Summe</strong>") ;
		html.endTableRow() ;
	
//		html.bvwpTableRow("", "", "", "", "", "", "", "", "", "<strong>" + convertToMillions(utils) + "</strong>" ) ;
		html.beginTableMulticolumnRow() ;
		html.beginDivRightAlign();
		html.write("<strong>" + convertToMillions(utils) + "</strong>" ) ;
		html.endDiv();
		html.endTableRow() ;
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
		html.write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
		html.endTableRow() ;
	}

	static void writeRohAndEndOutput(Html html, double utilsUserFromRoHOldUsers, double utilsUserFromRoHNewUsers, double operatorProfit) {
		System.out.printf("RoH: utl gain old users: %16.1e ; utl gain new users: %16.1e ; operator profit gain: %16.1e ; sum: %16.1e\n", 
				utilsUserFromRoHOldUsers, utilsUserFromRoHNewUsers, operatorProfit, utilsUserFromRoHOldUsers+utilsUserFromRoHNewUsers+operatorProfit ) ; 
	
		html.beginTableMulticolumnRow() ;
		html.write("<strong>Zum Vergleich: RoH-Rechnung</strong>") ;
		html.endTableRow() ;
	
		html.beginTableMulticolumnRow() ;
		html.write("RoH: utl gain old users: " + Utils.convertToMillions(utilsUserFromRoHOldUsers) + 
				"RoH: utl gain new users: " + Utils.convertToMillions(utilsUserFromRoHNewUsers) + "; operator profit gain: " + Utils.convertToMillions(operatorProfit)
				+ "; sum: " + Utils.convertToMillions(utilsUserFromRoHOldUsers + utilsUserFromRoHNewUsers +operatorProfit) ) ;
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
	
	static void writeSubHeaderVerlagert(Html html, Id id, DemandSegment segm, Mode mode, final double deltaAmounts) {
		
		System.out.printf("====================%16s; %16s; %16s; verlagerter Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; verlagerter Verkehr: " + deltaAmounts + " Personen/Tonnen" + "</strong>") ;
		html.endTableRow();
	}
	
	static void writeSubHeaderInduziert(Html html, Id id, DemandSegment segm, Mode mode, final double deltaAmounts) {
		
		System.out.printf("====================%16s; %16s; %16s;  induzierter Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; induzierter Verkehr: " + deltaAmounts + " Personen/Tonnen" + "</strong>") ;
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
					implicitUtlPerItem, convertToMillions(implicitUtlOverall)
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
				convertToMillions(utlChange) 
				) ;
	}
	
	static String convertToMillions( double tmp ) {
		StringBuilder sb = new StringBuilder() ;
		Formatter formatter = new Formatter(sb) ;
		formatter.format("%12.6f mio", tmp/1000./1000.) ;
		formatter.close(); 
		return sb.toString();
//		
//		return Double.toString( ((long)(tmp/100./1000.))/10. ) + " mio";
	}

	static void writeAufnehmendRow(Html html, final double deltaAmounts, Attribute attribute, final double attributeValuePlanfall,
			UtlChangesData utlChangesPerItem, final double utlChange) {
		System.out.printf(FMT_STRING,attribute,
				0., 0., 
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				utlChangesPerItem.utl , utlChange/1000./1000.
				) ;
		html.bvwpTableRow(attribute.toString()+" aufn.",
				0., 0., 
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
				utlChangesPerItem.utl , convertToMillions(utlChange) ) ;
	}

	static void writeAbgebendRow(Html html, final double deltaAmounts, Attribute attribute, final double attributeValueNullfall,
			final double attributeValuePlanfall, UtlChangesData utlChangesPerItem, final double utlChange) {
		System.out.printf(FMT_STRING, attribute,
				attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				0., 0., 
				-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
				// Nullfall)
				utlChangesPerItem.utl , utlChange/1000./1000.
				) ;
		html.bvwpTableRow(attribute.toString()+" abgeb.",
				attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				attributeValuePlanfall, 0., // this uses Planfall just for completeness 
				-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
				// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
				// Nullfall)
				utlChangesPerItem.utl , convertToMillions( utlChange) ) ;
	}
	
	
	static void writeVerlagertSum(Html html, HashMap<Mode,Double> utilsVerl) {
		System.out.printf("%188s\n", "----------------" ) ;
		for (Entry<Mode,Double> e : utilsVerl.entrySet()){
			System.out.println("Fuer Mode "+e.getKey()+" Summe Nutzen verlagert: "+e.getValue() ) ;
		}

		System.out.printf("%188s\n", "================" ) ;
	
//		html.beginTableMulticolumnRow() ;
//		html.write("Summe") ;
//		html.endTableRow() ;
//	
//		html.bvwpTableRow("Summe", "", "", "", "", "", "", "", "", "<strong>" + Double.toString(   ((long)(utilsVerl/100./1000.))/10.   ) + " mio</strong>" ) ;
	}
	
	static void writeInduziertSum(Html html,  HashMap<Mode,Double> utilsInduz) {
		System.out.printf("%188s\n", "----------------" ) ;

		for (Entry<Mode,Double> e : utilsInduz.entrySet()){
			System.out.println("Fuer Mode "+e.getKey()+" Summe Nutzen induziert: "+e.getValue() ) ;
		}
		
		System.out.printf("%188s\n", "================" ) ;
//	
//		html.beginTableMulticolumnRow() ;
//		html.write("Summe") ;
//		html.endTableRow() ;
//	
//		html.bvwpTableRow("Summe", "", "", "", "", "", "", "", "", "<strong>" + Double.toString(   ((long)(utilsInduz/100./1000.))/10.   ) + " mio</strong>" ) ;
	}

	 static void writeImplVerlagertSum(Html html,
			HashMap<Mode, Double> modularImplVerlagertUtils) {

			System.out.printf("%188s\n", "----------------" ) ;
			for (Entry<Mode,Double> e : modularImplVerlagertUtils.entrySet()){
				System.out.println("Fuer Mode "+e.getKey()+" Summe IMPLIZITER Nutzen verlagert: "+e.getValue() ) ;
			}

			System.out.printf("%188s\n", "================" ) ;
	}
	

		static void writeImplInduziertSum(Html html,  HashMap<Mode,Double> utilsImplInduz) {
			System.out.printf("%188s\n", "----------------" ) ;

			for (Entry<Mode,Double> e : utilsImplInduz.entrySet()){
				System.out.println("Fuer Mode "+e.getKey()+" Summe IMPLIZITER Nutzen induziert: "+e.getValue() ) ;
			}
			
			System.out.printf("%188s\n", "================" ) ;
}
		
		static void writeOverallOutputTable(Html html,Map<Mode,Double> verblRV, Map<Mode,Double> verlRV,Map<Mode,Double> verlImp, Map<Mode,Double> indRV,Map<Mode,Double> indImp)
		{
			html.beginTableMulticolumnRow();
			html.write("Summen ueber alle Relationen und Zwecke");
			html.endTableRow();
			List<String> line = new ArrayList<String>();
			line.add("Komponente");
			for (Mode mode : Mode.values()){
				line.add(mode.toString());
			}
			line.add("Summe");
			html.tableRowFromList(line, true);
			line.clear();
			
			line.add("Nutzen&auml;nderung aus &Auml;nderung Ressourcenverzehr im verbleibenden Verkehr");
			Double rowSum = 0.;
			Map<Mode,Double> allesTotal = new HashMap<Mode,Double>();

			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(verblRV, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(verblRV, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(allesTotal, mode, d);
				}	
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line,true);
			line.clear();
			
			line.add("Nutzen&auml;nderung aus &Auml;nderung Ressourcenverzehr im verlagerten Verkehr");
			rowSum = 0.;
			Map<Mode,Double> verlTot = new HashMap<Mode,Double>();
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(verlRV, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(verlRV, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(verlTot, mode, d);
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line, false);
			line.clear();
			
			line.add("Nutzen&auml;nderung aus impliziten Nutzen im verlagerten Verkehr");
			rowSum = 0.;
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(verlImp, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(verlImp, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(verlTot, mode, d);
				}
				
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line, false);
			line.clear();
			
			line.add("Nutzen&auml;nderung insgesamt im verlagerten Verkehr");
			rowSum = 0.;
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(verlTot, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(verlTot, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(allesTotal, mode, d);
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line,true);
			line.clear();
			
			line.add("Nutzen&auml;nderung aus &Auml;nderung Ressourcenverzehr im induzierten Verkehr");
			rowSum = 0.;
			Map<Mode,Double> indTot = new HashMap<Mode,Double>();
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(indRV, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(indRV, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(indTot, mode, d);
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line, false);
			line.clear();
			
			line.add("Nutzen&auml;nderung aus impliziten Nutzen im induzierten Verkehr");
			rowSum = 0.;
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(indImp, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(indImp, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(indTot, mode, d);
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line, false);
			line.clear();
			
			line.add("Nutzen&auml;nderung insgesamt im induzierten Verkehr");
			rowSum = 0.;
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(indTot, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(indTot, mode);
				if (d != null){
					rowSum += d;
					addUtlToMap(allesTotal, mode, d);
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line,true);
			line.clear();
			
			line.add("Summe Nutzen&auml;nderungen insgesamt");
			rowSum = 0.;
			for (Mode mode: Mode.values()){
				String string1 = tryToGetValueOrReturnNa(allesTotal, mode); 
				line.add(string1);
				Double d = tryToGetValueOrReturnNull(allesTotal, mode);
				if (d != null){
					rowSum += d;
				}
			}
			line.add(convertToMillions(rowSum));
			html.tableRowFromList(line,true);
			line.clear();

			html.endTable() ;
			
			html.endBody() ;
			html.endHtml() ;
	
		}
		private static String tryToGetValueOrReturnNa(Map<Mode,Double> map, Mode mode){
			String result = null;
			if (map.containsKey(mode)) result = convertToMillions(map.get(mode));
			if (result == null) result = "n.a.";
			
			return result;
			
		}
		private static Double tryToGetValueOrReturnNull(Map<Mode,Double> map, Mode mode){
			Double result = null;
			if (map.containsKey(mode)) result = map.get(mode);
			
			
			return result;
			
		}
		static void addUtlToMap(Map<Mode, Double> map, Mode mode ,double utl){
			double utlBefore = 0.;
			
			if (map.containsKey(mode)) utlBefore = map.get(mode);
			double utlAfter = utlBefore + utl;
			map.put(mode, utlAfter);

		}
}
