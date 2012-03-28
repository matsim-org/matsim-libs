package playground.kai.gauteng;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.roadpricing.RoadPricingSchemeI;

import playground.kai.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.kai.gauteng.routing.GautengTravelDisutilityInclTollFactory;
import playground.kai.gauteng.scoring.GautengScoringFunctionFactory;
import playground.kai.gauteng.scoring.GenerationOfMoneyEvents;
import playground.kai.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.kai.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.kai.run.KaiAnalysisListener;

/**
 * Design comments:<ul>
 * <li> The money (toll) is converted into utils both for the router and for the scoring.
 * <li> However, setting up the toll scheme in terms of disutilities does not seem right.
 * </ul>
 *
 */
class GautengControler {
	static Logger log = Logger.getLogger(GautengControler.class) ;
	
	public static void main ( String[] args ) {

		String configFileName = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml" ;

		final Controler controler = new Controler( configFileName ) ;

		controler.setOverwriteFiles(true) ;
		
		Scenario sc = controler.getScenario();

		
		
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		
		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingSchemeI vehDepScheme = 
			new GautengRoadPricingScheme( sc.getConfig(), sc.getNetwork() , sc.getPopulation() );

		// CONSTRUCT UTILITY OF MONEY:
		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney( sc.getConfig().planCalcScore() ) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( sc.getNetwork(), sc.getPopulation(), vehDepScheme) 
		) ;
		
		
		controler.setScoringFunctionFactory(
				new GautengScoringFunctionFactory(sc.getConfig(), sc.getNetwork(), personSpecificUtilityOfMoney )
		);

		// insert into routing:
		controler.setTravelDisutilityFactory( 
				new GautengTravelDisutilityInclTollFactory( vehDepScheme, personSpecificUtilityOfMoney ) 
		);
		
		
		
		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		// RUN:
		controler.run();
	
	}



}
