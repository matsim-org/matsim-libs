package playground.anhorni.locationchoice.run;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.anhorni.locationchoice.analysis.PrintShopAndLeisureLocations;
import playground.anhorni.locationchoice.analysis.TravelDistanceDistribution;
import playground.anhorni.locationchoice.analysis.events.CalcLegDistancesListenerDetailed;
import playground.anhorni.locationchoice.analysis.events.CalcLegTimesListenerDetailed;
import playground.anhorni.locationchoice.analysis.plans.CalculatePlanTravelStats;
import playground.anhorni.locationchoice.run.scoring.ScoreElements;
import playground.anhorni.locationchoice.run.scoring.TRBScoringFunctionFactory;

public class TRBControler extends Controler {

	public TRBControler(String[] args) {
		super(Gbl.createConfig(args));
	}

	public TRBControler(Config config) {
		super(config);
	}
	
	public static void main(String[] args) {
		new TRBControler(args).run();
	}

	public void run() {
		TRBScoringFunctionFactory trbScoringFunctionFactory = 
			new TRBScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring(), this);
		this.setScoringFunctionFactory(trbScoringFunctionFactory);

		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes.txt", false));
		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes_wayThere.txt", true));
		this.addControlerListener(new CalcLegDistancesListenerDetailed("CalcLegDistances_wayThere.txt"));
		this.addControlerListener(new CalculatePlanTravelStats(true));
		this.addControlerListener(new PrintShopAndLeisureLocations());
		this.addControlerListener(new TravelDistanceDistribution());
		super.run();
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final TravelTime travelTimes) {
		return super.createRoutingAlgorithm(travelCosts, travelTimes);
	}
}
