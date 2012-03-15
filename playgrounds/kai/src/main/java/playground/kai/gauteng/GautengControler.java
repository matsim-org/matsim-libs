package playground.kai.gauteng;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.roadpricing.RoadPricingScheme;

import playground.kai.gauteng.routing.GautengTollTravelCostCalculatorFactory;
import playground.kai.gauteng.routing.GautengTravelCostCalculatorFactory;
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
		// from/to ptInteraction activity.  This, in my opinion "legs" is the correct (matsim) term
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
	
	private static void installTravelCostCalculatorFactory(Controler controler) {
		// returns null, if there is no road pricing
		// really? not `false'?  kai, mar'12
		
		// there is some asymmetry here: installScoringFunctionFactory solves the difference between toll and non-toll in
		// the factory.  The present method, however, solves it here.  kai, mar'12
		
		if (controler.getConfig().scenario().isUseRoadpricing()){
			RoadPricingScheme roadPricingScheme = controler.getRoadPricing().getRoadPricingScheme();
			controler.setTravelCostCalculatorFactory(new GautengTollTravelCostCalculatorFactory(roadPricingScheme));
		}
		else{
			controler.setTravelCostCalculatorFactory(new GautengTravelCostCalculatorFactory());
		}
	}



}
