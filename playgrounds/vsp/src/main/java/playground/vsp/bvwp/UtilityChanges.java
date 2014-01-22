package playground.vsp.bvwp;

import static playground.vsp.bvwp.Key.makeKey;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.ChangeType;
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
		Html totalHtml = new Html("total");
		computeAndPrintResults(economicValues,nullfall,planfall,html, totalHtml) ;
	}

	final void computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall, Html html, Html totalHtml ) {
		// (GK-GK') * x + 0.5 * (GK-GK') (x'-x) =
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )

		// yyyy these two can't be here if html is initialized in user code!
		html.beginHtml() ;
		html.beginBody() ;
		html.beginTable() ;
		
		totalHtml.beginHtml() ;
		totalHtml.beginBody() ;
		totalHtml.beginTable() ;
		
		

		HashMap<Mode,Double> modularUtils = new HashMap<Mode,Double>();
		HashMap<Mode,Double> modularInducedUtils = new HashMap<Mode,Double>();
		HashMap<Mode,Double> modularVerlagertUtils = new HashMap<Mode,Double>();
		HashMap<Mode,Double> modularImplInducedUtils = new HashMap<Mode,Double>();
		HashMap<Mode,Double> modularImplVerlagertUtils = new HashMap<Mode,Double>();

		
		
		// total
		double utils = 0. ;

		
		double utilsUserFromRoH = 0. ;
		double operatorProfit = 0. ;
		
		for ( Id id : nullfall.getAllRelations() ) { // for all OD relations
			Utils.initializeOutputTables(html);				
			
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			boolean verbleibendGerechnet = false;
			for ( DemandSegment segm : DemandSegment.values() ) { // for all types (e.g. PV or GV)
				
				
				Mode improvedMode = autodetectImprovingMode( nullfallForODRelation, planfallForODRelation, segm);

				if ( improvedMode == null ) {
					continue ; // means that in the demand segment, nothing has changed; goto next demand segment
				}

				Attributes econValuesReceiving = economicValues.getAttributes(improvedMode, segm) ;
				Attributes attributesNullfallReceiving = nullfallForODRelation.getAttributes(improvedMode, segm) ;
				Attributes attributesPlanfallReceiving = planfallForODRelation.getAttributes(improvedMode, segm) ;
				
				double sumSent = 0. ;

				for ( Mode mode : Mode.values() )
				{
					for (ChangeType type : ChangeType.values())
					{ // for all modes
							
					
					Attributes 		econValues = economicValues.getAttributes(mode, segm) ;
					
					Attributes attributesNullfall = nullfallForODRelation.getAttributes(mode, segm, type) ;
					Attributes attributesPlanfall = planfallForODRelation.getAttributes(mode, segm, type) ;

					final Key key = makeKey(mode, segm, Attribute.XX, type);
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
					System.out.flush();
					System.err.println(" amountPlanfall: " + amountPlanfall + " amountNullfall: " + amountNullfall );
					System.err.flush() ;

					if ( amountPlanfall==0. && amountNullfall==0. ) {
						// (suppress output if this (relation,mode,demand_segment) is never used)
						continue ;
					}

					System.out.flush();
					System.err.println(" mode: " + mode + " improvedMode: " + improvedMode );
					System.err.flush() ;

					if (!verbleibendGerechnet)
					{
					if ( mode.equals(improvedMode) ) {
						// Altnutzer:
						Double verbleibendUpToNow;
						verbleibendUpToNow = modularUtils.get(mode);
						if (verbleibendUpToNow == null) {	
							verbleibendUpToNow = 0.;
						}
						
						
						double amountAltnutzer = amountNullfall ;
						System.out.flush();
						System.err.println("writing verbleibend:");
						System.err.flush() ;
						Utils.writeSubHeaderVerbleibend(html, id, segm, mode, amountAltnutzer);
						double verbleibend = computeAndPrintValuesForAltnutzer(econValues, attributesNullfall, attributesPlanfall, amountAltnutzer, html);
						utils += verbleibend;
						verbleibendUpToNow += verbleibend;
						modularUtils.put(mode, verbleibendUpToNow);
					} else {
						sumSent += Math.abs( deltaAmounts ) ;
					}
						verbleibendGerechnet = true;
					}
					
				
					
					if (type == ChangeType.VERLAGERT){
						Double verlagertUpToNow;
						
						verlagertUpToNow = modularVerlagertUtils.get(mode);
						if (verlagertUpToNow == null) {	verlagertUpToNow = 0.;
						}
					
					Utils.writeSubHeaderVerlagert(html, id, segm, mode, deltaAmounts);
					double currentVerlUtil = computeAndPrintGivingOrReceiving(econValues, attributesNullfall, attributesPlanfall, html);
					
					verlagertUpToNow+= currentVerlUtil;
					modularVerlagertUtils.put(mode, verlagertUpToNow);
					utils += currentVerlUtil;
					}
					
					
					else if (type == ChangeType.INDUZIERT){
						Double induziertUpToNow;
						
						induziertUpToNow = modularInducedUtils.get(mode);
						if (induziertUpToNow == null) {	induziertUpToNow = 0.;
						}
						Utils.writeSubHeaderInduziert(html, id, segm, mode, deltaAmounts);
						double currentIndUtil = computeAndPrintGivingOrReceiving(econValues, attributesNullfall, attributesPlanfall, html);
						induziertUpToNow+= currentIndUtil;
						modularInducedUtils.put(mode, induziertUpToNow);
						utils += currentIndUtil;
						
						
						
						System.out.println( " amount induced: " + deltaAmounts ) ;
						
						final double implUtlInducedPerItem = this.computeImplicitUtilityPerItem( econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
						final double implUtlInduced = implUtlInducedPerItem * deltaAmounts ;
						
						
						if ( implUtlInduced != 0. ) {
							Utils.writeImplicitUtl(html, implUtlInducedPerItem, implUtlInduced, "impl utl ind");
							Utils.writePartialSum(html, implUtlInduced );
						}
						Double implInduziertUtil;
						implInduziertUtil = modularImplInducedUtils.get(mode);
						if (implInduziertUtil == null ) {
							implInduziertUtil = 0.;
						}
						implInduziertUtil += implUtlInduced;
						modularImplInducedUtils.put(mode, implInduziertUtil);
						utils += implUtlInduced ;
						
					}
					
					//Verlagert
					if ( mode != improvedMode ) {
						
						Double implVerlagertUtil;
						implVerlagertUtil = modularImplVerlagertUtils.get(mode);
						if (implVerlagertUtil == null)
							{
							implVerlagertUtil = 0.;
							}
							
						
						double u=computeAndPrintImplicitUtl(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving,
								econValues, attributesNullfall, attributesPlanfall, html);
					
						// compute implicit uti completely on the side of the giving modes:
						implVerlagertUtil += u;
						modularImplVerlagertUtils.put(mode, implVerlagertUtil);
						utils += u;
					}

					// roh etc. stuff (for comparison):
					utilsUserFromRoH = computeUserBenefit(utilsUserFromRoH, econValues, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);
					operatorProfit = computeOperatorProfit(operatorProfit, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);
					} //changeType
				} // mode			
				


			} // demand segment
		} // relation
		
		
		Utils.writeSum(html, utils);
		
		Utils.writeVerlagertSum(html, modularVerlagertUtils);
		Utils.writeImplVerlagertSum(html, modularImplVerlagertUtils);
		Utils.writeInduziertSum(html, modularInducedUtils);
		Utils.writeImplInduziertSum(html, modularImplInducedUtils);
		Utils.writeOverallOutputTable(totalHtml, modularUtils, modularVerlagertUtils,modularImplVerlagertUtils,modularInducedUtils, modularImplInducedUtils);
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
					Utils.writeAufnehmendRow(html, deltaAmounts, attribute, attributeValuePlanfall, utlChangesPerItem, utlChange);
				} else {
					// wir sind abgebend; utl gains should be positive
					Utils.writeAbgebendRow(html, deltaAmounts, attribute, attributeValuePlanfall, attributeValueNullfall,
							utlChangesPerItem, utlChange);
				}

			}
		}
		if ( utils != 0. ) {
			Utils.writePartialSum(html, utils);
		}
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
			Utils.writeImplicitUtl(html, implicitUtlPerItem, implicitUtlOverall, "implicit utl frm");
		}

		final double implicitUtlPerItemReceiving = this.computeImplicitUtilityPerItem( econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
		// probably positive
		
		final double implicitUtlOverallReceiving = implicitUtlPerItemReceiving * Math.abs(deltaAmounts) ;
		if ( implicitUtlOverallReceiving != 0. ) {
			Utils.writeImplicitUtl( html, implicitUtlPerItemReceiving, implicitUtlOverallReceiving, "implicit utl to" ) ;
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