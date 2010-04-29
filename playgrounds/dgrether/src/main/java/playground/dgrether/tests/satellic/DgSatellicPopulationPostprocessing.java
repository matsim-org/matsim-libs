/* *********************************************************************** *
 * project: org.matsim.*
 * DgSatelllicPopulationPostprocessing
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.tests.satellic;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.dgrether.DgPaths;


public class DgSatellicPopulationPostprocessing {

  public static final String BASE = DgPaths.SHAREDSVN + "studies/countries/de/prognose_2025/";

  public static final String NETWORK = BASE + "demand/network_cleaned.xml.gz";
  
  public static final String POPIN = BASE + "demand/population_gv_1pct_routet.xml.gz";

  public static final String POPOUT = BASE + "demand/population_gv_1pct.xml.gz";
  
  
  public void doPostprocessing(){
    //load base scenario data
    Scenario sc = new ScenarioImpl();
    sc.getConfig().network().setInputFile(NETWORK);
    sc.getConfig().plans().setInputFile(POPIN);
    ScenarioLoader loader = new ScenarioLoaderImpl(sc);
    loader.loadScenario();
    NetworkImpl network = (NetworkImpl) sc.getNetwork();
    PopulationImpl pop = (PopulationImpl) sc.getPopulation();
    Random random = MatsimRandom.getLocalInstance();
    //do the routing
//    TravelTime travelTimes = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(network, sc.getConfig().travelTimeCalculator());
//    TravelCost travelCosts = new TravelCostCalculatorFactoryImpl().createTravelCostCalculator(travelTimes, sc.getConfig().charyparNagelScoring());
//    PersonPrepareForSim pp4s = new PersonPrepareForSim(
//        new PlansCalcRoute(sc.getConfig().plansCalcRoute(), network, travelCosts, travelTimes, new DijkstraFactory()), network);
    for (Person p : pop.getPersons().values()){
//      pp4s.run(p);
      //do the time allocation
      Activity a = (Activity) p.getPlans().get(0).getPlanElements().get(0);
      Leg l = (Leg) p.getPlans().get(0).getPlanElements().get(1);
      double tripLength = l.getTravelTime();
      a.setEndTime(this.calculateActivityEndTime(tripLength, random));
    }
    
    //write the result
    PopulationWriter writer = new PopulationWriter(pop, network);
    writer.write(POPOUT);
    
  }
  
  private double calculateActivityEndTime(double tripLength, Random random) {
    double startTime = 0.0;
    double interval = 0.0;
    if (tripLength < (2 * 3600.0)) {
      startTime = 6.0 * 3600.0;
      interval = 12.0 * 3600.0;
    }
    else if (tripLength < (4 * 3600.0)) {
      startTime = 6.0 * 3600.0;
      interval = 10.0 * 3600.0;
    }
    else if (tripLength < (6 * 3600.0)) {
      startTime = 6.0 * 3600.0;
      interval = 8.0 * 3600.0;
    }
    else if (tripLength < (8 * 3600.0)) {
      startTime = 6.0 * 3600.0;
      interval = 4.0 * 3600.0;
    }
    else if (tripLength < (24 * 3600.0)) {
      startTime = 4.0 * 3600.0;
      interval = 2.0 * 3600.0;
    }
    else if (tripLength > (24 * 3600.0)) {
      startTime = 2.0 * 3600.0;
      interval = startTime;
    }
    return startTime + (interval * random.nextDouble());
  }





  public static void main(String[] args){
    new DgSatellicPopulationPostprocessing().doPostprocessing();
  }
  
}
