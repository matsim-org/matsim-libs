package playground.meisterk.org.matsim.run.ktiYear3;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.matrices.Matrix;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.marcel.kti.router.SwissHaltestellen;
import playground.meisterk.org.matsim.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.org.matsim.controler.listeners.KtiRouterListener;
import playground.meisterk.org.matsim.controler.listeners.ScoreElements;
import playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory;

public class KTIControler extends Controler {

	private PreProcessLandmarks commonRoutingData = null;
	private KtiRouterListener ktiRouterListener=null;
	private static boolean firstTime=true;

	public KTIControler(String[] args) {
		super(Gbl.createConfig(args));
	}

	public KTIControler(Config config) {
		super(config);
	}

	public void run() {

		KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(Gbl.getConfig()
				.charyparNagelScoring(), this.getFacilityPenalties());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		// the scoring function processes facility loads independent of whether
		// a location choice module is used or not
		this.ktiRouterListener=new KtiRouterListener();
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
		this.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt"));
		
		// unfortunatly, the startup method of the config is launched after the 'getRoutingAlgorithm', for this reason it will not work to put the
		// ktiRouterListener here (it has been inserted in the getRoutingAlgorithm implementation
		//ktiRouterListener.prepareKTIRouter(this);
		//this.addControlerListener(ktiRouterListener);
		// ATTENTION, remove this line for the runs!!!!!!!!!!!!!!!!
		this.setOverwriteFiles(true);

		super.run();

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new KTIControler(args).run();
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {

		synchronized (this) {
			if (this.commonRoutingData == null) {
				this.commonRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				this.commonRoutingData.run(this.network);
			}
		}
		
		// at this position, we need to read the information about pt routing once
		if (firstTime){
			firstTime=false;
			this.ktiRouterListener.prepareKTIRouter(this);
		}

		return new PlansCalcRouteKti(this.network, this.commonRoutingData, travelCosts, travelTimes, this.ktiRouterListener.getPtTravelTimes(), this.ktiRouterListener.getHaltestellen(),
				getWorld().getLayer("municipality"));
	}

}
