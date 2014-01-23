package playground.vsp.bvwp;

import static playground.vsp.bvwp.Key.makeKey;

import org.matsim.api.core.v01.Id;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;

/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */
abstract class UtilityChanges {
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
		html.beginTable() ;


		double utils = 0. ;
		double utilsUserFromRoH = 0. ;
//		double operatorProfit = 0. ;
		for ( Id id : nullfall.getAllRelations() ) { // for all OD relations
			Utils.initializeOutputTables(html);				

			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			for ( DemandSegment segm : DemandSegment.values() ) { // for all types (e.g. PV or GV)

				Mode improvedMode = autodetectImprovingMode( nullfallForODRelation, planfallForODRelation, segm);

				if ( improvedMode == null ) {
					continue ; // means that in the demand segment, nothing has changed; goto next demand segment
				}

				Attributes econValuesReceiving = economicValues.getAttributes(improvedMode, segm) ;
				Attributes attributesNullfallReceiving = nullfallForODRelation.getAttributes(improvedMode, segm) ;
				Attributes attributesPlanfallReceiving = planfallForODRelation.getAttributes(improvedMode, segm) ;

				double sumSent = 0. ;

				for ( Mode mode : Mode.values() ) {

					Attributes 		econValues = economicValues.getAttributes(mode, segm) ;

					Attributes attributesNullfall = nullfallForODRelation.getAttributes(mode, segm) ;
					Attributes attributesPlanfall = planfallForODRelation.getAttributes(mode, segm) ;

					final Key key = makeKey(mode, segm, Attribute.XX);
					System.out.println( "key: " + key.toString() );
					System.out.flush(); 
					final double amountNullfall;
					final double amountPlanfall;
					final double deltaAmounts ;
					try{
						amountNullfall = nullfallForODRelation.get( key) ;
						amountPlanfall = planfallForODRelation.get( key) ;
						deltaAmounts = amountPlanfall - amountNullfall ;
					}
					catch (NullPointerException e) {
						System.err.println("Mode: " + mode + " lacks data - skipping.");
						continue;
					}

					if ( amountPlanfall==0. && amountNullfall==0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						continue ;
					}

					if ( mode.equals(improvedMode) ) {
						// Altnutzer:
						double amountAltnutzer = amountNullfall ;
						System.out.flush();
						System.err.println("writing verbleibend:");
						System.err.flush() ;
						Utils.writeSubHeaderVerbleibend(html, id, segm, mode, amountAltnutzer);
						utils += computeAndPrintValuesForAltnutzer(econValues, attributesNullfall, attributesPlanfall, amountAltnutzer, html);
					} else {
						sumSent += Math.abs( deltaAmounts ) ;
					}

					if ( mode != improvedMode ) {
						// compute completely on the side of the giving modes:
						Utils.writeSubHeaderVerlagert(html, id, segm, mode, deltaAmounts);
 
						utils += computeAndPrintGivingOrReceiving(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving, 
								econValues, attributesNullfall, attributesPlanfall, html);

						utils += computeAndPrintImplicitUtl(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving,
								econValues, attributesNullfall, attributesPlanfall, html);
					}

					// roh etc. stuff (for comparison):
					utilsUserFromRoH = computeUserBenefit(utilsUserFromRoH, econValues, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);

				} // mode			

				
				final double amountNullfallRcv = nullfallForODRelation.get( makeKey(improvedMode, segm, Attribute.XX)) ;
				final double amountPlanfallRcv = planfallForODRelation.get( makeKey(improvedMode, segm, Attribute.XX)) ;
				final double deltaAmountsRcv = amountPlanfallRcv - amountNullfallRcv ;
				final double amountInduced = deltaAmountsRcv - sumSent ;
				System.out.println( " amount induced: " + amountInduced ) ;
				Utils.writeSubHeaderInduziert(html, id, segm, improvedMode, amountInduced);
				double partialUtl = 0. ;
				
				for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
					if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
						// aufnehmende Seite:
						final double attributeValuePlanfallReceiving = attributesPlanfallReceiving.getByEntry(attribute);
						final double attributeValueNullfallReceiving = attributesNullfallReceiving.getByEntry(attribute);

						UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, amountInduced, 
								attributeValueNullfallReceiving, attributeValuePlanfallReceiving, econValuesReceiving.getByEntry(attribute));
						final double utlChange = utlChangesPerItem.utl * Math.abs(amountInduced);
						partialUtl += utlChange ;

						if ( utlChange!=0. ) {
							Utils.writeAufnehmendRow(html, -amountInduced, attribute, attributeValuePlanfallReceiving, utlChangesPerItem, utlChange);
						}
					}
				}

				final double implUtlInducedPerItem = this.computeImplicitUtilityPerItem( econValuesReceiving, 
						attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
				final double implUtlInduced = implUtlInducedPerItem * amountInduced ;


				if ( implUtlInduced != 0. ) {
					Utils.writeImplicitUtl(html, implUtlInducedPerItem, implUtlInduced, "impl utl ind");
				}
				partialUtl += implUtlInduced ;
				if ( partialUtl != 0. ) {
					Utils.writePartialSum(html, partialUtl );
				}
				utils += partialUtl ;


			} // demand segment
		} // relation

		Utils.writeSum(html, utils);

		double operatorProfit = 0. ;
		for ( Id id : nullfall.getAllRelations() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			for ( DemandSegment segm : DemandSegment.values() ) {
				for ( Mode mode : Mode.values() ) {
					Attributes attributesNullfall = nullfallForODRelation.getAttributes(mode, segm) ;
					Attributes attributesPlanfall = planfallForODRelation.getAttributes(mode, segm) ;
					double amountNullfall = attributesNullfall.getByEntry(Attribute.XX) ;
					double amountPlanfall = attributesPlanfall.getByEntry(Attribute.XX) ;
					double operatorProfitBefore = operatorProfit ;
					operatorProfit = computeOperatorProfit(operatorProfit, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);
					if ( operatorProfit != operatorProfitBefore ){
						html.beginTableMulticolumnRow();
						html.write( "Operator profit gain; " + mode.toString() + ": " + (operatorProfit-operatorProfitBefore) );
						html.endTableRow();
					}
				}
			}
		}


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

	private double computeAndPrintGivingOrReceiving(Attributes econValuesReceiving, Attributes attributesNullfallReceiving,
			Attributes attributesPlanfallReceiving, Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall, Html html) {

		double utils = 0. ;

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX) ;
		// negative, since this is never called for the receiving mode

		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			double partialUtils = 0. ;
			if ( attribute != Attribute.XX && attribute != Attribute.priceUser ) {
				{
					// abgebende Seite:
					final double attributeValuePlanfall = attributesPlanfall.getByEntry(attribute);
					final double attributeValueNullfall = attributesNullfall.getByEntry(attribute);

					UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, deltaAmounts, 
							attributeValueNullfall, attributeValuePlanfall, econValues.getByEntry(attribute));
					final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
					partialUtils += utlChange ;

					if ( utlChange!=0. ) {
						Utils.writeAbgebendRow(html, deltaAmounts, attribute, attributeValueNullfall, attributeValuePlanfall, utlChangesPerItem, utlChange);
					}
				}
				{
					// aufnehmende Seite:
					final double attributeValuePlanfallReceiving = attributesPlanfallReceiving.getByEntry(attribute);
					final double attributeValueNullfallReceiving = attributesNullfallReceiving.getByEntry(attribute);

					UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, -deltaAmounts, 
							attributeValueNullfallReceiving, attributeValuePlanfallReceiving, econValues.getByEntry(attribute));
					final double utlChange = utlChangesPerItem.utl * Math.abs(deltaAmounts);
					partialUtils += utlChange ;

					if ( utlChange!=0. ) {
						// wir sind aufnehmend; utl gains should be negative
						Utils.writeAufnehmendRow(html, -deltaAmounts, attribute, attributeValuePlanfallReceiving, utlChangesPerItem, utlChange);
					}
				}
			}
			if ( partialUtils != 0. ) {
				Utils.writePartialSum(html, partialUtils) ;
			}
			utils += partialUtils ;
		}
