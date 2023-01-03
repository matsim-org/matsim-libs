package playground.vsp.bvwp;

import static playground.vsp.bvwp.Key.makeKey;

import java.util.HashMap;
import java.util.Map;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;

/**
 * Class that provides services.  To be maintained by ``programmers''.
 * 
 * @author nagel
 */



abstract class UtilityChanges {
	
	Map<Mode, Map<Attribute,Double>> verbleibendRV = new HashMap<MultiDimensionalArray.Mode, Map<Attribute,Double>>();
	
	Map<Mode, Map<Attribute,Double>> verlagertRVAb = new HashMap<MultiDimensionalArray.Mode, Map<Attribute,Double>>();
	Map<Mode, Map<Attribute,Double>> verlagertRVAuf = new HashMap<MultiDimensionalArray.Mode, Map<Attribute,Double>>();
	
	Map<Mode, Double[]> verlagertNMAb = new HashMap<>();
	Map<Mode, Double[]> verlagertNMAuf = new HashMap<>();
	
    //verlagertNMAb/Auf: 0 - personen 1- personenKM 2 - pkw-KM 3 - personen-h 4 - pkw-h 5 - nutzerkosten  
	
	Map<Mode, Double> verlagertImpAb = new HashMap<MultiDimensionalArray.Mode, Double>();
	Map<Mode, Double> verlagertImpAuf = new HashMap<MultiDimensionalArray.Mode, Double>();
	
	Map<Mode, Map<Attribute,Double>> induziertRV = new HashMap<MultiDimensionalArray.Mode, Map<Attribute,Double>>();
	Map<Mode, Double> induziertImp = new HashMap<MultiDimensionalArray.Mode, Double>();
    Double[] induziertNM = {0.,0.,0.,0.,0.,0.};

	

    private HashMap<DemandSegment, Double> besetzungsgradeKurz;
    private HashMap<DemandSegment, Double> besetzungsgradeLang;

	
	UtilityChanges() {
		System.out.println("Setting utility computation method to " + this.getClass() ) ;
		fillBesetzungsgrad();
		for (Mode mode : Mode.values()){
		    Double[] da = {0.,0.,0.,0.,0.,0.};
		    verlagertNMAb.put(mode, da);
		    Double[] db = {0.,0.,0.,0.,0.,0.};
		    verlagertNMAuf.put(mode, db);

		}
		
	}

