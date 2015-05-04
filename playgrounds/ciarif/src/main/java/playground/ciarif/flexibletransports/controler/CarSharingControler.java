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

package playground.ciarif.flexibletransports.controler;

import org.apache.log4j.Logger;
import org.matsim.contrib.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalties;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationFactoryImpl;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.ciarif.flexibletransports.controler.listeners.CarSharingListener;
import playground.ciarif.flexibletransports.controler.listeners.FtPopulationPreparation;
import playground.ciarif.flexibletransports.data.MyTransportMode;
import playground.ciarif.flexibletransports.router.FtCarSharingRouteFactory;
import playground.ciarif.flexibletransports.router.FtTravelCostCalculatorFactory;
import playground.ciarif.flexibletransports.router.PlansCalcRouteFtInfo;
import playground.ciarif.flexibletransports.scenario.FtScenarioLoaderImpl;
import playground.ciarif.flexibletransports.scoring.FtScoringFunctionFactory;
import playground.meisterk.kti.controler.listeners.CalcLegTimesKTIListener;
import playground.meisterk.kti.controler.listeners.LegDistanceDistributionWriter;
import playground.meisterk.kti.controler.listeners.ScoreElements;
import playground.meisterk.kti.router.KtiLinkNetworkRouteFactory;
import playground.meisterk.org.matsim.config.PlanomatConfigGroup;

public final class CarSharingControler extends Controler
{
  protected static final String SVN_INFO_FILE_NAME = "svninfo.txt";
  protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
  protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
  protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
  protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";
  //private KtiConfigGroup ktiConfigGroup = new KtiConfigGroup();
  private FtConfigGroup ftConfigGroup = new FtConfigGroup();
  private final PlansCalcRouteFtInfo plansCalcRouteFtInfo = new PlansCalcRouteFtInfo(this.ftConfigGroup);

  private static final Logger log = Logger.getLogger(FlexibleTransportControler.class);

  public CarSharingControler(String[] args) {
   super(args);

  // super.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
   super.getConfig().addModule(this.ftConfigGroup);

      ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(MyTransportMode.car, new KtiLinkNetworkRouteFactory(getScenario().getNetwork(), new PlanomatConfigGroup()));
      ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(MyTransportMode.pt, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
    //this.getNetwork().getFactory().setRouteFactory(MyTransportMode.ride, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
      ((PopulationFactoryImpl) getScenario().getPopulation().getFactory()).setRouteFactory(MyTransportMode.carsharing, new FtCarSharingRouteFactory(this.plansCalcRouteFtInfo));
      this.loadMyControlerListeners();
	throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
  }

  @Override
  protected void loadData() {
	if (!this.isScenarioLoaded()) {
			FtScenarioLoaderImpl loader = new FtScenarioLoaderImpl(this.getScenario(), this.plansCalcRouteFtInfo, this.ftConfigGroup);
			loader.loadScenario();
			this.setScenarioLoaded(true);
	}
  }
  @Override
  protected void setUp(){

	  {
		  if (this.ftConfigGroup.isUsePlansCalcRouteFt()) {
			  log.info("Using ftRouter");
			  this.plansCalcRouteFtInfo.prepare(getScenario().getNetwork());
		  }

		  FtScoringFunctionFactory ftScoringFunctionFactory = new FtScoringFunctionFactory(
				  this.getConfig(), 
				  this.ftConfigGroup, 
				  ((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties(),
				  getScenario().getActivityFacilities(), getScenario().getNetwork());
		  this.setScoringFunctionFactory(ftScoringFunctionFactory);
		  //
		  FtTravelCostCalculatorFactory costCalculatorFactory = new FtTravelCostCalculatorFactory(this.ftConfigGroup);
		  setTravelDisutilityFactory(costCalculatorFactory);
		  super.setUp();
	  }
  }
  
  private void loadMyControlerListeners()
  {
//    super.loadControlerListeners();

    this.addControlerListener(new FacilitiesLoadCalculator(((FacilityPenalties) this.getScenario().getScenarioElement(FacilityPenalties.ELEMENT_NAME)).getFacilityPenalties()));
    this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
    this.addControlerListener(new CalcLegTimesKTIListener("calcLegTimesKTI.txt", "legTravelTimeDistribution.txt"));
    this.addControlerListener(new LegDistanceDistributionWriter("legDistanceDistribution.txt"));
    this.addControlerListener(new FtPopulationPreparation(this.ftConfigGroup));
    this.addControlerListener(new CarSharingListener(this.ftConfigGroup));
  }

//	@Override
//	public PlanAlgorithm createRoutingAlgorithm() {
//		return this.ftConfigGroup.isUsePlansCalcRouteFt() ?
//				createFtRoutingAlgorithm(
//						this.createTravelCostCalculator(),
//						this.getLinkTravelTimes()) :
//				super.createRoutingAlgorithm();
//	}

  //private PlanAlgorithm createFtRoutingAlgorithm(TravelDisutility travelCosts, TravelTime travelTimes)
  //{
  //  log.info("travelcosts = " + travelCosts );

  //    return new PlansCalcRouteFT(
  //      super.getConfig().plansCalcRoute(), 
  //      super.network, 
  //      travelCosts, 
  //      travelTimes, 
  //      super.getLeastCostPathCalculatorFactory(),
  //      ((PopulationFactoryImpl) super.population.getFactory()).getModeRouteFactory(),
  //      this.plansCalcRouteFtInfo);
  //}

  public static void main(String[] args)
  {
    if ((args == null) || (args.length == 0)) {
      System.out.println("No argument given!");
      System.out.println("Usage: CarSharingControler config-file [dtd-file]");
      System.out.println();
    } else {
      final Controler controler = new CarSharingControler(args);
      controler.run();
    }
    System.exit(0);
  }
}