//		if ( utils != 0. ) {
//			Utils.writePartialSum(html, utils);
//		}
		return utils;
	}

	private double computeAndPrintImplicitUtl(Attributes econValuesReceiving, Attributes attributesNullfallReceiving, Attributes attributesPlanfallReceiving, 
			Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall, Html html) {

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX ) ;
		// negative, since this is never called for the receiving mode

		final double implicitUtlPerItem = this.computeImplicitUtilityPerItem(econValues, attributesNullfall, attributesPlanfall) ;
		// probably positive

		final double implicitUtlOverall = - implicitUtlPerItem * Math.abs(deltaAmounts) ;
		if ( implicitUtlOverall != 0. ) {
			Utils.writeImplicitUtl(html, implicitUtlPerItem, implicitUtlOverall, "impl utl giv");
		}

		final double implicitUtlPerItemReceiving = this.computeImplicitUtilityPerItem( econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
		// probably positive

		final double implicitUtlOverallReceiving = implicitUtlPerItemReceiving * Math.abs(deltaAmounts) ;
		if ( implicitUtlOverallReceiving != 0. ) {
			Utils.writeImplicitUtl( html, implicitUtlPerItemReceiving, implicitUtlOverallReceiving, "impl utl rcv" ) ;
		}

		double util = implicitUtlOverall + implicitUtlOverallReceiving ;

		if ( util != 0. ) {
			Utils.writePartialSum(html, util);
		}

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

				Utils.writeAltnutzerRow(quantitiesNullfall, quantitiesPlanfall, amountAltnutzer, html, attribute, deltaQuantities,
						utlChangePerItem, utlChange);
				utils += utlChange ;

			}
		}
		if ( utils != 0. ) {
			Utils.writePartialSum(html, utils);
		}
		return utils;
	}

	abstract UtlChangesData utlChangePerEntry(Attribute attribute, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtilityPerItem(Attributes econValues, Attributes quantitiesNullfall, 
			Attributes quantitiesPlanfall) ;
}