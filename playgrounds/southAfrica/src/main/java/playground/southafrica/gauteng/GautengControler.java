package playground.southafrica.gauteng;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.routing.PersonSpecificTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.PersonSpecificUoMScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.southafrica.utilities.Header;

class PersonHouseholdMapping {
	/** name to use to add an instance as a scenario element*/
	public static final String ELEMENT_NAME = "personHouseholdMapping"; 
	
	Map<Id,Id> delegate = new HashMap<Id,Id>() ;
	// key = personId; value = householdId
	// Id hhId = personHouseholdMapping.get( personId ) ;

	Id getHhIdFromPersonId( Id personId ) {
		return delegate.get(personId) ;
	}
	Id insertPersonidHhidPair( Id personId, Id hhId ) {
		// maybe check if that key (= personId) is already taken.  Shoudl not happen.
		return delegate.put( personId, hhId) ;
	}
}

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
		Header.printHeader(GautengControler.class.toString(), args);
		if(args.length != 6){
			throw new RuntimeException("Must provide four arguments: config file path; " +
					"input plans file; road pricing file to use; base value of time (for cars); " +
					"multiplier for commercial vehicles; and number of threads to use (globally).") ;
		}
		// Get arguments
		// Read the base Value-of-Time (VoT) for private cars, and the VoT multiplier from the arguments, johan Mar'12
		String configFileName = args[0] ;
//		String configFileName = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml" ;
		String plansFilename = args[1] ;
		String tollFilename = args[2];
		double baseValueOfTime = Double.parseDouble(args[3]);
		double valueOfTimeMultiplier = Double.parseDouble(args[4]);
		int numberOfThreads = Integer.parseInt(args[5]);

		final Controler controler = new Controler( configFileName ) ;

		controler.setOverwriteFiles(true) ;
		
		/* Allow for the plans file to be passed as argument. */
		controler.getConfig().plans().setInputFile(plansFilename) ;
		
		/* Allow for the road pricing filename to be passed as an argument. */
		controler.getConfig().roadpricing().setTollLinksFile(tollFilename);
		
		/* Set number of threads. */
		controler.getConfig().global().setNumberOfThreads(numberOfThreads);
		
		
		Scenario sc = controler.getScenario();
		
//		constructPersonHhMappingAndInsertIntoScenario(sc);
		
		
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		
		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingScheme vehDepScheme = 
			new GautengRoadPricingScheme( sc.getConfig().roadpricing().getTollLinksFile() , sc.getNetwork() , sc.getPopulation() );

		// CONSTRUCT UTILITY OF MONEY:
		
		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney( sc.getConfig().planCalcScore() , baseValueOfTime, valueOfTimeMultiplier) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( sc.getNetwork(), sc.getPopulation(), vehDepScheme) 
		) ;
		
		
		controler.setScoringFunctionFactory(
				new PersonSpecificUoMScoringFunctionFactory(sc.getConfig(), sc.getNetwork(), personSpecificUtilityOfMoney )
		);

		// insert into routing:
		controler.setTravelDisutilityFactory( 
				new PersonSpecificTravelDisutilityInclTollFactory( vehDepScheme, personSpecificUtilityOfMoney ) 
		);
		
		
		
		// ADDITIONAL ANALYSIS:
		// This is not truly necessary.  It could be removed or copied in order to remove the dependency on the kai
		// playground.  For the time being, I (kai) would prefer to leave it the way it is since I am running the Gauteng
		// scenario and I don't want to maintain two separate analysis listeners.  But once that period is over, this
		// argument does no longer apply.  kai, mar'12
		//
		// I (JWJ, June '13) commented this listener out as the dependency is not working.
		
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		// RUN:
		controler.run();
	
	}

	private static void constructPersonHhMappingAndInsertIntoScenario(
			Scenario sc) {
		Households hhs = ((ScenarioImpl) sc).getHouseholds() ;
		
		PersonHouseholdMapping phm = new PersonHouseholdMapping() ;
		
		for ( Household hh : hhs.getHouseholds().values() ) {
			for ( Id personId : hh.getMemberIds() ) {
				phm.insertPersonidHhidPair( personId, hh.getId() ) ;
			}
		}
		sc.addScenarioElement( PersonHouseholdMapping.ELEMENT_NAME, phm ) ;
		
		// retreive as follows:
		PersonHouseholdMapping retreivedPhm = (PersonHouseholdMapping) sc.getScenarioElement( PersonHouseholdMapping.ELEMENT_NAME ) ;
	}



}
