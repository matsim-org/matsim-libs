package playground.vsp.zzArchive.bvwpOld;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;
import playground.vsp.zzArchive.bvwpOld.Values.Mode;


/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */
@Deprecated
abstract class UtilityChanges {
	private static final String FMT_STRING = "%16s || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %16.1f ||\n";
	UtilityChanges() {
		System.out.println("Setting utility computation method to " + this.getClass() ) ;
	}
	
	final void computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall ) {
		Html html = new Html() ;
		computeAndPrintResults(economicValues,nullfall,planfall,html) ;
	}

	final void computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall, Html html ) {
		// (GK-GK') * x + 0.5 * (GK-GK') (x'-x) =
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )
		
		// yyyy these two can't be here if html is initialized in user code!
		html.beginHtml() ;
		html.beginBody() ;

		double utils = 0. ;
		double utilsUserFromRoH = 0. ;
		double operatorProfit = 0. ;
		for ( String id : nullfall.getAllRelations() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			for ( DemandSegment demandSegment : DemandSegment.values() ) { // for all types (e.g. PV or GV)

				Mode improvedMode = autodetectImprovingMode(
						nullfallForODRelation, planfallForODRelation,
						demandSegment);

				if ( improvedMode == null ) {
					continue ; // means that in the demand segment, nothing has changed; goto next demand segment
				}

				Attributes econValuesImprovedMode = economicValues.getByMode(improvedMode).getByDemandSegment(demandSegment) ;
				Attributes attributesNullfallImprovedMode = nullfallForODRelation.getByMode(improvedMode).getByDemandSegment(demandSegment) ;
				Attributes attributesPlanfallImprovedMode = planfallForODRelation.getByMode(improvedMode).getByDemandSegment(demandSegment) ;

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

				for ( Mode mode : Mode.values() ) { // for all modes
					Attributes econValues = economicValues.getByMode(mode).getByDemandSegment(demandSegment) ;
					Attributes attributesNullfall = nullfallForODRelation.getByMode(mode).getByDemandSegment(demandSegment) ;
					Attributes attributesPlanfall = planfallForODRelation.getByMode(mode).getByDemandSegment(demandSegment) ;
					final double amountNullfall = attributesNullfall.getByEntry(Attribute.XX);
					final double amountPlanfall = attributesPlanfall.getByEntry(Attribute.XX);

					if ( amountPlanfall!=0. || amountNullfall!=0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)

						if ( mode==improvedMode ) {
							// yyyy pull "improved mode" forwards in pgm flow
							// Altnutzer:

							double amountAltnutzer = amountNullfall ;

							System.out.printf("%16s; %16s; %16s; verbleibender Verkehr: %16.1f Personen/Tonnen\n", 
									id, mode, demandSegment, amountAltnutzer );
							
							html.beginTableMulticolumnRow() ;
							html.write( id + "; " + demandSegment + "; " + mode + "; verbleibender Verkehr: " + amountAltnutzer + " Personen/Tonnen") ;
							html.endTableRow() ;

							utils = computeAndPrintValuesForAltnutzer(utils,
									econValues, attributesNullfall,
									attributesPlanfall, amountAltnutzer, html);

						} else {

							final double deltaAmounts = amountPlanfall - amountNullfall ;

							// abgebend:

							System.out.printf("%16s; %16s; %16s; wechselnder Verkehr: %16.1f Personen/Tonnen\n", 
									id, mode, demandSegment, deltaAmounts ) ;
							html.beginTableMulticolumnRow() ;
							html.write( id + "; " + demandSegment + "; " + mode + "; wechselnder Verkehr: " + deltaAmounts + " Personen/Tonnen") ;
							html.endTableRow();
							utils = computeAndPrintGivingOrReceiving(utils, econValues, attributesNullfall,
									attributesPlanfall, deltaAmounts, html);
							
							// jetzt Rechnung für aufnehmend:
							
							System.out.printf("%16s; %16s; %16s; " +
									"wechselnder Verkehr: %16.1f Personen/Tonnen\n", 
									id, improvedMode, demandSegment, -deltaAmounts ) ;
							html.beginTableMulticolumnRow() ;
							html.write( id + "; " + demandSegment + "; " + mode + "; wechselnder Verkehr: " + (-deltaAmounts) + " Personen/Tonnen") ;
							html.endTableRow();

							utils = computeAndPrintGivingOrReceiving(utils, econValuesImprovedMode,
									attributesNullfallImprovedMode, attributesPlanfallImprovedMode, -deltaAmounts, html);

							// implicit utl:
							utils = computeAndPrintImplicitUtl(utils,
									econValuesImprovedMode, attributesNullfallImprovedMode,attributesPlanfallImprovedMode,
									econValues, attributesNullfall,attributesPlanfall, deltaAmounts, html);
						}
					}

					// compute user gains according to RoH: improvement * <x>
					if ( amountPlanfall >= amountNullfall ) {
						final double averageOfXX = (amountPlanfall + amountNullfall)/2. ;
						for ( Attribute attribute : Attribute.values() ) {
							if ( attribute!=Attribute.XX && attribute!=Attribute.costOfProduction ) {
								final double improvementOfAttribute = attributesPlanfall.getByEntry(attribute) - attributesNullfall.getByEntry(attribute);
								utilsUserFromRoH += improvementOfAttribute * averageOfXX * econValues.getByEntry(attribute);
							}
						}
					}
					
					// (operator profit also for operator that looses) 
					final double revenueNullfall = attributesNullfall.getByEntry(Attribute.priceUser) * amountNullfall ;
					final double revenuePlanfall = attributesPlanfall.getByEntry(Attribute.priceUser) * amountPlanfall ;
					final double operatorCostNullfall = attributesNullfall.getByEntry(Attribute.costOfProduction) * amountNullfall ;
					final double operatorCostPlanfall = attributesPlanfall.getByEntry(Attribute.costOfProduction) * amountPlanfall ;
					operatorProfit +=  -(revenueNullfall - operatorCostNullfall) + (revenuePlanfall - operatorCostPlanfall) ;
					
					
				} // mode				

			} // demand segment
		} // relation
		System.out.printf("%182s\n", "----------------" ) ;
		System.out.printf("bvwp benefit: %167.1f\n", utils ) ;
		System.out.printf("%182s\n", "================" ) ;
		//		System.out.printf("utl gain: %161.1f\n", utils ) ;

		html.beginTableMulticolumnRow() ;
		html.write("Summe") ;
		html.endTableRow() ;
		
		html.bvwpTableRow("Summe", "", "", "", "", "", "", "", Double.toString(utils) ) ;

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

