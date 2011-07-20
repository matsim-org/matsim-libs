/* *********************************************************************** *
 * project: org.matsim.*
 * ptBseAsPlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
//import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
//import org.matsim.core.events.handler.AdditionalTeleportationDepartureEventHandler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.pt.config.PtCountsConfigGroup;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;

public class NewPtBsePlanStrategy implements PlanStrategy, 
											/*AdditionalTeleportationDepartureEventHandler,*/  
											IterationEndsListener, 
											BeforeMobsimListener, 
											AfterMobsimListener  {

	// yyyyyy something beyond just "reset" is needed in terms of events handling, otherwise it does not work.

	private final static Logger log = Logger.getLogger(NewPtBsePlanStrategy.class);

	private PlanStrategy delegate = null ;
	private Controler controler;
	private SimResultsContainerImpl simResults;
	final static String MODULE_NAME = "ptCounts";
	final static String BSE_MOD_NAME = "bse";
	final static String STR_LINKOFFSETFILE = "linkCostOffsets.xml";
	private MATSimUtilityModificationCalibrator<TransitStopFacility> calibrator = null;
	static double countsScaleFactor /*=1*/;  // not so great
	private final Counts occupCounts = new Counts();
	private final Counts boardCounts = new Counts();
	private final Counts alightCounts = new Counts();
	private PtBseOccupancyAnalyzer ptBseOccupAnalyzer;
	static TransitSchedule trSched ;

	public NewPtBsePlanStrategy( Controler controler ) {
		// IMPORTANT: Do not change this constructor.  It needs to be like this in order to be callable as a "Module"
		// from the config file.  kai/manuel, dec'10

		// remember the controler:  (yyyy I don't think this is necessary.  kai, jul'11)
		this.controler = controler ; 

		// add "this" to the events channel so that reset is called between iterations
		// (yyyy I think this should now be better done by the controler listener mechanics.  kai, jul'11)
		/*this.controler.getEvents().addHandler( this ) ; no more*/
		this.controler.addControlerListener(this) ;

		// set up the bus occupancy analyzer ...  
		this.ptBseOccupAnalyzer = new PtBseOccupancyAnalyzer();
		this.controler.getEvents().addHandler(ptBseOccupAnalyzer);
		// ... and connect it to the simResults container:
		this.simResults = new SimResultsContainerImpl( ptBseOccupAnalyzer );

		// this collects events and generates cadyts plans from it
		PtPlanToPlanStepBasedOnEvents ptStep = new PtPlanToPlanStepBasedOnEvents( this.controler.getScenario() /*,  ptBseOccupAnalyzer 18.jul.2011*/ ) ;
		// yyyyyy passing ptBseOccupAnalyzer into PtPlanToPlanStepBasedOnEvents is, I think, unnecessary and should be avoided.
		// See there.  kai, jul'11
		this.controler.getEvents().addHandler( ptStep ) ;

		// build the calibrator.  This is a static method, and in consequence has no side effects
		this.calibrator = CadytsBuilder.buildCalibrator( this.controler.getScenario() );

		// finally, we create the PlanStrategy, with the bse-based plan selector:
		this.delegate = new PlanStrategyImpl( new NewPtBsePlanChanger( ptStep, this.calibrator ) ) ;

		// NOTE: The coupling between calibrator and simResults is done in "reset".
		
		// ===========================
		// everything beyond this line is, I think, analysis code.  kai, jul'11

		//read occup counts from file
		//String occupancyCountsFilename = this.controler.getConfig().findParam("ptCounts", "inputOccupancyCountsFile"); //better read it from config object like below
		String occupancyCountsFilename = this.controler.getConfig().ptCounts().getOccupancyCountsFileName();
		if (occupancyCountsFilename != null) {
			new MatsimCountsReader(this.occupCounts).readFile(occupancyCountsFilename);
		}
		// yyyyyy the counts data is read in "buildCalibrator", and here again.  This is not necessary,
		// and confuses the reader of the program.  kai, jul'11
		
		//countsScaleFactor = Double.parseDouble(this.controler.getConfig().ptCounts().getCountsScaleFactor() //better read it from config object like below
		countsScaleFactor = this.controler.getConfig().ptCounts().getCountsScaleFactor();

		controler.getScenario().addScenarioElement(this.occupCounts) ;
	}

	/*
	@Override
	public void reset(int iteration) {
		// yyyy since this is now also a controler listener, material in here should be moved to "notifyIterationEnds".  kai, jul'11
		
		String filename = this.controler.getControlerIO().getIterationFilename(iteration, STR_LINKOFFSETFILE) ;

		//show in log the results of sim volumes
		//System.out.println( "resultsContainer.toString() " +  simResults.toString() ) ;

		// mobsim results are in resultsContainer, which is (implicitly) an events listener.  Communicate them to the calibrator:
		this.calibrator.afterNetworkLoading(this.simResults);

		// the remaining material is, in my view, "just" output:
		try {
			PtBseLinkCostOffsetsXMLFileIO ptBseLinkCostOffsetsXMLFileIO = new PtBseLinkCostOffsetsXMLFileIO( this.controler.getScenario().getTransitSchedule() );
			ptBseLinkCostOffsetsXMLFileIO.write( filename , this.calibrator.getLinkCostOffsets());
			ptBseLinkCostOffsetsXMLFileIO = null;
		}catch(IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void handleEvent(AdditionalTeleportationDepartureEvent eve) {
		// dummy
	}
	*/

	//Analysis methods
	///////////////////////////////////////////////////////
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		int iter = event.getIteration();
		if ( isActiveInThisIteration( iter, event.getControler() ) ) {
			ptBseOccupAnalyzer.clear();
			event.getControler().getEvents().addHandler(ptBseOccupAnalyzer);  //Necessary because it is removed in notifyAfterMobsim 18.jul.2011
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		int it = event.getIteration();
		if ( isActiveInThisIteration( it, event.getControler() ) ) {
			event.getControler().getEvents().removeHandler(ptBseOccupAnalyzer);

			//Get all M44 stations and invoke the method write to get all information of them
			TransitLine specificLine = event.getControler().getScenario().getTransitSchedule().getTransitLines().get(new IdImpl("B-M44"));
			List<Id> stopIds = new ArrayList<Id>();
			for (TransitRoute route :specificLine.getRoutes().values()){
				for (TransitRouteStop stop: route.getStops()){
					stopIds.add( stop.getStopFacility().getId());
				}

			}
			String outFile = event.getControler().getControlerIO().getIterationFilename(it, "ptBseOccupancyAnalysis.txt");
			ptBseOccupAnalyzer.writeResultsForSelectedStopIds(outFile, this.occupCounts , stopIds );
		}
	}

	//Determines the pt counts interval (currently each 10 iterations)
	private boolean isActiveInThisIteration( int iter , Controler controler ) {
		return (iter % controler.getConfig().ptCounts().getPtCountsInterval() == 0)  &&  (iter >= controler.getFirstIteration());
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		///////originally this was in reset method//////////////
		this.calibrator.afterNetworkLoading(this.simResults);
		// the remaining material is, in my view, "just" output:
		String filename = this.controler.getControlerIO().getIterationFilename(event.getIteration(), STR_LINKOFFSETFILE) ;
		try {
			PtBseLinkCostOffsetsXMLFileIO ptBseLinkCostOffsetsXMLFileIO = new PtBseLinkCostOffsetsXMLFileIO( this.controler.getScenario().getTransitSchedule() );
			ptBseLinkCostOffsetsXMLFileIO.write( filename , this.calibrator.getLinkCostOffsets());
			ptBseLinkCostOffsetsXMLFileIO = null;
		}catch(IOException e) {
			e.printStackTrace();
		}///////////////////////////////////////////////////////
		
		generateAndWriteCountsComparisons(event);
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line
	// yyyyyy this statement is no longer correct since someone added other public methods below.  kai, jul'11

	private void generateAndWriteCountsComparisons(final IterationEndsEvent event) {
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

				Network network = controler.getNetwork();
				PtBseCountsComparisonAlgorithm ccaBoard = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.boardCounts, network, countsScaleFactor);
				PtBseCountsComparisonAlgorithm ccaAlight = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.alightCounts, network, countsScaleFactor);
				PtBseCountsComparisonAlgorithm ccaOccupancy = new PtBseCountsComparisonAlgorithm(this.ptBseOccupAnalyzer, this.occupCounts, network, countsScaleFactor);

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
	
	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		this.delegate.addStrategyModule(module);
	}

	@Override
	public int getNumberOfStrategyModules() {
		return this.delegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(Person person) {
		this.delegate.run(person);
	}

	@Override
	public void init() {
		this.delegate.init();
	}

	@Override
	public void finish() {
		this.delegate.finish();
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return this.delegate.getPlanSelector();
	}

	class SimResultsContainerImpl implements SimResults<TransitStopFacility> {
		private static final long serialVersionUID = 1L;
		private PtBseOccupancyAnalyzer occupancyAnalyzer = null ;

		SimResultsContainerImpl( PtBseOccupancyAnalyzer oa ) {
			this.occupancyAnalyzer = oa ;
		}

		@Override
		public double getSimValue(final TransitStopFacility stop , final int startTime_s, final int endTime_s, final TYPE type) {  //stopFacility or link
			int hour = startTime_s / 3600;
			Id stopId = stop.getId();
			int[] values = this.occupancyAnalyzer.getOccupancyVolumesForStop(stopId);

			if (values == null){
				return 0;
			}

			return values[hour] * NewPtBsePlanStrategy.this.countsScaleFactor;
		}

		@Override
		public String toString() {
			final StringBuffer stringBuffer2 = new StringBuffer();
			final String STOPID = "stopId: ";
			final String VALUES = "; values:";
			final char TAB = '\t';
			final char RETURN =  '\n';

			for ( Id stopId : this.occupancyAnalyzer.getOccupancyStopIds()) {  //Only occupancy!
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append(STOPID ) ;
				stringBuffer.append( stopId ) ;
				stringBuffer.append( VALUES) ;

				boolean hasValues = false;   //only prints stops with volumes > 0
				int[] values = this.occupancyAnalyzer.getOccupancyVolumesForStop(stopId ) ;

				for ( int ii=0 ; ii<values.length ; ii++ ) {
					hasValues = hasValues || (values[ii]>0);

					stringBuffer.append(TAB) ;
					stringBuffer.append( values[ii] ) ;
				}
				stringBuffer.append(RETURN) ;
				if (hasValues)
					stringBuffer2.append(stringBuffer.toString());

			}
			return stringBuffer2.toString() ;
		}

	}
	

	final String getCalibratorSettings() {
		StringBuffer sBuff = new StringBuffer();
		sBuff.append("[BruteForce=" + this.calibrator.getBruteForce() + "]" ); 
		sBuff.append("[CenterRegression=" + this.calibrator.getCenterRegression() + "]" );
		sBuff.append("[FreezeIteration=" + this.calibrator.getFreezeIteration() + "]" );
		sBuff.append("[MinStddev=" + this.calibrator.getMinStddev(SingleLinkMeasurement.TYPE.FLOW_VEH_H) + "]" );
		sBuff.append("[PreparatoryIterations=" + this.calibrator.getPreparatoryIterations() + "]" );
		sBuff.append("[RegressionInertia=" + this.calibrator.getRegressionInertia() + "]" );
		sBuff.append("[VarianceScale=" + this.calibrator.getVarianceScale() + "]" );
		return sBuff.toString();
	}
	
}