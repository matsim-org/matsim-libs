package playground.kai.gauteng;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.misc.Time;

import playground.kai.gauteng.routing.GautengTollTravelCostCalculatorFactory;
import playground.kai.gauteng.scoring.GautengScoringFunctionFactory;

class MyControlerListener implements StartupListener, AfterMobsimListener {
	
	playground.kai.analysis.MyCalcLegTimes calcLegTimes = null ;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.calcLegTimes = new playground.kai.analysis.MyCalcLegTimes( event.getControler().getScenario() ) ;
		event.getControler().getEvents().addHandler( this.calcLegTimes ) ;

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		int iteration = event.getIteration() ;

		this.calcLegTimes.writeStats(event.getControler().getControlerIO().getIterationFilename(iteration, "mytripdurations.txt"));

		Logger.getLogger(this.getClass()).info("[" + iteration + "] average trip (probably: leg) duration is: " 
				+ (int) this.calcLegTimes.getAverageTripDuration()
				+ " seconds = " + Time.writeTime(this.calcLegTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));

		// trips are from "true" activity to "true" activity.  legs may also go
		// from/to ptInteraction activity.  Thus, in my opinion "legs" is the correct (matsim) term
		// kai, jul'11

	}

}

class GautengControler {
	
	public static void main ( String[] args ) {

		Controler controler = new Controler( args ) ;

		controler.setOverwriteFiles(true) ;
		
		installScoringFunctionFactory(controler) ;
		installTravelCostCalculatorFactory(controler) ;
		
		ControlerListener myControlerListener = new MyControlerListener() ;
		controler.addControlerListener(myControlerListener) ;
		
		controler.run();
	
	}
	
	private static void installScoringFunctionFactory(Controler controler) {
		Scenario sc = controler.getScenario();
		controler.setScoringFunctionFactory(new GautengScoringFunctionFactory(sc.getConfig(), sc.getNetwork()));
	}
	
	/**
	 * Explanation:<ul>
	 * <li> This factory is installed very early in the initialization sequence.
	 * <li> controler.run() then calls init() which calls loadCoreListeners() which calls
	 * <pre> new RoadPricing() </pre>
	 * and add this as a CoreListener.
	 * <li> The RoadPricing object, when called by notifyStartup(event), pulls the initialization
	 * information from the event and instantiates itself.  While doing that, it constructs an object
	 * <pre> new CalcPaidToll( network, tollingScheme ) </pre>
	 * which is added as an events handler.  
	 * <li> CalcPaidToll will then collect events for every agent and accumulate the resulting toll.
	 * <li> Just after this, controler.setTravelCostCalculatorFactory(...) is set with a factory that
	 * adds the toll to the already existing travel cost object. <i> Which implies to me that in the situation here
	 * the toll is added twice for routing. yyyyyy </i>
	 * <li> A notifyAfterMobsim(...) makes the CalcPaidToll object send the accumulated toll to the agents.
	 * </ul>
	 */
	private static void installTravelCostCalculatorFactory(Controler controler) {
		
		final boolean isUsingRoadpricing = controler.getConfig().scenario().isUseRoadpricing();
		controler.setTravelDisutilityFactory(
				new GautengTollTravelCostCalculatorFactory(isUsingRoadpricing, controler.getRoadPricing())
				);

	}



}
