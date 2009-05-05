package playground.meisterk.org.matsim.run.ktiYear3;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.world.Layer;
import org.matsim.world.World;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.meisterk.org.matsim.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.org.matsim.controler.listeners.SaveRevisionInfo;
import playground.meisterk.org.matsim.controler.listeners.ScoreElements;
import playground.meisterk.org.matsim.run.ptRouting.PTRoutingInfo;
import playground.meisterk.org.matsim.scoring.ktiYear3.KTIYear3ScoringFunctionFactory;

public class KTIControler extends Controler {

	private PreProcessLandmarks commonRoutingData = null;
	private PTRoutingInfo ptRoutingInfo=null;
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

		this.ptRoutingInfo = new PTRoutingInfo();
		// the scoring function processes facility loads independent of whether
		// a location choice module is used or not
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
		this.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt"));
		this.addControlerListener(new SaveRevisionInfo("svninfo.txt"));

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

		PlanAlgorithm router = null;

		boolean usePlansCalcRouteKti = Boolean.parseBoolean(Gbl.getConfig().getModule("kti").getValue("usePlansCalcRouteKti"));
		if (!usePlansCalcRouteKti) {
			router = super.getRoutingAlgorithm(travelCosts, travelTimes);
		} else {

			synchronized (this) {
				if (this.commonRoutingData == null) {
					this.commonRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
					this.commonRoutingData.run(this.network);
				}
			}

			// at this position, we need to read the information about pt routing (only the first time)
			// the problem is, that this method is invoked before the startup listeners, and therefore we need
			// information about pt routing here.
			if (firstTime){
				ptRoutingInfo.prepareKTIRouter(this);
				firstTime=false;
			}

			World localWorld = ptRoutingInfo.getLocalWorld();
			Layer municipalityLayer = localWorld.getLayer("municipality");
			
			router = new PlansCalcRouteKti(
					this.network, 
					this.commonRoutingData, 
					travelCosts, 
					travelTimes, 
					ptRoutingInfo.getPtTravelTimes(), 
					ptRoutingInfo.getHaltestellen(),
					municipalityLayer);

		}

		return router;
	}

}
