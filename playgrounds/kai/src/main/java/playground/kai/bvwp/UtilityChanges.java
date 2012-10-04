package playground.kai.bvwp;

import org.matsim.api.core.v01.Id;

import playground.kai.bvwp.Values.Attribute;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.DemandSegment;


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
				for ( DemandSegment demandSegment : DemandSegment.values() ) { // for all types (e.g. PV or GV)
					ValuesForAUserType econValues = econValuesByMode.getByDemandSegment(demandSegment) ;
					ValuesForAUserType quantitiesNullfall = quantitiesNullfallByMode.getByDemandSegment(demandSegment) ;
					ValuesForAUserType quantitiesPlanfall = quantitiesPlanfallByMode.getByDemandSegment(demandSegment) ;
					final double amountNullfall = quantitiesNullfall.getByEntry(Attribute.XX);
					final double amountPlanfall = quantitiesPlanfall.getByEntry(Attribute.XX);

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
									id, mode, demandSegment, amountAltnutzer );

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
						id, mode, demandSegment, deltaAmounts ) ;
						System.out.printf( "%17s || %17s | %17s || %17s | %17s || %17s | %17s || %17s | %17s ||\n",
								"Attribut",
								"Attribut Nullfall", "... mal Menge",
								"Attribut Planfall", "... mal Menge",
								"Attribut Diff", "... mal Menge",
								"Nutzen Diff", "... mal Menge") ;
						
						for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
							if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
								final double attributeValuePlanfall = quantitiesPlanfall.getByEntry(attribute);
								final double attributeValueNullfall = quantitiesNullfall.getByEntry(attribute);

								UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, deltaAmounts, 
										attributeValueNullfall, attributeValuePlanfall, econValues.getByEntry(attribute));
								final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
								utils += utlChange ;
								

								if ( deltaAmounts > 0 ) {
									// Wir sind aufnehmend!

									System.out.printf(fmtString,attribute,
											attributeValueNullfall, 0., 
											attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
											attributeValuePlanfall, attributeValuePlanfall * deltaAmounts,
											utlChangesPerItem.utl , utlChange
									) ;

								} else {
									// wir sind abgebend
									System.out.printf(fmtString, attribute,
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
					for ( Attribute attribute : Attribute.values() ) {
						if ( attribute!=Attribute.XX && attribute!=Attribute.costOfProduction ) {
							final double improvementOfAttribute = quantitiesPlanfall.getByEntry(attribute) - quantitiesNullfall.getByEntry(attribute);
							utilsUserFromRoH += improvementOfAttribute * averageOfXX * econValues.getByEntry(attribute);
						}
					}
					final double revenueNullfall = quantitiesNullfall.getByEntry(Attribute.priceUser) * amountNullfall ;
					final double revenuePlanfall = quantitiesPlanfall.getByEntry(Attribute.priceUser) * amountPlanfall ;
					final double operatorCostNullfall = quantitiesNullfall.getByEntry(Attribute.costOfProduction) * amountNullfall ;
					final double operatorCostPlanfall = quantitiesPlanfall.getByEntry(Attribute.costOfProduction) * amountPlanfall ;
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

		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				// yyyy not so great: if policy measure = price change, then RoH and resource consumption are
				// different here.  kai/benjamin, sep'12
				
				double deltaQuantities = quantitiesPlanfall.getByEntry(attribute)-quantitiesNullfall.getByEntry(attribute) ;
				final double utlChangePerItem = deltaQuantities * econValues.getByEntry(attribute);
				final double utlChange = utlChangePerItem * amountAltnutzer;
				System.out.printf(fmtString,
						attribute,
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
	abstract double computeImplicitUtility(ValuesForAUserType econValues, ValuesForAUserType quantitiesNullfall, 
			ValuesForAUserType quantitiesPlanfall) ;
}