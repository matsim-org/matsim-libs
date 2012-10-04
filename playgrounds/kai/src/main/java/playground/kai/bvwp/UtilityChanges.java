package playground.kai.bvwp;

import org.matsim.api.core.v01.Id;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;


/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */
abstract class UtilityChanges {
	UtilityChanges() {
		System.out.println("\n==================================================================================================================================");
		System.out.println("Setting utility computation method to " + this.getClass() ) ;
	}
	
	final void utilityChange( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall ) {
		// (GK-GK') * x + 0.5 * (GK-GK') (x'-x) =
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )

		double utils = 0. ;
		double utilsUserFromRoH = 0. ;
		double operatorProfit = 0. ;

		for ( Id id : nullfall.values.keySet() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.values.get(id) ;
			Values planfallForODRelation = planfall.values.get(id) ;
			for ( Mode mode : Mode.values() ) { // for all modes
				ValuesForAMode econValuesByMode = economicValues.getByMode(mode) ;
				ValuesForAMode quantitiesNullfallByMode = nullfallForODRelation.getByMode(mode) ;
				ValuesForAMode quantitiesPlanfallByMode = planfallForODRelation.getByMode(mode) ;
				for ( Type type : Type.values() ) { // for all types (e.g. PV or GV)
					ValuesForAUserType econValues = econValuesByMode.getByDemandSegment(type) ;
					ValuesForAUserType quantitiesNullfall = quantitiesNullfallByMode.getByDemandSegment(type) ;
					ValuesForAUserType quantitiesPlanfall = quantitiesPlanfallByMode.getByDemandSegment(type) ;
					final double amountNullfall = quantitiesNullfall.getByEntry(Entry.XX);
					final double amountPlanfall = quantitiesPlanfall.getByEntry(Entry.XX);

					if ( amountPlanfall!=0. || amountNullfall!=0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						
						// ###############
						// ALTNUTZER
						final String fmtString = "%17s || %17.2f | %17.1f || %17.2f | %17.1f || %17.2f | %17.1f || %17.2f | %17.1f ||\n" ;
						if ( amountNullfall <= amountPlanfall ) {
							// (wir haben (relevante) Altnutzer nur auf dem nehmenden Verkehrsmittel!)
							// (cannot use "<" since there are (illustrative) scenarios where nobody switches.  kn, sep'12)
							
							double amountAltnutzer = amountNullfall ;
							
							System.out.printf("%17s; %17s; %17s; verbleibender Verkehr: %17.1f Personen/Tonnen\n", 
									id, mode, type, amountAltnutzer );

							utils = computeAndPrintValuesForAltnutzer(utils,
									econValues, quantitiesNullfall,
									quantitiesPlanfall, fmtString,
									amountAltnutzer);
							
						}
						
						// ###########
						// abgegeben oder aufgenommen
						
						double implicitUtl = this.computeImplicitUtility(econValues, quantitiesNullfall, quantitiesPlanfall) ;
						
						double deltaAmounts = amountPlanfall - amountNullfall ;

						System.out.printf("%17s; %17s; %17s; " +
						"wechselnder Verkehr: %17.1f Personen/Tonnen\n", 
						id, mode, type, deltaAmounts ) ;
						System.out.printf( "%17s || %17s | %17s || %17s | %17s || %17s | %17s || %17s | %17s ||\n",
								"Attribut",
								"Attribut Nullfall", "... mal Menge",
								"Attribut Planfall", "... mal Menge",
								"Attribut Diff", "... mal Menge",
								"Nutzen Diff", "... mal Menge") ;
						
						for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
							if ( entry != Entry.XX && entry != Entry.priceUser ) {
								final double attributeValuePlanfall = quantitiesPlanfall.getByEntry(entry);
								final double attributeValueNullfall = quantitiesNullfall.getByEntry(entry);

								UtlChangesData utlChangesPerItem = utlChangePerEntry(entry, deltaAmounts, 
										attributeValueNullfall, attributeValuePlanfall, econValues.getByEntry(entry));
								final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
								utils += utlChange ;
								

								if ( deltaAmounts > 0 ) {
									// Wir sind aufnehmend!

									System.out.printf(fmtString,entry,
											attributeValueNullfall, 0., 
											attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
											attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
											utlChangesPerItem.utl , utlChange
									) ;

								} else {
									// wir sind abgebend
									System.out.printf(fmtString, entry,
											attributeValueNullfall, attributeValueNullfall * deltaAmounts,
											attributeValuePlanfall, 0., 
											attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
											utlChangesPerItem.utl , utlChange
									) ;
								}

							}
						}
						if ( implicitUtl != 0. ) {
							final double implicitUtlOverall = implicitUtl * deltaAmounts;
							System.out.printf(fmtString,
									"implicit utl",
									0.,0.,
									0.,0.,
									0.,0.,
									implicitUtl, implicitUtlOverall 
									) ;
							utils += implicitUtlOverall ;

						}
					}
					// compute user gains according to RoH: improvement * <x>
					final double averageOfXX = (amountPlanfall + amountNullfall)/2. ;
					for ( Entry entry : Entry.values() ) {
						if ( entry!=Entry.XX && entry!=Entry.costOfProduction ) {
							final double improvementOfAttribute = quantitiesPlanfall.getByEntry(entry) - quantitiesNullfall.getByEntry(entry);
							utilsUserFromRoH += improvementOfAttribute * averageOfXX * econValues.getByEntry(entry);
						}
					}
					final double revenueNullfall = quantitiesNullfall.getByEntry(Entry.priceUser) * amountNullfall ;
					final double revenuePlanfall = quantitiesPlanfall.getByEntry(Entry.priceUser) * amountPlanfall ;
					final double operatorCostNullfall = quantitiesNullfall.getByEntry(Entry.costOfProduction) * amountNullfall ;
					final double operatorCostPlanfall = quantitiesPlanfall.getByEntry(Entry.costOfProduction) * amountPlanfall ;
					operatorProfit +=  -(revenueNullfall - operatorCostNullfall) + (revenuePlanfall - operatorCostPlanfall) ;
				}
			}
		}
		System.out.printf("%182s\n", "----------------" ) ;
		System.out.printf("bvwp benefit: %167.1f\n", utils ) ;
		System.out.printf("%182s\n", "================" ) ;
