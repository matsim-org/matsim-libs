package playground.vsp.bvwp;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;
import static playground.vsp.bvwp.Key.*;



class EconomicValues {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			Mode mode = Mode.Strasse ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				economicValues.put( makeKey( mode, segm, Attribute.Distanz_km), -0.23 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -5.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
				economicValues.put( makeKey( mode, segm, Attribute.Distanz_km), -1.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
			}
		}
		{
			Mode mode = Mode.Bahn ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				economicValues.put( makeKey( mode, segm, Attribute.Distanz_km), -0.1 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -5.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
				economicValues.put( makeKey( mode, segm, Attribute.Distanz_km), -0.1 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValuesZielnetz() {
		Values economicValues = new Values() ;
		{
			Mode mode = Mode.Strasse ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -6.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -23.5 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -1.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
		}
		{
			Mode mode = Mode.Bahn ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -6.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -23.5 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.1 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValuesZielnetzRoad() {
		Values economicValues = new Values() ;
		{
			Mode mode = Mode.Strasse ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.1 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_ARBEIT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.5 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_EINKAUF ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -6.2 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}

			{
				DemandSegment segm = DemandSegment.PV_SONST ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_URLAUB ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			
			
			
			
			{
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -8.7 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -1.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
		}
		{
			Mode mode = Mode.Bahn ;
			{
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.1 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_ARBEIT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.5 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_EINKAUF ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -6.2 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}

			{
				DemandSegment segm = DemandSegment.PV_SONST ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.PV_URLAUB ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -4.3 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			
			
			
			
			{
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -0.0 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -8.7 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
			{
				DemandSegment segm = DemandSegment.GV ;
//				economicValues.put( makeKey( mode, segm, Attribute.km), -1.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Reisezeit_h), -0.00 ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Nutzerkosten_Eu), -1. ) ;
				economicValues.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu), -1. ) ;
			}
		}
	
		return economicValues ;
	}
//	static Values createEconomicValues2() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.0 ) ;
//				pvValues.setByEntry( Attribute.hrs, -18.00 ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType pvValues = railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.1 ) ;
//				pvValues.setByEntry( Attribute.hrs, -18.00 ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//
//	static Values createEconomicValues3() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType gvValues = roadValues.getByDemandSegment(DemandSegment.GV) ;
//				gvValues.setByEntry( Attribute.km, -0.00 ) ;
//				gvValues.setByEntry( Attribute.hrs, -0.00 ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType gvValues = railValues.getByDemandSegment(DemandSegment.GV) ;
//				gvValues.setByEntry( Attribute.km, -5. ) ;
//				gvValues.setByEntry( Attribute.hrs, -0.00 ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//	
//	static Values createEconomicValuesFolienMann() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType pvCommercialValues = roadValues.getByDemandSegment(DemandSegment.PV_COMMERCIAL) ;
//				pvCommercialValues.setByEntry( Attribute.km, -0.00 ) ;
//				pvCommercialValues.setByEntry( Attribute.hrs, -23.50 ) ;
//			}
//			{
//				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.0 ) ;
//				pvValues.setByEntry( Attribute.hrs, -6.30 ) ;
//			}
//			
//			economicValues.setValuesForMode( Mode.rail, roadValues.createDeepCopy() ) ;
//		}
//		return economicValues ;
//	}
//
//	/**
//	 * I think that a test depends on this one here.  kai, jul'12
//	 */
//	static Values createEconomicValuesForTest1() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.23 ) ;
//				pvValues.setByEntry( Attribute.hrs, -5.00 ) ;
//			}
//			{
//				ValuesForAUserType gvValues = roadValues.getByDemandSegment(DemandSegment.GV) ;
//				gvValues.setByEntry( Attribute.km, -1.00 ) ;
//				gvValues.setByEntry( Attribute.hrs, -0.00 ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType pvValues = railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.1 ) ;
//				pvValues.setByEntry( Attribute.hrs, -5.00 ) ;
//			}
//			{
//				ValuesForAUserType gvValues = railValues.getByDemandSegment(DemandSegment.GV) ;
//				gvValues.setByEntry( Attribute.km, -0.1 ) ;
//				gvValues.setByEntry( Attribute.hrs, -0.00 ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//
//	static Values createEconomicValuesAP200PV() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.50 ) ;
//				pvValues.setByEntry( Attribute.hrs, -6.00 ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType pvValues = railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.1 ) ;
//				pvValues.setByEntry( Attribute.hrs, -6.00 ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//
//	static Values createEconomicValuesFictiveExamplePV() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType pvValues = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.28 ) ;
//				pvValues.setByEntry( Attribute.hrs, -18.00 ) ;
//				pvValues.setByEntry( Attribute.priceUser, -1. ) ;
//				pvValues.setByEntry( Attribute.costOfProduction, -1. ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType pvValues = railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				pvValues.setByEntry( Attribute.km, -0.1 ) ;
//				pvValues.setByEntry( Attribute.hrs, -18.00 ) ;
//				pvValues.setByEntry( Attribute.priceUser, -1. ) ;
//				pvValues.setByEntry( Attribute.costOfProduction, -1. ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//
//	static Values createEconomicValuesFictiveExampleGV() {
//		Values economicValues = new Values() ;
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType values = roadValues.getByDemandSegment(DemandSegment.GV) ;
//				values.setByEntry( Attribute.km, -0.28 ) ;
//				values.setByEntry( Attribute.hrs, -0.00 ) ;
//				values.setByEntry( Attribute.priceUser, -1. ) ;
//				values.setByEntry( Attribute.costOfProduction, -1. ) ;
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType values = railValues.getByDemandSegment(DemandSegment.GV) ;
//				values.setByEntry( Attribute.km, -0.00 ) ;
//				values.setByEntry( Attribute.hrs, -0.00 ) ;
//				values.setByEntry( Attribute.priceUser, -1. ) ;
//				values.setByEntry( Attribute.costOfProduction, -1. ) ;
//			}
//		}
//	
//		return economicValues ;
//	}
//	
//	/**
//	 * Attempt to recreate values of BVWP'10.  Changes to this should be done elsewhere.
//	 */
//	public static Values createEconomicValuesBVWP2010() {
//		Values economicValues = new Values() ;
//
//		// PV_NON_COMMERCIAL:
//		{
//			ValuesForAMode valuesForAMode = economicValues.getByMode(Mode.road) ;
//			{
//				ValuesForAUserType valuesForAModeAndDemandSegment = valuesForAMode.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				valuesForAModeAndDemandSegment.setByEntry( Attribute.hrs, -18.00 ) ;
//			}
//		}
//		{
//			ValuesForAMode valuesForAMode = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType valuesForAModeAndDemandSegment = valuesForAMode.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
//				valuesForAModeAndDemandSegment.setByEntry( Attribute.hrs, -18.00 ) ;
//			}
//		}
//		
//		// GV:
//		{
//			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
//			{	
//				ValuesForAUserType values = roadValues.getByDemandSegment(DemandSegment.GV) ;
//				values.setByEntry( Attribute.hrs, -0.00 ) ; 
//			}
//		}
//		{
//			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
//			{
//				ValuesForAUserType values = railValues.getByDemandSegment(DemandSegment.GV) ;
//				values.setByEntry( Attribute.hrs, -0.00 ) ;
//			}
//		}
//		
//		// General settings:
//		for ( Mode mode : Mode.values() ) {
//			for ( DemandSegment segment : DemandSegment.values() ) {
//				ValuesForAUserType vv = economicValues.getByMode(mode).getByDemandSegment(segment);
//
//				// all monetary values are always -1:
//				vv.setByEntry( Attribute.priceUser, -1. ) ;
//				vv.setByEntry( Attribute.costOfProduction, -1. ) ;
//				
//				// all distance values are always 0 (included in cost of production):
//				vv.setByEntry( Attribute.km, -0.0 ) ; // VoD = 0
//				
//				// there was no VoR in BVWP'03/'10:
//				vv.setByEntry( Attribute.excess_hrs, 0. ) ;
//			}
//		}
//
//		return economicValues ;
//	}

}
