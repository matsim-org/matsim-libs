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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	
	static void writeOperatorProfit(Map<Mode,Double> operatorProfitGains, Html html){
		for (Entry<Mode,Double> profit : operatorProfitGains.entrySet()){
		html.beginTableMulticolumnRow();
		html.write( "Operator profit gain; " + profit.getKey().toString() + ": " + Utils.convertToMillions(profit.getValue()) );
		html.endTableRow();
		}
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

	static void writeRoh(Html html, double utilsUserFromRoHOldUsers, double utilsUserFromRoHNewUsers,Map<Mode,Double> operatorProfitGains) {
	    double operatorProfit = 0.;
	    for (Double d : operatorProfitGains.values()){
	        operatorProfit+=d;
	    }
	    
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
	
	}
	static void endOutput(Html html) {
	
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
	
	static void writeSubHeaderVerlagert(Html html, String id, DemandSegment segm, Mode mode, final double deltaAmounts) {
		
		System.out.printf("====================%16s; %16s; %16s; verlagerter Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; verlagerter Verkehr: " + deltaAmounts + " Personen/Tonnen" + "</strong>") ;
		html.endTableRow();
	}
	
	static void writeSubHeaderInduziert(Html html, String id, DemandSegment segm, Mode mode, final double deltaAmounts) {
		
		System.out.printf("====================%16s; %16s; %16s;  induzierter Verkehr: %16.1f Personen/Tonnen ====================\n", 
				id, mode, segm, deltaAmounts ) ;
		html.beginTableMulticolumnRow() ;
		html.write( "<strong>" + id + "; " + segm + "; " + mode + "; induzierter Verkehr: " + deltaAmounts + " Personen/Tonnen" + "</strong>") ;
		html.endTableRow();
	}
	
	

	static void writeSubHeaderVerbleibend(Html html, String id, DemandSegment segm, Mode mode, double amountAltnutzer) {
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
		static void addUtlToMap(Map<Mode, Map<Attribute,Double>> map, Mode mode, Attribute attribute ,double utl){
			Map<Attribute,Double> utlMapBefore = new HashMap<Attribute, Double>();

			if (map.containsKey(mode)) utlMapBefore = map.get(mode);
			double utlBefore = 0.;
			if (utlMapBefore.containsKey(attribute)) utlBefore = utlMapBefore.get(attribute);
			
			double utlAfter = utlBefore + utl;
			utlMapBefore.put(attribute, utlAfter);
			map.put(mode, utlMapBefore);

		}
		
		static void addUtlToMap(Map<Mode, Double> map, Mode mode, double utl){
			
			double utlBefore = 0.;
			if (map.containsKey(mode)) utlBefore = map.get(mode);
			
			double utlAfter = utlBefore + utl;
			map.put(mode, utlAfter);

		}

		static double  writeOverallOutputTable(Html totalHtml,
				Map<Mode, Map<Attribute, Double>> verbleibendRV,
				Map<Mode, Map<Attribute, Double>> verlagertRVAuf,
				Map<Mode, Map<Attribute, Double>> verlagertRVAb,
				Map<Mode, Double> verlagertImpAuf,
				Map<Mode, Double> verlagertImpAb,
				Map<Mode, Map<Attribute, Double>> induziertRV,
				Map<Mode, Double> induziertImp,
				Map<Mode, Double[]> verlagertNMAb,
				Map<Mode, Double[]> verlagertNMAuf,
				Double[] induziertNM 
		        ) {
			 
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write("<b>Summen ueber alle Relationen und Zwecke (Angabe in EUR)</b>");
				totalHtml.endTableRow();
				double totalSum = 0.;
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write("<b>Verbleibender Verkehr</b>");
				totalHtml.endTableRow();

				List<String> line = new ArrayList<String>();

				double vblSum = 0.;
				for (Entry<Mode, Map<Attribute, Double>> e : verbleibendRV.entrySet()){
					totalHtml.beginTableMulticolumnRow();
					totalHtml.write(e.getKey().toString()+":");
					totalHtml.endTableRow();
					double zws = 0.;
					for (Entry<Attribute, Double> ee : e.getValue().entrySet()){
						line.add(ee.getKey().toString());
						line.add(convertToMillions(ee.getValue()));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						zws += ee.getValue();
					}
//					line.add("Zwischensumme "+e.getKey().toString());
//					line.add("<b> </b> ");
//					line.add(convertToMillions(zws));
//					totalHtml.tableRowFromList(line, false);
//					line.clear();
					vblSum += zws;
					
				}
				line.add("Summe verbleibender Verkehr insgesamt");
				line.add(" ");
				line.add(convertToMillions(vblSum));
				totalHtml.tableRowFromList(line, true);
				line.clear();
				totalSum += vblSum;

				totalHtml.beginTableMulticolumnRow();
				totalHtml.write(" ");
				totalHtml.endTableRow();
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write("<b>Verlagerter Verkehr</b>");
				totalHtml.endTableRow();
				
				double verlSum = 0.;
				HashSet<Mode> relModes = new HashSet<Mode>();
				relModes.addAll(verlagertRVAb.keySet());
				relModes.addAll(verlagertRVAuf.keySet());
				Set<Attribute> relAttr = new HashSet<Attribute>();
				for (Mode mode : relModes){
					if (verlagertRVAb.containsKey(mode)) relAttr.addAll(verlagertRVAb.get(mode).keySet());
					if (verlagertRVAuf.containsKey(mode)) relAttr.addAll(verlagertRVAuf.get(mode).keySet());
				}
				
				for (Mode mode : relModes){
					totalHtml.beginTableMulticolumnRow();
					totalHtml.write("<b>"+mode.toString()+":</b>");
					totalHtml.endTableRow();
		            //verlagertNMAb/Auf: 0 - personen 1- personenKM 2 - pkw-KM 3 - personen-h 4 - pkw-h 5 - nutzerkosten  
				
					line.add("Verlagerte Fahrleistung:");
					line.add(Math.round(verlagertNMAuf.get(mode)[2])+" Pkw-km");
					totalHtml.tableRowFromList(line,false);
					line.clear();
	                
					line.add("Mittlerer Besetzungsgrad:");
					Double bsg = verlagertNMAuf.get(mode)[1] / verlagertNMAuf.get(mode)[2];
                    line.add(bsg.toString());
                    totalHtml.tableRowFromList(line,false);
                    line.clear();
					
	                line.add("Mittlere Fahrweite des verlagerten Verkehrs:");
                    line.add(Math.round(verlagertNMAuf.get(mode)[1]/Math.abs(verlagertNMAuf.get(mode)[0]))+"km");
	                totalHtml.tableRowFromList(line,false);
	                line.clear();
										
					line.add("Verlagerte Fahrzeit:");
					line.add(Math.round(verlagertNMAuf.get(mode)[4])+" Pkw-h");
					totalHtml.tableRowFromList(line,false);
					line.clear();
					
					line.add("Mittlere Nutzerkosten des verlagerten Verkehrs:");
                    line.add(Math.round(Math.abs(verlagertNMAuf.get(mode)[5]*100/verlagertNMAuf.get(mode)[2]))+" c/Pkw-km");
                    totalHtml.tableRowFromList(line,false);
                    line.clear();
                    
                    
					
					double zws =0.;
					Map <Attribute,Double> attAbmap = new HashMap<Attribute,Double>();
						if (verlagertRVAb.containsKey(mode)) attAbmap = verlagertRVAb.get(mode);
					Map <Attribute,Double> attAufmap = new HashMap<Attribute,Double>();
						if (verlagertRVAuf.containsKey(mode)) attAufmap = verlagertRVAuf.get(mode);

						for (Attribute attr : relAttr){
		
							double zzws = 0.;
							if (attAbmap.containsKey(attr)) {
								line.add(attr.toString() + " abgebend");
								double v = attAbmap.get(attr);
								zzws += v;
								line.add(convertToMillions(v));
								totalHtml.tableRowFromList(line, false);
								line.clear();								
							}
							if (attAufmap.containsKey(attr)) {
								line.add(attr.toString() + " aufnehmend");
								double v = attAufmap.get(attr);
								zzws += v;
								line.add(convertToMillions(v));
								totalHtml.tableRowFromList(line, false);
								line.clear();								
							}
							if (zzws!=0.0){
							line.add("<b> </b>");
							line.add("<b> </b>");
							line.add(convertToMillions(zzws));
							totalHtml.tableRowFromList(line, false);
							line.clear();
							zws += zzws;
							}
						
						}

						line.add("Nutzenaenderung aus Aenderung Ressourcenverzehr bei Verlagerung");
						line.add("<b> </b> ");
						line.add(convertToMillions(zws));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						
						double impl =0.;
					if (verlagertImpAb.containsKey(mode)){
						line.add("Impl. Nutzen abgebend");
						double v = verlagertImpAb.get(mode);
						impl += v;
						line.add(convertToMillions(v));
						totalHtml.tableRowFromList(line, false);
						line.clear();
					}	
					if (verlagertImpAuf.containsKey(mode)){
						line.add("Impl. Nutzen aufnehmend");
						double v = verlagertImpAuf.get(mode);
						impl += v;
						line.add(convertToMillions(v));
						totalHtml.tableRowFromList(line, false);
						line.clear();
					}
					if (impl!=0.0){
						line.add("<b> </b>");
						line.add("<b> </b>");

						line.add(convertToMillions(impl));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						zws += impl;
						}

						line.add("Nutzenaenderung bei Verlagerung von Verkehrstraeger "+mode.toString());
						line.add("<b> </b> ");
						line.add(convertToMillions(zws));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						verlSum += zws;
				}
				
//				writePartialSum(totalHtml, "Nutzenaenderung bei Verlagerung insgesamt (alle Verkehrstraeger)", verlSum);
				line.add("Nutzenaenderung bei Verlagerung insgesamt (alle Verkehrstraeger)");
				line.add("");
				line.add(convertToMillions(verlSum));
				totalHtml.tableRowFromList(line, true);		
				line.clear();
				totalSum += verlSum;
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write(" ");
				totalHtml.endTableRow();
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write("<b>Induzierter Verkehr</b>");
				totalHtml.endTableRow();
				
                //induziertNM: 0 - personen 1- personenKM 2 - pkw-KM 3 - personen-h 4 - pkw-h 5 - nutzerkosten  
				
			    line.add("Zus&auml;tzliche Fahrten des induzierten Verkehrs:");
	            line.add(Math.round(induziertNM[0])+" Personen");
	            totalHtml.tableRowFromList(line,false);
	            line.clear();
				
	            Double bsg = induziertNM[1] / induziertNM[2];
	            
	            line.add("Zus&auml;tzliche Fahrten des induzierten Verkehrs:");
	            double pkwf = induziertNM[0]/bsg ;
                line.add(Math.round(pkwf)+" Pkw");
                totalHtml.tableRowFromList(line,false);
                line.clear();
	            
	            line.add("Mittlerer Besetzungsgrad:");
                line.add(bsg.toString());
                totalHtml.tableRowFromList(line,false);
                line.clear();
                
	            
			    line.add("Zus&auml;tzliche Fahrleistung des induzierten Verkehrs:");
                line.add(Math.round(induziertNM[2])+" Pkw-km");
                totalHtml.tableRowFromList(line,false);
                line.clear();
                
                line.add("Zus&auml;tzliche Fahrleistung des induzierten Verkehrs:");
                line.add(Math.round(induziertNM[1])+" Personen-km");
                totalHtml.tableRowFromList(line,false);
                line.clear();
                           
                               
                line.add("Mittlere Fahrweite des induzierten Verkehrs:");
                line.add(Math.round(induziertNM[2]/pkwf)+"km");
                totalHtml.tableRowFromList(line,false);
                line.clear();
                                    
                line.add("Zus&auml;tzliche Fahrzeit des induzierten Verkehrs:");
                line.add(Math.round(induziertNM[4])+" Pkw-h");
                totalHtml.tableRowFromList(line,false);
                line.clear();
                
                line.add("Mittlere Nutzerkosten des induzierten Verkehrs:");
                line.add(Math.round(induziertNM[5]*100/induziertNM[2])+" c/Pkw-km");
                totalHtml.tableRowFromList(line,false);
                line.clear();
				
				double indSum = 0.;
				
				Set<Mode> relevantModesForInd = new HashSet<Mode>();
				relevantModesForInd.addAll(induziertRV.keySet());
				relevantModesForInd.addAll(induziertImp.keySet());
				
				for (Mode mode :  relevantModesForInd)
				{
					totalHtml.beginTableMulticolumnRow();
					totalHtml.write(mode.toString()+":");
					totalHtml.endTableRow();
					double zws = 0.;
					if (induziertRV.containsKey(mode)){
						for (Entry<Attribute,Double> e : induziertRV.get(mode).entrySet()){
						line.add(e.getKey().toString() + " aufnehmend");
						line.add(convertToMillions(e.getValue()));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						zws += e.getValue();
						}
						}
					if (induziertImp.containsKey(mode)){
						line.add("Impliziter Nutzen induziert");
						line.add(convertToMillions(induziertImp.get(mode)));
						totalHtml.tableRowFromList(line, false);
						line.clear();
						zws += induziertImp.get(mode);
					}
					line.add("Zwischensumme "+mode.toString());
					line.add(" ");
					line.add(convertToMillions(zws));
					totalHtml.tableRowFromList(line, true);
					line.clear();
					indSum += zws;
					
				}
				line.add("Summe induzierter Verkehr insgesamt");
				line.add(" ");
				line.add(convertToMillions(indSum));
				totalHtml.tableRowFromList(line, true);
				line.clear();
				totalSum += indSum;
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.write(" ");
				totalHtml.endTableRow();
				
				line.add("Summe");
				line.add(" ");
				line.add(convertToMillions(totalSum));
				totalHtml.tableRowFromList(line, true);
				line.clear();
				
				totalHtml.beginTableMulticolumnRow();
				totalHtml.endTableRow();
				
				
				
			return(totalSum);
					
				}
		 

		 		

				
				
				
				
		}

