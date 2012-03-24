package playground.kai.gauteng;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcAverageTolledTripLength;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeI;

import playground.kai.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.kai.gauteng.routing.GautengTravelDisutilityInclTollFactory;
import playground.kai.gauteng.scoring.GautengScoringFunctionFactory;
import playground.kai.run.KaiAnalysisListener;

class GautengControler {
	static Logger log = Logger.getLogger(GautengControler.class) ;
	
	public static void main ( String[] args ) {

		String configFileName = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml" ;

		final Controler controler = new Controler( configFileName ) ;

		controler.setOverwriteFiles(true) ;
		
		if (controler.getConfig().scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		Scenario sc = controler.getScenario();

		// CONSTRUCT ROAD PRICING SCHEME:
		RoadPricingSchemeI vehDepScheme = constructRoadPricingScheme(controler);		

		// INSERT INTO SCORING:
		insertRoadPricingIntoScoring(controler, vehDepScheme);

		// INSERT INTO ROUTING:
		controler.setTravelDisutilityFactory( new GautengTravelDisutilityInclTollFactory( vehDepScheme ) );
		
		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener()) ;
		
		// RUN:
		controler.run();
	
	}

	private static void insertRoadPricingIntoScoring(final Controler controler, RoadPricingSchemeI vehDepScheme) {
		final CalcPaidToll calcPaidToll = new CalcPaidToll(controler.getNetwork(), vehDepScheme, controler.getPopulation() ) ;
		final CalcAverageTolledTripLength cattl = new CalcAverageTolledTripLength(controler.getNetwork(), vehDepScheme );

		// accumulate toll for agent:
		controler.addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				Controler localControler = event.getControler() ;

				// add the events handler to calculate the tolls paid by agents
				localControler.getEvents().addHandler(calcPaidToll);
				// analysis:
				localControler.getEvents().addHandler(cattl);

			}
		} ) ;
		// send money event at end of iteration:
		controler.addControlerListener( new AfterMobsimListener() {
			@Override
			public void notifyAfterMobsim(final AfterMobsimEvent event) {
				// evaluate the final tolls paid by the agents and add them to their scores
				calcPaidToll.sendUtilityEvents(Time.MIDNIGHT, event.getControler().getEvents());
				// yyyyyy I would, in fact, prefer if agents did this at their arrival!!!!
			}
		} ) ;
		// print some statistics (why not right after the mobsim?):
		controler.addControlerListener( new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(final IterationEndsEvent event) {
				log.info("The sum of all paid tolls          : " + calcPaidToll.getAllAgentsToll() + " monetary units.");
				log.info("The number of people who paid toll : " + calcPaidToll.getDraweesNr());
				log.info("The average paid trip length       : " + cattl.getAverageTripLength() + " m.");
			}
		} ) ;
		
		// catch money event with special scoring function:
		controler.setScoringFunctionFactory(new GautengScoringFunctionFactory(controler.getConfig(), controler.getNetwork()));
	}

	private static RoadPricingSchemeI constructRoadPricingScheme(final Controler controler) {

		// read the road pricing scheme from file
		RoadPricingScheme scheme = new RoadPricingScheme();
		RoadPricingReaderXMLv1 rpReader = new RoadPricingReaderXMLv1(scheme);
		try {
			rpReader.parse(controler.getConfig().roadpricing().getTollLinksFile());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// wrapper that provides vehicle dependent scheme:
		RoadPricingSchemeI vehDepScheme = new GautengRoadPricingScheme( scheme, controler.getScenario().getNetwork() 
				, controler.getScenario().getPopulation() );

		return vehDepScheme;
	}



}
