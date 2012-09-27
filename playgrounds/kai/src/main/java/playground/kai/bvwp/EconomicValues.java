package playground.kai.bvwp;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

class EconomicValues {

	static Values createEconomicValues1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.23 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
				pvValues.setByEntry( Entry.priceUser, -1. ) ;
			}
			{
				ValuesForAUserType gvValues = roadValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -1.00 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
				gvValues.setByEntry( Entry.priceUser, -1. ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
				pvValues.setByEntry( Entry.priceUser, -1. ) ;
			}
			{
				ValuesForAUserType gvValues = railValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -0.1 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
				gvValues.setByEntry( Entry.priceUser, -1. ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValues2() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.0 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValues3() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType gvValues = roadValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -0.00 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType gvValues = railValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -5. ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
	
		return economicValues ;
	}
	
	static Values createEconomicValuesFolienMann() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvCommercialValues = roadValues.getByDemandSegment(Type.PV_COMMERCIAL) ;
				pvCommercialValues.setByEntry( Entry.km, -0.00 ) ;
				pvCommercialValues.setByEntry( Entry.hrs, -23.50 ) ;
			}
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.0 ) ;
				pvValues.setByEntry( Entry.hrs, -6.30 ) ;
			}
			
			economicValues.setValuesForMode( Mode.rail, roadValues.createDeepCopy() ) ;
		}
		return economicValues ;
	}

	/**
	 * I think that a test depends on this one here.  kai, jul'12
	 */
	static Values createEconomicValuesForTest1() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.23 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
			}
			{
				ValuesForAUserType gvValues = roadValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -1.00 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -5.00 ) ;
			}
			{
				ValuesForAUserType gvValues = railValues.getByDemandSegment(Type.GV) ;
				gvValues.setByEntry( Entry.km, -0.1 ) ;
				gvValues.setByEntry( Entry.hrs, -0.00 ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValuesAP200PV() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.50 ) ;
				pvValues.setByEntry( Entry.hrs, -6.00 ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -6.00 ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValuesFictiveExamplePV() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType pvValues = roadValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.28 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
				pvValues.setByEntry( Entry.priceUser, -1. ) ;
				pvValues.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType pvValues = railValues.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.km, -0.1 ) ;
				pvValues.setByEntry( Entry.hrs, -18.00 ) ;
				pvValues.setByEntry( Entry.priceUser, -1. ) ;
				pvValues.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
	
		return economicValues ;
	}

	static Values createEconomicValuesFictiveExampleGV() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType values = roadValues.getByDemandSegment(Type.GV) ;
				values.setByEntry( Entry.km, -0.28 ) ;
				values.setByEntry( Entry.hrs, -0.00 ) ;
				values.setByEntry( Entry.priceUser, -1. ) ;
				values.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType values = railValues.getByDemandSegment(Type.GV) ;
				values.setByEntry( Entry.km, -0.00 ) ;
				values.setByEntry( Entry.hrs, -0.00 ) ;
				values.setByEntry( Entry.priceUser, -1. ) ;
				values.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
	
		return economicValues ;
	}

	public static Values createEconomicValuesBVWP2015() {
		Values economicValues = new Values() ;
		{
			ValuesForAMode valuesForAMode = economicValues.getByMode(Mode.road) ;
			{
				ValuesForAUserType valuesForAModeAndDemandSegment = valuesForAMode.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.km, -0.28 ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.hrs, -18.00 ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.priceUser, -1. ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
		{
			ValuesForAMode valuesForAMode = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType valuesForAModeAndDemandSegment = valuesForAMode.getByDemandSegment(Type.PV_NON_COMMERCIAL) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.km, -0.1 ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.hrs, -18.00 ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.priceUser, -1. ) ;
				valuesForAModeAndDemandSegment.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
		{
			ValuesForAMode roadValues = economicValues.getByMode(Mode.road) ;
			{	
				ValuesForAUserType values = roadValues.getByDemandSegment(Type.GV) ;
				values.setByEntry( Entry.km, -0.28 ) ;
				values.setByEntry( Entry.hrs, -0.00 ) ;
				values.setByEntry( Entry.priceUser, -1. ) ;
				values.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}
		{
			ValuesForAMode railValues = economicValues.getByMode(Mode.rail) ;
			{
				ValuesForAUserType values = railValues.getByDemandSegment(Type.GV) ;
				values.setByEntry( Entry.km, -0.00 ) ; // no user costs per km
				values.setByEntry( Entry.hrs, -1.00 ) ; // NEW: a value of time per ton
				values.setByEntry( Entry.priceUser, -1. ) ;
				values.setByEntry( Entry.costOfProduction, -1. ) ;
			}
		}

		return economicValues ;
	}

}
