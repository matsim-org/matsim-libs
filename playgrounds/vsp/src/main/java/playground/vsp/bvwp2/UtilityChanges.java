package playground.vsp.bvwp2;

import static playground.vsp.bvwp2.Key.makeKey;

import org.matsim.api.core.v01.Id;

import playground.vsp.bvwp2.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp2.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp2.MultiDimensionalArray.Mode;

/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */
abstract class UtilityChanges {
	static final String FMT_STRING = "%16s || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %16.1f || %16.2f | %12.1f  mio||\n";
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
		for ( Id id : nullfall.getAllRelations() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			for ( DemandSegment segm : DemandSegment.values() ) { // for all types (e.g. PV or GV)

				Mode improvedMode = autodetectImprovingMode( nullfallForODRelation, planfallForODRelation, segm);
//				System.out.println( "autodetected improved mode: " + improvedMode );

				if ( improvedMode == null ) {
					continue ; // means that in the demand segment, nothing has changed; goto next demand segment
				}

				Utils.initializeOutputTables(html);				
				
				Attributes econValuesReceiving = economicValues.getAttributes(improvedMode, segm) ;
				Attributes attributesNullfallReceiving = nullfallForODRelation.getAttributes(improvedMode, segm) ;
				Attributes attributesPlanfallReceiving = planfallForODRelation.getAttributes(improvedMode, segm) ;

				for ( Mode mode : Mode.values() ) { // for all modes

					Attributes econValues = economicValues.getAttributes(mode, segm) ;
					Attributes attributesNullfall = nullfallForODRelation.getAttributes(mode, segm) ;
					Attributes attributesPlanfall = planfallForODRelation.getAttributes(mode, segm) ;

					final double amountNullfall = nullfallForODRelation.get( makeKey(mode, segm, Attribute.XX)) ;
					final double amountPlanfall = planfallForODRelation.get( makeKey(mode, segm, Attribute.XX)) ;
					final double deltaAmounts = amountPlanfall - amountNullfall ;

					if ( amountPlanfall==0. && amountNullfall==0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						continue ;
					}

					if ( mode==improvedMode ) {
						// Altnutzer:
						double amountAltnutzer = amountNullfall ;
						Utils.writeSubHeaderVerbleibend(html, id, segm, mode, amountAltnutzer);
						utils += computeAndPrintValuesForAltnutzer(econValues, attributesNullfall, attributesPlanfall, amountAltnutzer, html);
					}

					Utils.writeSubHeaderWechselnd(html, id, segm, mode, deltaAmounts);
					
					utils += computeAndPrintGivingOrReceiving(econValues, attributesNullfall, attributesPlanfall, html);

					if ( mode != improvedMode ) {
						// compute implicit uti completely on the side of the giving modes:
						utils += computeAndPrintImplicitUtl(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving,
								econValues, attributesNullfall, attributesPlanfall, html);
					}

					// roh etc. stuff (for comparison):
					utilsUserFromRoH = computeUserBenefit(utilsUserFromRoH, econValues, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);
					operatorProfit = computeOperatorProfit(operatorProfit, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);

				} // mode				

			} // demand segment
		} // relation

		Utils.writeSum(html, utils);

