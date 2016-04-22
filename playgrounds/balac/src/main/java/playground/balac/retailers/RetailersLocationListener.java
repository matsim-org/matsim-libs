/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.balac.retailers;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.balac.retailers.IO.FileRetailerReader;
import playground.balac.retailers.IO.LinksRetailerReaderV2;
import playground.balac.retailers.IO.RetailersSummaryWriter;
import playground.balac.retailers.data.Retailer;
import playground.balac.retailers.data.Retailers;
import playground.balac.retailers.utils.CountFacilityCustomers;
import playground.balac.retailers.utils.ReRoutePersons;
import playground.balac.retailers.utils.Utils;



public class RetailersLocationListener
  implements StartupListener, IterationEndsListener, BeforeMobsimListener
{
  private static final Logger log = Logger.getLogger(RetailersLocationListener.class);
  public static final String CONFIG_GROUP = "Retailers";
  public static final String CONFIG_RETAILERS = "retailers";
  public static final String CONFIG_STRATEGY_TYPE = "strategyType";
  public static final String CONFIG_MODEL_ITERATION = "modelIteration";
  public static final String CONFIG_ANALYSIS_FREQUENCY = "analysisFrequency";
  public static final String CONFIG_RSW_OUTPUT_FILE = "rswOutputFile";
  private final boolean parallel = false;
  private String facilityIdFile = null;
  private Retailers retailers;
  private MatsimServices controler;
  private LinksRetailerReaderV2 lrr;
  private RetailersSummaryWriter rsw;
  private CountFacilityCustomers cfc;

  @Override
	public void notifyStartup(StartupEvent event)
  {
    this.controler = event.getServices();
    FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(this.controler.getConfig().planCalcScore());
      RouteFactoryImpl routeFactory = ((PopulationFactoryImpl) this.controler.getScenario().getPopulation().getFactory()).getRouteFactory();

    this.facilityIdFile = this.controler.getConfig().findParam("Retailers", "retailers");
    if (this.facilityIdFile == null) throw new RuntimeException("In config file, param = retailers in module = Retailers not defined!");

      this.retailers = new FileRetailerReader(this.controler.getScenario().getActivityFacilities().getFacilities(), this.facilityIdFile).readRetailers(this.controler);

      this.cfc = new CountFacilityCustomers(this.controler.getScenario().getPopulation().getPersons());
    Utils.setFacilityQuadTree(Utils.createFacilityQuadTree(this.controler));
    log.info("Creating PersonQuadTree");
    Utils.setPersonQuadTree(Utils.createPersonQuadTree(this.controler));    

    String rswOutputFile = this.controler.getConfig().findParam("Retailers", "rswOutputFile");
    if (rswOutputFile == null) {
      throw new RuntimeException("The file to which the Retailers Summary should be written has not been set");
    }

    this.rsw = new RetailersSummaryWriter(rswOutputFile);
  }

  @Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event)
  {
  }

  @Override
	public void notifyIterationEnds(IterationEndsEvent event)
  {
    Retailer r;
    int modelIter = 0;
    String modelIterParam = this.controler.getConfig().findParam("Retailers", "modelIteration");
    if (modelIterParam == null) {
      log.warn("The iteration in which the model should be run has not been set, the model run will be performed at the last iteration");
      modelIter = this.controler.getConfig().controler().getLastIteration();
    }
    else {
      modelIter = Integer.parseInt(modelIterParam);
    }

    int analysisFrequency = 0;
    String AnalysisFrequencyParam = this.controler.getConfig().findParam("Retailers", "analysisFrequency");
    if (AnalysisFrequencyParam == null) {
      log.warn("The frequency with which the analysis should be run has not been set, the analysis will be only performed when the model will run and at the last iteration");
      analysisFrequency = this.controler.getConfig().controler().getLastIteration();
    }
    else {
      analysisFrequency = Integer.parseInt(AnalysisFrequencyParam);
    }

    if (event.getIteration() == modelIter) {
    	this.lrr = new LinksRetailerReaderV2(this.controler, this.retailers);
        this.lrr.init();
        final PersonAlgorithm router =
    		  new PlanRouter(
    				  event.getServices().getTripRouterProvider().get() );
        for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) {
        	r = localIterator.next();
        	this.rsw.write(r, event.getIteration(), this.cfc);
        	r.runStrategy(this.lrr.getFreeLinks());
        	this.lrr.updateFreeLinks();
            Map persons = this.controler.getScenario().getPopulation().getPersons();
            new ReRoutePersons().run(
        		r.getMovedFacilities(),
                    this.controler.getScenario().getNetwork(),
        		persons,
        		router,
                    this.controler.getScenario().getActivityFacilities());
     	}
    }
    if ((event.getIteration() != 0) && (event.getIteration() % analysisFrequency == 0) 
    		&& (event.getIteration() != modelIter) 
    		&& (event.getIteration() != this.controler.getConfig().controler().getLastIteration()))
    {
    	log.info("Test1");
    	for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) { r = localIterator.next();

        this.rsw.write(r, event.getIteration(), this.cfc);
      }

    }

    if (event.getIteration() != this.controler.getConfig().controler().getLastIteration())
      return;
    for (Iterator<Retailer> localIterator = this.retailers.getRetailers().values().iterator(); localIterator.hasNext(); ) { r = localIterator.next();

      this.rsw.write(r, event.getIteration(), this.cfc);
      log.info("Test2");
    }

    this.rsw.close();
  }
}
