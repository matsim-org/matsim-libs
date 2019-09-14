package playground.vsp.zzArchive.bvwpOld;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;
import playground.vsp.zzArchive.bvwpOld.Values.Mode;


public class IllustrationsKarlsruheBaselGV {

	private static Values economicValues;
	private static ScenarioForEvalData nullfall;
	private static ScenarioForEvalData planfall;

	public static void main(String[] args) {

		// create the economic values
		economicValues = EconomicValues.createEconomicValuesBVWP2010();

		// create the base case:
		nullfall = ScenarioKarlsruheBaselGV.createNullfall1();

		// create the policy case:
		planfall = ScenarioKarlsruheBaselGV.createPlanfall1(nullfall);

		System.out.println(economicValues) ;

		{
			Html html = new Html("bvwp2003") ;
			System.out.println("\n==================================================================================================================================");
			html.multiFmtComment("bvwp'03/'10 for comparison:" ) ;
			html.endParagraph() ;
			UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
			utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall, html) ;
			html.endBody() ;
			html.endHtml() ;
		}
		{
			Html html = new Html("bvwp2015base") ;
			System.out.println("\n==================================================================================================================================");
			html.multiFmtComment("bvwp2015 w/ implicit utl. but w/o anything else:" );
			UtilityChanges utilityChanges1 = new UtilityChangesBVWP2015();
			utilityChanges1.computeAndPrintResults(economicValues, nullfall, planfall, html) ;
			html.multiFmtComment( "Comment: implicit utl just compensates for bvwp03 gain.  Can't get gain in RoH when VoX is zero in " +
			"improved attribute X." );
			html.endBody() ;
			html.endHtml() ;
		}
		{
			Html html = new Html("bvwp2015wVoTgoods") ;
			System.out.println("\n==================================================================================================================================");
			html.multiFmtComment("bvwp2015 w/ implicit utl. and w/ VoT for goods:") ;
			Values economicValuesTmp = economicValues.createDeepCopy() ;
			economicValuesTmp.getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).setByEntry(Attribute.hrs, -1.00 ) ;
			economicValuesTmp.getByMode(Mode.rail).getByDemandSegment(DemandSegment.GV).setByEntry(Attribute.hrs, -1.00 ) ;
			UtilityChanges utilityChanges1 = new UtilityChangesBVWP2015();
			utilityChanges1.computeAndPrintResults(economicValuesTmp, nullfall, planfall,html) ;
			html.multiFmtComment("Comment: VoT_goods!=0 means large gains for acceleration of freight!!!" );
			html.endBody() ;
			html.endHtml() ;
		}
		{
			Html html = new Html("bvwp2015wRoadPriceAsScarcityPrice") ;
			System.out.println("\n==================================================================================================================================");
			html.multiFmtComment("bvwp2015 w/ implicit utl. and w/ road price as user price for rail in nullfall:") ;
			ScenarioForEvalData nullfallTmp = nullfall.createDeepCopy() ;
			for ( String id : nullfallTmp.getAllRelations() ) {
				final Values attribsForOd = nullfall.getByODRelation(id);
				double roadPrice = attribsForOd.getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).getByEntry(Attribute.priceUser) ;
				attribsForOd.getByMode(Mode.rail).getByDemandSegment(DemandSegment.GV).setByEntry(Attribute.priceUser, roadPrice ) ;
			}
			UtilityChanges utilityChanges1 = new UtilityChangesBVWP2015();
				utilityChanges1.computeAndPrintResults(economicValues, nullfall, planfall,html) ;
			html.multiFmtComment("Using road price as scarcity price results in 1/2 of benefits as computation according to resource consumption.") ;
			html.endBody() ;
			html.endHtml() ;
		}
		{
			Html html = new Html("bvwp15wVoRgoods") ;
			System.out.println("\n==================================================================================================================================");
			html.multiFmtComment("bvwp2015 w/ implicit utl. and w/ VoR for goods:") ;
			Values economicValuesTmp = economicValues.createDeepCopy() ;
			economicValuesTmp.getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).setByEntry(Attribute.excess_hrs, -1.00 ) ;
			economicValuesTmp.getByMode(Mode.rail).getByDemandSegment(DemandSegment.GV).setByEntry(Attribute.excess_hrs, -1.00 ) ;
			UtilityChanges utilityChanges1 = new UtilityChangesBVWP2015();
			utilityChanges1.computeAndPrintResults(economicValuesTmp, nullfall, planfall,html) ;
			html.multiFmtComment("Comment: With these values, same as w/ VoT for goods.  However, with VoR no gains for pure GV accelerations.") ;
			html.endBody() ;
			html.endHtml() ;
		}
	}

	private static void runRoH() {
		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;		
	}

	private static void runBVWP2010() {
		UtilityChanges utilityChanges = new UtilityChangesBVWP2010();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall);
	}

}