		Utils.writeRohAndEndOutput(html, utilsUserFromRoH, operatorProfit);
	}

	private double computeUserBenefit(double utilsUserFromRoH, Attributes econValues, Attributes attributesNullfall,
			Attributes attributesPlanfall, final double amountNullfall, final double amountPlanfall) {
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
		return utilsUserFromRoH;
	}

	private double computeOperatorProfit(double operatorProfit, Attributes attributesNullfall, Attributes attributesPlanfall,
			final double amountNullfall, final double amountPlanfall) {
		{
		// (operator profit also for operator that looses) 
		final double revenueNullfall = attributesNullfall.getByEntry(Attribute.priceUser) * amountNullfall ;
		final double revenuePlanfall = attributesPlanfall.getByEntry(Attribute.priceUser) * amountPlanfall ;
		final double operatorCostNullfall = attributesNullfall.getByEntry(Attribute.costOfProduction) * amountNullfall ;
		final double operatorCostPlanfall = attributesPlanfall.getByEntry(Attribute.costOfProduction) * amountPlanfall ;
		operatorProfit +=  -(revenueNullfall - operatorCostNullfall) + (revenuePlanfall - operatorCostPlanfall) ;
		}
		return operatorProfit;
	}

	private double computeAndPrintGivingOrReceiving(Attributes econValues, Attributes attributesNullfall,
			Attributes attributesPlanfall, Html html) {
		
		double utils = 0. ;

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX) ;

		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				final double attributeValuePlanfall = attributesPlanfall.getByEntry(attribute);
				final double attributeValueNullfall = attributesNullfall.getByEntry(attribute);

				UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, deltaAmounts, 
						attributeValueNullfall, attributeValuePlanfall, econValues.getByEntry(attribute));
				final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
				utils += utlChange ;

				if ( utlChange==0. ) {
					continue ;
				}
				if ( deltaAmounts > 0 ) {
					// wir sind aufnehmend; utl gains should be negative
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
							utlChangesPerItem.utl , utlChange) ;
				} else {
					// wir sind abgebend; utl gains should be positive
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
							utlChangesPerItem.utl , utlChange) ;
				}

			}
		}
		Utils.writePartialSum(html, utils);
		return utils;
	}

	private double computeAndPrintImplicitUtl(Attributes econValuesReceiving, Attributes attributesNullfallReceiving, Attributes attributesPlanfallReceiving, 
			Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall, Html html) {

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX ) ;
		// negative, since this is never called for the receiving mode

		final double implicitUtlPerItem = this.computeImplicitUtilityPerItem(econValues, attributesNullfall, attributesPlanfall) ;
		// probably positive
		
		final double implicitUtlOverall = - implicitUtlPerItem * Math.abs(deltaAmounts) ;
		Utils.writeImplicitUtl(html, implicitUtlPerItem, implicitUtlOverall, "implicit utl frm");

		final double implicitUtlPerItemReceiving = this.computeImplicitUtilityPerItem( econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
		// probably positive
		
		final double implicitUtlOverallReceiving = implicitUtlPerItemReceiving * Math.abs(deltaAmounts) ;
		Utils.writeImplicitUtl( html, implicitUtlPerItemReceiving, implicitUtlOverallReceiving, "implicit utl to" ) ;
		
		double util = implicitUtlOverall + implicitUtlOverallReceiving ;
		
		Utils.writePartialSum(html, util);

		return util ;
	}

	private static Mode autodetectImprovingMode(Values nullfallForODRelation, Values planfallForODRelation, DemandSegment segm) {
		// go through modes and determine the improved mode
		Mode improvedMode = null ;
		for ( Mode mode : Mode.values() ) { // for all modes
			Attributes quantitiesNullfall = nullfallForODRelation.getAttributes(mode, segm) ;
			Attributes quantitiesPlanfall = planfallForODRelation.getAttributes(mode, segm) ;
			for ( Attribute attribute : Attribute.values() ) {
				final double quantityNullfall = quantitiesNullfall.getByEntry(attribute);
				final double quantityPlanfall = quantitiesPlanfall.getByEntry(attribute);
				if ( attribute==Attribute.XX &&  quantityPlanfall > quantityNullfall ) {
						improvedMode = mode ;
						break ;
				}
				if ( attribute!=Attribute.XX && quantityPlanfall < quantityNullfall ) {
					improvedMode = mode ;
					break ;
				}
			}
		}
		return improvedMode;
	}

	private double computeAndPrintValuesForAltnutzer( Attributes econValues,
			Attributes quantitiesNullfall,
			Attributes quantitiesPlanfall, double amountAltnutzer, Html html) {

		double utils = 0. ;
		
		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				// not so great: if policy measure = price change, then RoH and resource consumption are
				// different here.  kai/benjamin, sep'12
				// Aber ist das nicht richtig: Eine Preisänderung bewirkt nur eine Veränderung der Einnahme-Aufteilung; roh geht (z.B.) hoch,
				// producer surplus geht runter, vw'lich keine Änderung.  kai, oct'13

				double deltaQuantities = quantitiesPlanfall.getByEntry(attribute)-quantitiesNullfall.getByEntry(attribute) ;
				final double utlChangePerItem = deltaQuantities * econValues.getByEntry(attribute);
				final double utlChange = utlChangePerItem * amountAltnutzer;
				if ( utlChange == 0. ) {
					continue ;
				}

				System.out.printf(FMT_STRING,
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
						utlChange
						) ;
				utils += utlChange ;

			}
		}
		Utils.writePartialSum(html, utils);
		return utils;
	}
	abstract UtlChangesData utlChangePerEntry(Attribute attribute, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtilityPerItem(Attributes econValues, Attributes quantitiesNullfall, 
			Attributes quantitiesPlanfall) ;
}