	final void computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall ) {
		Html html = new Html() ;
		html.beginHtml() ;
		html.beginBody() ;
		html.beginTable() ;
		
		Html totalHtml = new Html("total");
		totalHtml.beginHtml() ;
		totalHtml.beginBody() ;
		totalHtml.beginTable() ;

		computeAndPrintResults(economicValues,nullfall,planfall,html, totalHtml) ;
	}
	
	final double computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall, String outFileName ) {
		Html html = new Html(outFileName) ;
		html.beginHtml() ;
		html.beginBody() ;
		html.beginTable() ;
		Html totalHtml = new Html(outFileName+"_total");
		totalHtml.beginHtml() ;
		totalHtml.beginBody() ;
		totalHtml.beginTable() ;
		return computeAndPrintResults(economicValues,nullfall,planfall,html, totalHtml) ;
	}

	final double computeAndPrintResults( Values economicValues, ScenarioForEvalData nullfall, ScenarioForEvalData planfall, Html html, Html totalHtml ) {
		// (GK-GK') * x + 0.5 * (GK-GK') (x'-x) =
		// 0.5 * (GK-GK') (x+x') = 0.5 * ( GK*x + GK*x' - GK'*x - GK'*x' )
		double utils = 0. ;
		double utilsUserFromRoHOldUsers = 0. ;
		double utilsUserFromRoHNewUsers= 0. ;

		
		for ( String id : nullfall.getAllRelations() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Utils.initializeOutputTables(html);				
			
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

					Attributes econValues = economicValues.getAttributes(mode, segm) ;

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
						final double utilsOld = utils;
						utils += computeAndPrintValuesForAltnutzer(econValues, attributesNullfall, mode, attributesPlanfall, amountAltnutzer, html);
					} else {
						sumSent += Math.abs( deltaAmounts ) ;
					}

					if ( mode != improvedMode ) {
						// compute completely on the side of the giving modes:
						Utils.writeSubHeaderVerlagert(html, id, segm, mode, deltaAmounts);

						final double utilsBefore = utils ;

						utils += computeAndPrintGivingOrReceiving(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving, 
								econValues, attributesNullfall, attributesPlanfall, mode, html, segm);
						
						if ( utils != utilsBefore ) {
							Utils.writePartialSum(html, "Nutzen&auml;nderung aus &Auml;nderung Ressourcenverzehr bei Verlagerung:", utils - utilsBefore);
						}
						
						final double utilsImp =computeAndPrintImplicitUtl(econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving,
								econValues, attributesNullfall, attributesPlanfall, mode, html);
						utils += utilsImp;

//						if ( utils != utilsBefore ) {
							Utils.writePartialSum(html, "Nutzen&auml;nderung bei Verlagerung gesamt:" , utils - utilsBefore);
//						}

					}

					// roh etc. stuff (for comparison):
					utilsUserFromRoHOldUsers += computeUserBenefitsOldUsers(econValues, attributesNullfall, attributesPlanfall, amountNullfall);
					utilsUserFromRoHNewUsers += computeUserBenefitsNewUsers(econValues, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);

				} // mode			
				double deltaAmountsRcv = 0.;
				try {
				final double amountNullfallRcv = nullfallForODRelation.get( makeKey(improvedMode, segm, Attribute.XX)) ;
				final double amountPlanfallRcv = planfallForODRelation.get( makeKey(improvedMode, segm, Attribute.XX)) ;
				deltaAmountsRcv = amountPlanfallRcv - amountNullfallRcv; }
				catch (NullPointerException e) {
					System.err.println("Relation: " + id + " has no demand - skipping.");
					continue;
				}
				final double amountInduced = deltaAmountsRcv - sumSent ;
				System.out.println( " amount induced: " + amountInduced ) ;
				Utils.writeSubHeaderInduziert(html, id, segm, improvedMode, amountInduced);
				double partialUtl = 0. ;
				
	            //induzNM: 0 - personen 1- personenKM 2 - pkw-KM 3 - personen-h 4 - pkw-h 5 - nutzerkosten  
				induziertNM[0]+=amountInduced;
				final double distance = attributesPlanfallReceiving.getByEntry(Attribute.Distanz_km);
				final double pkm = amountInduced*distance; 
				induziertNM[1]+= pkm;
				final double besetzungsgrad = returnBesetzungsgrad(segm, distance);
				final double pkwKm = pkm / besetzungsgrad;
				induziertNM[2]+= pkwKm;
				final double pH = amountInduced*attributesPlanfallReceiving.getByEntry(Attribute.Reisezeit_h);
				induziertNM[3] += pH;
				final double pkwH = pH / besetzungsgrad;
				induziertNM[4] += pkwH;
				
				induziertNM[5] += attributesPlanfallReceiving.getByEntry(Attribute.Nutzerkosten_Eu)*amountInduced;
				
				
				for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
					if ( attribute != Attribute.XX && attribute != Attribute.Nutzerkosten_Eu ) {
						// aufnehmende Seite:
						final double attributeValuePlanfallReceiving = attributesPlanfallReceiving.getByEntry(attribute);
						final double attributeValueNullfallReceiving = attributesNullfallReceiving.getByEntry(attribute);

						UtlChangesData utlChangesPerItem = utlChangePerEntry(attribute, amountInduced, 
								attributeValueNullfallReceiving, attributeValuePlanfallReceiving, econValuesReceiving.getByEntry(attribute));
						final double utlChange = utlChangesPerItem.utl * Math.abs(amountInduced);
						partialUtl += utlChange ;

						if ( utlChange!=0. ) {
							Utils.addUtlToMap(induziertRV, improvedMode, attribute , utlChange);
							
							Utils.writeAufnehmendRow(html, -amountInduced, attribute, attributeValuePlanfallReceiving, utlChangesPerItem, utlChange);
						}
					}
				}

				final double implUtlInducedPerItem = this.computeImplicitUtilityPerItem( econValuesReceiving, 
						attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
				final double implUtlInduced = implUtlInducedPerItem * amountInduced ;

				
				if ( implUtlInduced != 0. ) {
					Utils.writeImplicitUtl(html, implUtlInducedPerItem, implUtlInduced, "Impl. Nutz. induz.");
					Utils.addUtlToMap(induziertImp, improvedMode, implUtlInduced);
				}
				partialUtl += implUtlInduced ;
//				if ( partialUtl != 0. ) {
					Utils.writePartialSum(html, null, partialUtl );
//				}
				utils += partialUtl ;


			} // demand segment
		} // relation

		Utils.writeSum(html, utils);

		// ================================
		// ROH et al:
		Map<Mode,Double> operatorProfits = new HashMap<MultiDimensionalArray.Mode, Double>();
		double operatorProfit = 0. ;
		for ( String id : nullfall.getAllRelations() ) { // for all OD relations
			Values nullfallForODRelation = nullfall.getByODRelation(id) ;
			Values planfallForODRelation = planfall.getByODRelation(id) ;
			for ( Mode mode : Mode.values() ) {
			for ( DemandSegment segm : DemandSegment.values() ) {
					Attributes attributesNullfall = nullfallForODRelation.getAttributes(mode, segm) ;
					Attributes attributesPlanfall = planfallForODRelation.getAttributes(mode, segm) ;
					double amountNullfall = attributesNullfall.getByEntry(Attribute.XX) ;
					double amountPlanfall = attributesPlanfall.getByEntry(Attribute.XX) ;
					double operatorProfitBefore = operatorProfit ;
					operatorProfit = computeOperatorProfit(operatorProfit, attributesNullfall, attributesPlanfall, amountNullfall, amountPlanfall);
					
					if ( operatorProfit != operatorProfitBefore ){
						Utils.addUtlToMap(operatorProfits, mode, operatorProfit-operatorProfitBefore);
					}
				}
			}
		}
		
		

		Utils.writeOperatorProfit(operatorProfits, html);
		Utils.writeRoh(html, utilsUserFromRoHOldUsers, utilsUserFromRoHNewUsers, operatorProfits);
		Utils.endOutput(html);
		
		double sum = Utils.writeOverallOutputTable(totalHtml, verbleibendRV, verlagertRVAuf, verlagertRVAb, verlagertImpAuf, verlagertImpAb, induziertRV, induziertImp, verlagertNMAb, verlagertNMAuf, induziertNM);
		double diff = sum - (utilsUserFromRoHOldUsers + utilsUserFromRoHNewUsers + operatorProfit);
		Utils.writeOperatorProfit(operatorProfits, totalHtml);
		Utils.writeRoh(totalHtml, utilsUserFromRoHOldUsers, utilsUserFromRoHNewUsers, operatorProfits);
		Utils.endOutput(totalHtml);
		return diff;
	}

	private static double computeUserBenefitsOldUsers(Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall,
			final double amountNullfall) {
		double utils = 0. ;
		for ( Attribute attribute : Attribute.values() ) {
			if ( attribute!=Attribute.XX && attribute!=Attribute.Produktionskosten_Eu ) {
				final double improvementOfAttribute = attributesPlanfall.getByEntry(attribute) - attributesNullfall.getByEntry(attribute);
				utils += improvementOfAttribute * amountNullfall * econValues.getByEntry(attribute);
			}
		}
		return utils;
	}
	private static double computeUserBenefitsNewUsers(Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall,
			final double amountNullfall, final double amountPlanfall) {
		// compute user gains according to RoH: improvement * (xnew - xold) / 2
		double utilsUserFromRoH = 0. ;
		if ( amountPlanfall >= amountNullfall ) {
			final double averageOfXX = (amountPlanfall - amountNullfall)/2. ;
			for ( Attribute attribute : Attribute.values() ) {
				if ( attribute!=Attribute.XX && attribute!=Attribute.Produktionskosten_Eu ) {
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
			final double revenueNullfall = attributesNullfall.getByEntry(Attribute.Nutzerkosten_Eu) * amountNullfall ;
			final double revenuePlanfall = attributesPlanfall.getByEntry(Attribute.Nutzerkosten_Eu) * amountPlanfall ;
			final double operatorCostNullfall = attributesNullfall.getByEntry(Attribute.Produktionskosten_Eu) * amountNullfall ;
			final double operatorCostPlanfall = attributesPlanfall.getByEntry(Attribute.Produktionskosten_Eu) * amountPlanfall ;
			operatorProfit +=  -(revenueNullfall - operatorCostNullfall) + (revenuePlanfall - operatorCostPlanfall) ;
		}
		return operatorProfit;
	}

	private double computeAndPrintGivingOrReceiving(Attributes econValuesReceiving, Attributes attributesNullfallReceiving,
			Attributes attributesPlanfallReceiving, Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall, Mode mode, Html html, DemandSegment ds) {

		double utils = 0. ;

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX) ;
		// negative, since this is never called for the receiving mode
		
		if (deltaAmounts != 0. ){
            //verlagertNMAb/Auf: 0 - personen 1- personenKM 2 - pkw-KM 3 - personen-h 4 - pkw-h 5 - nutzerkosten  
		    {
		    this.verlagertNMAb.get(mode)[0] += deltaAmounts;
		    
		    
		    final double distance = attributesPlanfall.getByEntry(Attribute.Distanz_km);
		    final double besetzungsgrad = this.returnBesetzungsgrad(ds, distance);

		    final double personenKMChange = distance * deltaAmounts;
            final double pkwKMChange = personenKMChange / besetzungsgrad;
            this.verlagertNMAb.get(mode)[1] += personenKMChange;
            this.verlagertNMAb.get(mode)[2] += pkwKMChange;
            
            final double fahrzeit = attributesPlanfall.getByEntry(Attribute.Reisezeit_h);
            final double personenHChange = fahrzeit * deltaAmounts;
            final double pkwHChange= personenHChange / besetzungsgrad;
            
            this.verlagertNMAb.get(mode)[3] += personenHChange;
            this.verlagertNMAb.get(mode)[4] += pkwHChange;
            
            final double nutzerkosten = attributesPlanfall.getByEntry(Attribute.Nutzerkosten_Eu);

            this.verlagertNMAb.get(mode)[5] += nutzerkosten*deltaAmounts; 


		    }
		    //aufnehmend
		    {
		    
		    this.verlagertNMAuf.get(mode)[0] -= deltaAmounts;
	        final double distance = attributesPlanfallReceiving.getByEntry(Attribute.Distanz_km);
	        final double besetzungsgrad = this.returnBesetzungsgrad(ds, distance);

	            final double personenKMChange = distance * -deltaAmounts;
	            final double pkwKMChange = personenKMChange / besetzungsgrad;
	            this.verlagertNMAuf.get(mode)[1] += personenKMChange;
	            this.verlagertNMAuf.get(mode)[2] += pkwKMChange;
	            
	            final double fahrzeit = attributesPlanfallReceiving.getByEntry(Attribute.Reisezeit_h);
	            final double personenHChange = fahrzeit * -deltaAmounts;
	            final double pkwHChange= personenHChange / besetzungsgrad;
	            
	            this.verlagertNMAuf.get(mode)[3] += personenHChange;
	            this.verlagertNMAuf.get(mode)[4] += pkwHChange;
	            
	            final double nutzerkosten = attributesPlanfallReceiving.getByEntry(Attribute.Nutzerkosten_Eu);

	            this.verlagertNMAuf.get(mode)[5] += nutzerkosten*deltaAmounts;
		    }
		}
		
		
		
		


		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			double partialUtils = 0. ;
			if ( attribute != Attribute.XX && attribute != Attribute.Nutzerkosten_Eu ) {
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
						Utils.addUtlToMap(verlagertRVAb, mode, attribute, utlChange);
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
						Utils.addUtlToMap(verlagertRVAuf, mode, attribute, utlChange);

					}


				}
			}
				if ( partialUtils != 0. ) {
					Utils.writePartialSum(html, null, partialUtils) ;
					utils += partialUtils ;
				}
		}
		//		
		//			Utils.writePartialSum(html, utils);
		//		}
		return utils;
	}

	private double computeAndPrintImplicitUtl(Attributes econValuesReceiving, Attributes attributesNullfallReceiving, Attributes attributesPlanfallReceiving, 
			Attributes econValues, Attributes attributesNullfall, Attributes attributesPlanfall, Mode mode, Html html) {

		final double deltaAmounts = attributesPlanfall.getByEntry(Attribute.XX) - attributesNullfall.getByEntry(Attribute.XX ) ;
		// negative, since this is never called for the receiving mode

		final double implicitUtlPerItem = this.computeImplicitUtilityPerItem(econValues, attributesNullfall, attributesPlanfall) ;
		// probably positive

		final double implicitUtlOverall = - implicitUtlPerItem * Math.abs(deltaAmounts) ;
		if ( implicitUtlOverall != 0. ) {
			Utils.writeImplicitUtl(html, implicitUtlPerItem, implicitUtlOverall, "Impl. Nutz. abg.");
			Utils.addUtlToMap(verlagertImpAb, mode,  implicitUtlOverall);
		}

		final double implicitUtlPerItemReceiving = this.computeImplicitUtilityPerItem( econValuesReceiving, attributesNullfallReceiving, attributesPlanfallReceiving ) ; 
		// probably positive

		final double implicitUtlOverallReceiving = implicitUtlPerItemReceiving * Math.abs(deltaAmounts) ;
		if ( implicitUtlOverallReceiving != 0. ) {
			Utils.writeImplicitUtl( html, implicitUtlPerItemReceiving, implicitUtlOverallReceiving, "Impl. Nutz. aufn." ) ;
			Utils.addUtlToMap(verlagertImpAuf, mode , implicitUtlOverallReceiving);
		}

		double util = implicitUtlOverall + implicitUtlOverallReceiving ;

		if ( util != 0. ) {
			Utils.writePartialSum(html, null, util);
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
			Attributes quantitiesNullfall, Mode mode,
			Attributes quantitiesPlanfall, double amountAltnutzer, Html html) {

		double utils = 0. ;

		for ( Attribute attribute : Attribute.values() ) { // for all entries (e.g. km or hrs)
			if ( attribute != Attribute.XX && attribute != Attribute.Nutzerkosten_Eu ) {
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
				Utils.addUtlToMap(verbleibendRV, mode, attribute, utlChange);
				utils += utlChange ;

			}
		}
	
			Utils.writePartialSum(html, null, utils);
	
		return utils;
	}
	

	abstract UtlChangesData utlChangePerEntry(Attribute attribute, double deltaAmount, 
			double quantityNullfall, double quantityPlanfall, double econVal);
	abstract double computeImplicitUtilityPerItem(Attributes econValues, Attributes quantitiesNullfall, 
			Attributes quantitiesPlanfall) ;

private void fillBesetzungsgrad()
{
    this.besetzungsgradeKurz = new HashMap<DemandSegment, Double>();
    this.besetzungsgradeLang = new HashMap<DemandSegment, Double>();
    
    this.besetzungsgradeKurz.put(DemandSegment.PV_ARBEIT, 1.1);
    this.besetzungsgradeLang.put(DemandSegment.PV_ARBEIT, 1.1);

    this.besetzungsgradeKurz.put(DemandSegment.PV_AUSBILDUNG, 1.7);
    this.besetzungsgradeLang.put(DemandSegment.PV_AUSBILDUNG, 1.3);
    
    this.besetzungsgradeKurz.put(DemandSegment.PV_GESCHAEFT, 1.0);
    this.besetzungsgradeLang.put(DemandSegment.PV_GESCHAEFT, 1.1);
    
    this.besetzungsgradeKurz.put(DemandSegment.PV_EINKAUF, 1.3);
    this.besetzungsgradeLang.put(DemandSegment.PV_EINKAUF, 1.8);
    
    this.besetzungsgradeKurz.put(DemandSegment.PV_SONST, 1.6);
    this.besetzungsgradeLang.put(DemandSegment.PV_SONST, 2.0);
    
    this.besetzungsgradeKurz.put(DemandSegment.PV_URLAUB, 1.6);
    this.besetzungsgradeLang.put(DemandSegment.PV_URLAUB, 2.3);
    
}

private double returnBesetzungsgrad(DemandSegment ds, double distance){
    
    if (distance<50) {
        return this.besetzungsgradeKurz.get(ds);
        }
    else {
        return this.besetzungsgradeLang.get(ds);
        }
    }

    
}