//		System.out.printf("utl gain: %171.1f\n", utils ) ;
		System.out.printf("RoH: utl gain users: %17.1f ; operator profit gain: %17.1f ; sum: %17.1f\n", 
				utilsUserFromRoH, operatorProfit, utilsUserFromRoH+operatorProfit ) ; 
	}

	private double computeAndPrintValuesForAltnutzer(double utils,
			ValuesForAUserType econValues,
			ValuesForAUserType quantitiesNullfall,
			ValuesForAUserType quantitiesPlanfall, final String fmtString,
			double amountAltnutzer) {
		System.out.printf( "%17s || %17s | %17s || %17s | %17s || %17s | %17s || %17s | %17s ||\n",
				"Attribut",
				"Attribut Nullfall", "... mal Menge",
				"Attribut Planfall", "... mal Menge",
				"Attribut Diff", "... mal Menge",
				"Nutzen Diff", "... mal Menge") ;

		for ( Entry entry : Entry.values() ) { // for all entries (e.g. km or hrs)
			if ( entry != Entry.XX && entry != Entry.priceUser ) {
				// yyyy not so great: if policy measure = price change, then RoH and resource consumption are
				// different here.  kai/benjamin, sep'12
				
				double deltaQuantities = quantitiesPlanfall.getByEntry(entry)-quantitiesNullfall.getByEntry(entry) ;
				final double utlChangePerItem = deltaQuantities * econValues.getByEntry(entry);
				final double utlChange = utlChangePerItem * amountAltnutzer;
				System.out.printf(fmtString,
						entry,
						quantitiesNullfall.getByEntry(entry), 
						quantitiesNullfall.getByEntry(entry) * amountAltnutzer,
						quantitiesPlanfall.getByEntry(entry), 
						quantitiesPlanfall.getByEntry(entry) * amountAltnutzer,
						deltaQuantities , deltaQuantities * amountAltnutzer ,
						utlChangePerItem , 
						utlChange
				) ;
				utils += utlChange ;

			}
		}
		return utils;
	}
	abstract UtlChangesData utlChangePerEntry(Entry entry, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtility(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall) ;
}