//		html.endBody() ;
//		html.endHtml() ;
	}

	private double computeAndPrintGivingOrReceiving(double utils, Attributes econValues,
			Attributes attributesNullfall, Attributes attributesPlanfall, final double deltaAmounts, Html html) {
		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				final double attributeValuePlanfall = attributesPlanfall.getByEntry(attribute);
				final double attributeValueNullfall = attributesNullfall.getByEntry(attribute);

				UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, deltaAmounts, 
						attributeValueNullfall, attributeValuePlanfall, econValues.getByEntry(attribute));
				final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
				utils += utlChange ;


				if ( deltaAmounts > 0 ) {
					// wir sind aufnehmend; utl gains should be negative
					System.out.printf(FMT_STRING,attribute,
							attributeValueNullfall, 0., 
							attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
							attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
							utlChangesPerItem.utl , utlChange
					) ;
					html.bvwpTableRow(attribute.toString(),
							attributeValueNullfall, 0., 
							attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
							attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
							utlChangesPerItem.utl , utlChange) ;
				} else {
					// wir sind abgebend; utl gains should be positive
					System.out.printf(FMT_STRING, attribute,
							attributeValueNullfall, attributeValueNullfall * deltaAmounts,
							attributeValuePlanfall, 0., 
							-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
							// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
							// Nullfall)
							utlChangesPerItem.utl , utlChange
					) ;
					html.bvwpTableRow(attribute.toString(),
							attributeValueNullfall, attributeValueNullfall * deltaAmounts,
							attributeValuePlanfall, 0., 
							-attributeValueNullfall, attributeValueNullfall * deltaAmounts,
							// (selbst wenn sich das abgebende System ändert, so ist unser Gewinn dennoch basierend auf dem
							// Nullfall)
							utlChangesPerItem.utl , utlChange) ;
				}

			}
		}
		return utils;
	}

	private double computeAndPrintImplicitUtl(double utils,
			Attributes econValuesImprovedMode, Attributes attributesNullfallImprovedMode,
			Attributes attributesPlanfallImprovedMode,
			Attributes econValues,
			Attributes attributesNullfall,
			Attributes attributesPlanfall,
			final double deltaAmounts, Html html) {
		// implicit utility:
		final double implicitUtlImprovedMode = this.computeImplicitUtility(econValuesImprovedMode, 
				attributesNullfallImprovedMode, attributesPlanfallImprovedMode) ;

		final double implicitUtl = this.computeImplicitUtility(econValues, attributesNullfall, attributesPlanfall) ;

		if ( implicitUtl != 0. || implicitUtlImprovedMode != 0. ) {
			final double implicitUtlSum = implicitUtl - implicitUtlImprovedMode ;
			final double implicitUtlOverall = implicitUtlSum * deltaAmounts;
			System.out.printf(FMT_STRING,
					"implic. utl abg.",
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtl, implicitUtl*deltaAmounts 
			) ;
			System.out.printf(FMT_STRING,
					"implic. utl auf.",
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtlImprovedMode, -implicitUtlImprovedMode*deltaAmounts 
			) ;
			System.out.printf(FMT_STRING,
					"implicit utl",
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtlSum, implicitUtlOverall 
			) ;
			html.bvwpTableRow(					"implicit utl",
					0.,0.,
					0.,0.,
					0.,0.,
					implicitUtlSum, implicitUtlOverall 
			) ;
			utils += implicitUtlOverall ;

		}
		return utils;
	}

	private static Mode autodetectImprovingMode(Values nullfallForODRelation,
			Values planfallForODRelation, DemandSegment demandSegment) {
		// go through modes and determine the improved mode
		Mode improvedMode = null ;
		for ( Mode mode : Mode.values() ) { // for all modes
			ValuesForAMode quantitiesNullfallByMode = nullfallForODRelation.getByMode(mode) ;
			ValuesForAMode quantitiesPlanfallByMode = planfallForODRelation.getByMode(mode) ;
			Attributes quantitiesNullfall = quantitiesNullfallByMode.getByDemandSegment(demandSegment) ;
			Attributes quantitiesPlanfall = quantitiesPlanfallByMode.getByDemandSegment(demandSegment) ;
			for ( Attribute attribute : Attribute.values() ) {
				final double quantityNullfall = quantitiesNullfall.getByEntry(attribute);
				final double quantityPlanfall = quantitiesPlanfall.getByEntry(attribute);
				if ( attribute==Attribute.XX ) {
					if ( quantityPlanfall > quantityNullfall ) {
						improvedMode = mode ;
						break ;
					}
				}
				if ( quantityPlanfall < quantityNullfall ) {
					improvedMode = mode ;
					break ;
				}
			}
		}
		return improvedMode;
	}

	private double computeAndPrintValuesForAltnutzer(double utils,
			Attributes econValues,
			Attributes quantitiesNullfall,
			Attributes quantitiesPlanfall, double amountAltnutzer, Html html) {

		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				// yyyy not so great: if policy measure = price change, then RoH and resource consumption are
				// different here.  kai/benjamin, sep'12

				double deltaQuantities = quantitiesPlanfall.getByEntry(attribute)-quantitiesNullfall.getByEntry(attribute) ;
				final double utlChangePerItem = deltaQuantities * econValues.getByEntry(attribute);
				final double utlChange = utlChangePerItem * amountAltnutzer;
				System.out.printf(FMT_STRING,
						attribute,
						quantitiesNullfall.getByEntry(attribute), 
						quantitiesNullfall.getByEntry(attribute) * amountAltnutzer,
						quantitiesPlanfall.getByEntry(attribute), 
						quantitiesPlanfall.getByEntry(attribute) * amountAltnutzer,
						deltaQuantities , deltaQuantities * amountAltnutzer ,
						utlChangePerItem , 
						utlChange
				) ;
				html.bvwpTableRow(
						attribute.toString(),
						quantitiesNullfall.getByEntry(attribute), 
						quantitiesNullfall.getByEntry(attribute) * amountAltnutzer,
						quantitiesPlanfall.getByEntry(attribute), 
						quantitiesPlanfall.getByEntry(attribute) * amountAltnutzer,
						deltaQuantities , deltaQuantities * amountAltnutzer ,
						utlChangePerItem , 
						utlChange
				) ;
				utils += utlChange ;

			}
		}
		return utils;
	}
	abstract UtlChangesData utlChangePerEntry(Attribute attribute, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtility(Attributes econValues, Attributes quantitiesNullfall, 
			Attributes quantitiesPlanfall) ;
}