package playground.vsp.zzArchive.bvwpOld;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;
import playground.vsp.zzArchive.bvwpOld.Values.Mode;

class ScenarioKarlsruheBaselGV {

	static final String relation = "Karlsruhe-Basel";

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(relation, nullfallForOD ) ;
		{
			{
				ValuesForAMode valuesForAMode = nullfallForOD.getByMode(Mode.road) ;
				{
					Attributes vv = valuesForAMode.getByDemandSegment(DemandSegment.GV) ;
					vv.setByEntry( Attribute.XX, 2.6e8 ) ; // number of tons.  Irrelevant dummy value.
					vv.setByEntry( Attribute.km, 200. ) ;
					vv.setByEntry( Attribute.hrs, 3. ) ;
					{		
						double distance = vv.getByEntry(Attribute.km) ;
						double ttime = vv.getByEntry(Attribute.hrs ) ;
						double costOfProduction = distance * 1. + ttime * 0.28 ;  // VoD of vehicle; VoT of vehicle + of driver 

						vv.setByEntry( Attribute.costOfProduction, costOfProduction ) ;
					}

					vv.setByEntry( Attribute.priceUser, vv.getByEntry( Attribute.costOfProduction ) ) ; // competetive assumption
				}
			}
			{
				ValuesForAMode valuesForAMode = nullfallForOD.getByMode(Mode.rail) ;
				{
					Attributes vv = valuesForAMode.getByDemandSegment(DemandSegment.GV) ;
					vv.setByEntry( Attribute.XX, 2.6e7 ) ; // number of tons
					vv.setByEntry( Attribute.km, 200. ) ;
					vv.setByEntry( Attribute.hrs, 10. ) ;
					vv.setByEntry( Attribute.excess_hrs, 6. ) ;
					{
						double distance = vv.getByEntry(Attribute.km) ;
						double ttime = vv.getByEntry(Attribute.hrs ) ;
						double costOfProduction = distance * 0.1 + ttime * 0.0 ;  // VoD of vehicle; VoT of vehicle + of driver 

						vv.setByEntry( Attribute.costOfProduction, costOfProduction ) ; 
						// Needs to be "per ton" since this is the normal computation: xx * attribute * economicValue
					}
					
					double costOfProduction = vv.getByEntry( Attribute.costOfProduction ) ;
//					double userPriceOnRoad = nullfallForOD.getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).getByEntry(Attribute.priceUser) ;
//					
//					// assume price before measure is same as road price (scarcity price):
//					double userPrice = Math.max(costOfProduction, userPriceOnRoad) ;

					vv.setByEntry( Attribute.priceUser, costOfProduction ) ; 
					
				}
			}
			
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfall1(ScenarioForEvalData nullfall) {
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		Values planfallForOD = planfall.getByODRelation(relation) ;
		{
			ValuesForAMode valuesForAMode = planfallForOD.getByMode( Mode.rail ) ;
			{
				final Attributes vv = valuesForAMode.getByDemandSegment(DemandSegment.GV);

				// modify the travel times for the rail mode:
				double deltaTtime = -6. ;
				vv.incByEntry( Attribute.hrs, deltaTtime ) ;
				vv.setByEntry(Attribute.excess_hrs, 0. ) ;

				// user price = cost of production (should be same as in nullfall, but we keep modifying it there)
				vv.setByEntry(Attribute.priceUser, vv.getByEntry(Attribute.costOfProduction) ) ;

				// modify some demand (presumably as a result):
				double delta = 100. ;
				vv.incByEntry( Attribute.XX, delta ) ;
				planfall.getByODRelation(relation).getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).incByEntry(Attribute.XX, -delta ) ;
			}
		}
		return planfall;
	}
	
	public static void main( String[] args ) {
		IllustrationsKarlsruheBaselGV.main(args) ;
	}

}

