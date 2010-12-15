/* *********************************************************************** *
 * project: org.matsim.*
 * NewPtBseUCControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.mmoyo.cadyts_integration;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.config.PtCountsConfigGroup;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.NewPtBsePlanStrategy;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseCountsComparisonAlgorithm;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseOccupancyAnalyzer;

class NewPtBseUCControlerListener implements StartupListener , 
											IterationEndsListener, 
											BeforeMobsimListener, 
											AfterMobsimListener {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( NewPtBseUCControlerListener.class );

	private final Counts occupCounts = new Counts();
	private final Counts boardCounts = new Counts();
	private final Counts alightCounts = new Counts();
	private double countsScaleFactor;
	private PtBseOccupancyAnalyzer ptBseOccupAnalyzer;

		
	@Override
	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler() ;
		
		//Add ptBseOccupAnalyzer. Remove Yu's analyzer here? 
		ptBseOccupAnalyzer = new PtBseOccupancyAnalyzer();
		
		// create the strategy:
		PlanStrategy strategy = new NewPtBsePlanStrategy( controler , ptBseOccupAnalyzer) ;

		// add the strategy to the strategy manager:
		controler.getStrategyManager().addStrategy( strategy , 1.0 ) ;
		
		//read occup counts from file
		String occupancyCountsFilename = controler.getConfig().findParam("ptCounts", "inputOccupancyCountsFile");
		if (occupancyCountsFilename != null) {
			new MatsimCountsReader(this.occupCounts).readFile(occupancyCountsFilename);
		}
		this.countsScaleFactor = Double.parseDouble(controler.getConfig().getParam("ptCounts", "countsScaleFactor"));

	}

	//Analysis methods
	///////////////////////////////////////////////////////
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iter = event.getIteration();
		if ( isActiveInThisIteration( iter, event.getControler() ) ) {
			ptBseOccupAnalyzer.reset(iter);
		}
	}
	
	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		int it = event.getIteration();
		if ( isActiveInThisIteration( it, event.getControler() ) ) {
			event.getControler().getEvents().removeHandler(ptBseOccupAnalyzer);
			
			//Get all M44 stations and invoke the method write to get all information of them
			TransitLine lineM44 = event.getControler().getScenario().getTransitSchedule().getTransitLines().get(new IdImpl("B-M44"));
			List<Id> m44StopIds = new ArrayList<Id>();
			for (TransitRoute route :lineM44.getRoutes().values()){
				for (TransitRouteStop stop: route.getStops()){
					m44StopIds.add( stop.getStopFacility().getId());
				}	
				
			}
			String outFile = event.getControler().getControlerIO().getIterationFilename(it, "ptBseOccupancyAnalysis.txt");
			ptBseOccupAnalyzer.write(outFile, this.occupCounts , m44StopIds );
		}
	}
	
	//Determines the pt counts interval (currently each 10 iterations)
	private boolean isActiveInThisIteration( int iter , Controler controler ) {
		return (iter % controler.getConfig().ptCounts().getPtCountsInterval() == 0)  &&  (iter >= controler.getFirstIteration());
	}
	
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		Config config = event.getControler().getConfig();
		PtCountsConfigGroup ptCounts = config.ptCounts() ;
		if (ptCounts.getAlightCountsFileName() != null) { // yyyy this check should reasonably also be done in isActiveInThisIteration.  kai, oct'10
			Controler controler = event.getControler();
			int iter = event.getIteration();
			if ( isActiveInThisIteration( iter, controler ) ) {

				if ( config.ptCounts().getPtCountsInterval() != 10 )
					log.warn("yyyy This may not work when the pt counts interval is different from 10 because I think I changed things at two "
							+ "places but I can't find the other one any more :-(.  (May just be inefficient.)  kai, oct'10" ) ;

				controler.stopwatch.beginOperation("compare with pt counts");

				NetworkImpl network = controler.getNetwork();
				PtBseCountsComparisonAlgorithm ccaBoard = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.boardCounts, network, this.countsScaleFactor);
				PtBseCountsComparisonAlgorithm ccaAlight = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.alightCounts, network, this.countsScaleFactor);
				PtBseCountsComparisonAlgorithm ccaOccupancy = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.occupCounts, network, this.countsScaleFactor);

				String distanceFilterStr = config.findParam("ptCounts", "distanceFilter");
				String distanceFilterCenterNodeId = config.findParam("ptCounts", "distanceFilterCenterNode");
				if ((distanceFilterStr != null)
						&& (distanceFilterCenterNodeId != null)) {
					Double distanceFilter = Double.valueOf(distanceFilterStr);
					ccaBoard.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaAlight.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
					ccaOccupancy.setDistanceFilter(distanceFilter, distanceFilterCenterNodeId);
				}

				ccaBoard.setCountsScaleFactor(countsScaleFactor);
				ccaAlight.setCountsScaleFactor(countsScaleFactor);
				ccaOccupancy.setCountsScaleFactor(countsScaleFactor);
				
				//filter stations here??
				
				ccaBoard.run();
				ccaAlight.run();
				ccaOccupancy.run();

				String outputFormat = config.findParam("ptCounts", "outputformat");
				if (outputFormat.contains("kml") || outputFormat.contains("all")) {
					ControlerIO ctlIO=controler.getControlerIO();

					String filename = ctlIO.getIterationFilename(iter, "ptBseCountscompare.kmz");
					PtCountSimComparisonKMLWriter kmlWriter = new PtCountSimComparisonKMLWriter(ccaBoard.getComparison(), ccaAlight.getComparison(), ccaOccupancy.getComparison(),
							TransformationFactory.getCoordinateTransformation(config.global().getCoordinateSystem(),TransformationFactory.WGS84),
							this.boardCounts, this.alightCounts, this.occupCounts);

					kmlWriter.setIterationNumber(iter);
					kmlWriter.writeFile(filename);
					if (ccaBoard != null) {
						ccaBoard.write(ctlIO.getIterationFilename(iter, "simBseCountCompareBoarding.txt"));
					}
					if (ccaAlight != null) {
						ccaAlight.write(ctlIO.getIterationFilename(iter, "simBseCountCompareAlighting.txt"));
					}
					if (ccaOccupancy != null) {
						ccaOccupancy.write(ctlIO.getIterationFilename(iter,	"simBseCountCompareOccupancy.txt"));
					}
				}

				controler.stopwatch.endOperation("compare with pt counts");
			}
		}
	}

	
}