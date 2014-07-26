package playground.southafrica.gauteng;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingScheme;
import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactorOLD;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;
import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.routing.PersonSpecificTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.GautengScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.southafrica.utilities.Header;

import java.util.HashMap;
import java.util.Map;

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
//		if(args.length != 6){
//			throw new RuntimeException("Must provide four arguments: config file path; " +
//					"input plans file; road pricing file to use; base value of time (for cars); " +
//					"multiplier for commercial vehicles; and number of threads to use (globally).") ;
//		}
		// Get arguments
		// Read the base Value-of-Time (VoT) for private cars, and the VoT multiplier from the arguments, johan Mar'12
		String configFileName = args[0] ;
//		String configFileName = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml" ;
		
		String plansFilename = null ;
		if ( args.length>1 && args[1]!=null && args[1].length()>0 ) {
			plansFilename = args[1] ;
		}
		String tollFilename = null ;
		if ( args.length>2 && args[2]!=null && args[2].length()>0 ) {
			tollFilename = args[2];
		}
		double baseValueOfTime = 110. ;
		double valueOfTimeMultiplier = 4. ;
		if ( args.length>3 && args[3]!=null && args[3].length()>0 ) {
			baseValueOfTime = Double.parseDouble(args[3]);
		}
		if ( args.length>4 && args[4]!=null && args[4].length()>0 ) {
			valueOfTimeMultiplier = Double.parseDouble(args[4]);
		}
		int numberOfThreads = 1 ;
		if ( args.length>5 && args[5]!=null && args[5].length()>0 ) {
			numberOfThreads = Integer.parseInt(args[5]);
		}

		final Controler controler = new Controler( configFileName ) ;

		controler.setOverwriteFiles(true) ;
		
		/* Allow for the plans file to be passed as argument. */
		if ( plansFilename!=null && plansFilename.length()>0 ) {
			controler.getConfig().plans().setInputFile(plansFilename) ;
		}
		
		/* Allow for the road pricing filename to be passed as an argument. */
		if ( tollFilename!=null && tollFilename.length()>0 ) {
            ConfigUtils.addOrGetModule(controler.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(tollFilename);
		}
		
		/* Set number of threads. */
		controler.getConfig().global().setNumberOfThreads(numberOfThreads);
		
		
		final Scenario sc = controler.getScenario();
		
//		constructPersonHhMappingAndInsertIntoScenario(sc);
		
		
		if (sc.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		final TollFactorI tollFactor = new SanralTollFactorOLD() ;

		controler.addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Map<SanralTollVehicleType,Double> cnt = new HashMap<SanralTollVehicleType,Double>() ;
				for ( Person person : sc.getPopulation().getPersons().values() ) {
					SanralTollVehicleType type = tollFactor.typeOf( person.getId() ) ;
					if ( cnt.get(type)==null ) {
						cnt.put(type, 0.) ;
					}
					cnt.put( type, 1. + cnt.get(type) ) ;
				}
				for ( SanralTollVehicleType type : SanralTollVehicleType.values() ) {
					log.info( String.format( "type: %30s; cnt: %8.0f", type.toString() , cnt.get(type) ) );
				}
			}}) ;
		
		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
        RoadPricingScheme vehDepScheme =
			new GautengRoadPricingScheme( ConfigUtils.addOrGetModule(sc.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).getTollLinksFile() , sc.getNetwork() , sc.getPopulation(), tollFactor );

		// CONSTRUCT UTILITY OF MONEY:
		
		UtilityOfMoneyI personSpecificUtilityOfMoney = new GautengUtilityOfMoney( sc , sc.getConfig().planCalcScore(), baseValueOfTime, valueOfTimeMultiplier, tollFactor) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( sc.getNetwork(), sc.getPopulation(), vehDepScheme, tollFactor) 
		) ;
		
		
		controler.setScoringFunctionFactory(
				new GautengScoringFunctionFactory(sc, personSpecificUtilityOfMoney )
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
