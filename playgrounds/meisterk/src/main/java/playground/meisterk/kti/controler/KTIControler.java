/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.meisterk.kti.controler;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationFactoryImpl;
import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.KtiPopulationPreparation;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.kti.router.KtiPtRouteFactory;
import playground.meisterk.kti.router.KtiTravelCostCalculatorFactory;
import playground.meisterk.kti.router.PlansCalcRouteKtiInfo;
import playground.meisterk.kti.scenario.KtiScenarioLoaderImpl;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

/**
 * A special controler for the KTI-Project.
 *
 * @author meisterk
 * @author mrieser
 * @author wrashid
 *
 */
public class KTIControler extends Controler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
	private final PlansCalcRouteKtiInfo plansCalcRouteKtiInfo = new PlansCalcRouteKtiInfo(ktiConfigGroup);

	public KTIControler(String[] args) {
		super(args);

		super.getConfig().addModule(this.ktiConfigGroup);

        ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(TransportMode.car, new KtiLinkNetworkRouteFactory(getScenario().getNetwork(), new PlanomatConfigGroup()));
        ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(TransportMode.pt, new KtiPtRouteFactory(this.plansCalcRouteKtiInfo));

		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}

	@Override
	protected void loadData() {
		if (!this.isScenarioLoaded()) {
			KtiScenarioLoaderImpl loader = new KtiScenarioLoaderImpl(this.getScenario(), this.plansCalcRouteKtiInfo, this.ktiConfigGroup);
			loader.loadScenario();
			this.setScenarioLoaded(true);
		}
	}

	@Override
	protected void setUp() {

        KTIYear3ScoringFunctionFactory kTIYear3ScoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				getScenario(),
				this.ktiConfigGroup,
				((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties(),
                getScenario().getActivityFacilities());
		this.setScoringFunctionFactory(kTIYear3ScoringFunctionFactory);

		KtiTravelCostCalculatorFactory costCalculatorFactory = new KtiTravelCostCalculatorFactory(ktiConfigGroup);
		this.setTravelDisutilityFactory(costCalculatorFactory);

		super.setUp();
	}


	@Override
	protected void loadControlerListeners() {

		super.loadControlerListeners();

		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm() {
//		return this.ktiConfigGroup.isUsePlansCalcRouteKti() ?
//				createKtiRoutingAlgorithm(
//						this.createTravelCostCalculator(),
//						this.getLinkTravelTimes()) :
//				super.createRoutingAlgorithm();
//	}

//	public PlanAlgorithm createKtiRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
//		return new PlansCalcRouteKti(
//					super.getConfig().plansCalcRoute(),
//					super.network,
//					travelCosts,
//					travelTimes,
//					super.getLeastCostPathCalculatorFactory(),
//					((PopulationFactoryImpl) this.population.getFactory()).getModeRouteFactory(),
//					this.plansCalcRouteKtiInfo);
//	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: KtiControler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new KTIControler(args);
			controler.run();
		}
		System.exit(0);
	}


}
