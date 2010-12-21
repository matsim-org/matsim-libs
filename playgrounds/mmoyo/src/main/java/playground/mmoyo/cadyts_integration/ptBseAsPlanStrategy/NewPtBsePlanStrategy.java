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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
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
import org.matsim.core.events.AdditionalTeleportationDepartureEvent;
import org.matsim.core.events.handler.AdditionalTeleportationDepartureEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.Volume;
import org.matsim.pt.config.PtCountsConfigGroup;
import org.matsim.pt.counts.PtCountSimComparisonKMLWriter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseCountsComparisonAlgorithm;
import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.PtBseOccupancyAnalyzer;
import cadyts.interfaces.matsim.MATSimUtilityModificationCalibrator;
import cadyts.measurements.SingleLinkMeasurement;
import cadyts.measurements.SingleLinkMeasurement.TYPE;
import cadyts.supply.SimResults;
import cadyts.utilities.misc.DynamicData;

public class NewPtBsePlanStrategy implements PlanStrategy, AdditionalTeleportationDepartureEventHandler,  
IterationEndsListener, 
BeforeMobsimListener, 
AfterMobsimListener  {
	// yyyyyy something beyond just "reset" is needed in terms of events handling, otherwise it does not work.

	private PlanStrategy delegate = null ;
	private Controler controler;
	private SimResultsContainerImpl simResults;
	private final static String MODULE_NAME = "ptCounts";
	private final static String BSE_MOD_NAME = "bse";
	private MATSimUtilityModificationCalibrator<TransitStopFacility> calibrator = null;
	private static double countsScaleFactor = 1 ;  // not so great
	private final Counts occupCounts = new Counts();
	private final Counts boardCounts = new Counts();
	private final Counts alightCounts = new Counts();
	private PtBseOccupancyAnalyzer ptBseOccupAnalyzer;
		

	public NewPtBsePlanStrategy( Controler controler ) {
		// IMPORTANT: Do not change this constructor.  It needs to be like this in order to be callable as a "Module"
		// from the config file.  kai/manuel, dec'10
		
		// under normal circumstances, this is called relatively late in the initialization sequence, since otherwise the
		// strategyManager to which this needs to be added is not yet there. Thus, everything that was in notifyStartup can go
		// here. kai, oct'10
		
		// remember the controler:
		this.controler = controler ;
		
		// add "this" to the events channel so that reset is called between iterations
		controler.getEvents().addHandler( this ) ;
		controler.addControlerListener(this) ;

		// set up the bus occupancy analyzer    <- no more. a PtBseOccupancyAnalyzer object comes above as parameter
		PtBseOccupancyAnalyzer ptBseOccupAnalyzer = new PtBseOccupancyAnalyzer();
		controler.getEvents().addHandler(ptBseOccupAnalyzer);
		
		// this collects events and generates cadyts plans from it
		PtPlanToPlanStepBasedOnEvents ptStep = new PtPlanToPlanStepBasedOnEvents( controler.getScenario() , ptBseOccupAnalyzer ) ;
		controler.getEvents().addHandler( ptStep ) ;
		
		//fill  the linkId_stopId_Map  that stores the relation link -> stop
		// yyyy if we move the bus stop id's, this is no longer needed.  kai, oct'10
//LINK		this.transitSchedule = controler.getScenario().getTransitSchedule();
//LINK		this.linkId_stopId_Map = new TreeMap<Id, Id>();
//LINK		for (TransitStopFacility trStopFac : this.transitSchedule.getFacilities().values()){
//LINK			this.linkId_stopId_Map.put(trStopFac.getLinkId(), trStopFac.getId());
//LINK	}

		// prepare resultsContainer. 
		this.simResults = new SimResultsContainerImpl( ptBseOccupAnalyzer );
		
		// build the calibrator.  This is a static method, and in consequence has no side effects
		this.calibrator = buildCalibrator( controler.getScenario() );

		// finally, we create the PlanStrategy, with the bse-based plan selector:
		this.delegate = new PlanStrategyImpl( new NewPtBsePlanChanger( ptStep, this.calibrator ) ) ;
		
		// NOTE: The coupling between calibrator and simResults is done in "reset".
		
		//read occup counts from file
		String occupancyCountsFilename = controler.getConfig().findParam("ptCounts", "inputOccupancyCountsFile");
		if (occupancyCountsFilename != null) {
			new MatsimCountsReader(this.occupCounts).readFile(occupancyCountsFilename);
		}
		countsScaleFactor = Double.parseDouble(controler.getConfig().getParam("ptCounts", "countsScaleFactor"));


	}
	
	@Override
	public void reset(int iteration) {
		String filename = this.controler.getControlerIO().getIterationFilename(iteration, "linkCostOffsets.xml") ;
		
		//show in log the results of sim volumes
		//System.out.println( "resultsContainer.toString() " +  simResults.toString() ) ;
		
		// mobsim results are in resultsContainer, which is (implicitly) an events listener.  Communicate them to the calibrator:
		this.calibrator.afterNetworkLoading(this.simResults);

		// the remaining material is, in my view, "just" output:
		try {
			DynamicData</*Link*/TransitStopFacility> linkCostOffsets = this.calibrator.getLinkCostOffsets();
			new PtBseLinkCostOffsetsXMLFileIO( this.controler.getScenario().getTransitSchedule() ).write( filename , linkCostOffsets);
		}catch(IOException e) {
			e.printStackTrace();
		}

	}
	
	@Override
	public void handleEvent(AdditionalTeleportationDepartureEvent eve) {
		// dummy
	}


	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line
	
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
	
	private static MATSimUtilityModificationCalibrator<TransitStopFacility> buildCalibrator(final Scenario sc) {
			// made this method static so that there cannot be any side effects.  kai, oct'10
			
			Config config = sc.getConfig();
			TransitSchedule trSched = ((ScenarioImpl) sc).getTransitSchedule() ;
			
			// get default regressionInertia
			String regressionInertiaValue = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "regressionInertia");
			double regressionInertia = 0;
			if (regressionInertiaValue == null) {
				regressionInertia = MATSimUtilityModificationCalibrator.DEFAULT_REGRESSION_INERTIA;
			} else {
				regressionInertia = Double.parseDouble(regressionInertiaValue);
				// this works since it is used in the constructor
			}
			
			MATSimUtilityModificationCalibrator<TransitStopFacility> calibrator = new MATSimUtilityModificationCalibrator <TransitStopFacility>(MatsimRandom.getLocalInstance(), regressionInertia);
			//MATSimUtilityModificationCalibrator<TransitStopFacility> calibrator = new MATSimUtilityModificationCalibrator<TransitStopFacility>("calibration-log.txt", MatsimRandom.getLocalInstance().nextLong(), 3600);
			//calibrator.setRegressionInertia(regressionInertia);
			
			// Set default standard deviation
			{
				String value = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "minFlowStddevVehH");
				if (value != null) {
					double stddev_veh_h = Double.parseDouble(value);
					calibrator.setMinStddev(stddev_veh_h, TYPE.FLOW_VEH_H);
					System.out.println("BSE:\tminFlowStddevVehH\t=\t" + stddev_veh_h);
				}
			}
			
			//SET MAX DRAWS
			/*
			{
				final String maxDrawStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "maxDraws");
				if (maxDrawStr != null) {
					final int maxDraws = Integer.parseInt(maxDrawStr);
					System.out.println("BSE:\tmaxDraws=" + maxDraws);
					calibrator.setMaxDraws(maxDraws);
				} 
			}
			*/
	
			// SET FREEZE ITERATION
			{
				final String freezeIterationStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "freezeIteration");
				if (freezeIterationStr != null) {
					final int freezeIteration = Integer.parseInt(freezeIterationStr);
					System.out.println("BSE:\tfreezeIteration\t= " + freezeIteration);
					calibrator.setFreezeIteration(freezeIteration);
				} 
			}

			// SET Preparatory Iterations
			{
				final String preparatoryIterationsStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "preparatoryIterations");
				if (preparatoryIterationsStr != null) {
					final int preparatoryIterations = Integer.parseInt(preparatoryIterationsStr);
					System.out.println("BSE:\tpreparatoryIterations\t= " + preparatoryIterations);
					calibrator.setPreparatoryIterations(preparatoryIterations);
				} 
			}
	
			// SET varianceScale
			{
				final String varianceScaleStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "varianceScale");
				if (varianceScaleStr != null) {
					final double varianceScale = Double.parseDouble(varianceScaleStr);
					System.out.println("BSE:\tvarianceScale\t= " + varianceScale);
					calibrator.setVarianceScale(varianceScale);
				} 
			}
	
			//SET useBruteForce
			{
				final String useBruteForceStr = config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "useBruteForce");
				if (useBruteForceStr != null) {
					final boolean useBruteForce = new Boolean(useBruteForceStr).booleanValue();
					System.out.println("BSE:\tuseBruteForce\t= " + useBruteForce);
					calibrator.setBruteForce(useBruteForce);
				} 
			}
			
			calibrator.setStatisticsFile("calibration-stats.txt");
			
			// SET countsScale
			//double countsScaleFactor = config.counts().getCountsScaleFactor(); this is for private autos and we don't have this parameter in config file
			countsScaleFactor = Double.parseDouble(config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "countsScaleFactor"));
			System.out.println("BSE:\tusing the countsScaleFactor of " + countsScaleFactor + " as packetSize from config.");
			// yyyy how is this information moved into cadyts?
			//in inner class SimResultsContainerImpl.getSimValue with "return values[hour] * countsScaleFactor;" 
			
			// pt counts data were already read by ptContolerListener of controler. Can that information get achieved from here?
			// Should be in Scenario or ScenarioImpl.  If it is not there, it should be added there.  kai, oct'10
			
			//add a module in config not in file but "in execution"
			//reads occupancy counts data based on stops

			String countsFilename = config.findParam(NewPtBsePlanStrategy.MODULE_NAME, "inputOccupancyCountsFile");
			if ( countsFilename==null ) {
				throw new RuntimeException("could not get counts filename from config; aborting" ) ;
			}
			
			Counts occupCounts = new Counts() ;
			new MatsimCountsReader(occupCounts).readFile(countsFilename);
			if (occupCounts.getCounts().size()==0){
				throw new RuntimeException("BSE requires counts-data.");
			}
			
			// set up center and radius of counts stations locations
	//		distanceFilterCenterNodeCoord = network.getNodes().get(new IdImpl(config.findParam("counts", "distanceFilterCenterNode"))).getCoord();
	//		distanceFilter = Double.parseDouble(config.findParam("counts", "distanceFilter"));
			int arStartTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "startTime"));
			int arEndTime = Integer.parseInt(config.findParam(NewPtBsePlanStrategy.BSE_MOD_NAME, "endTime"));
	
			//add counts data into calibrator
			for (Map.Entry<Id, Count> entry : occupCounts.getCounts().entrySet()) {
				TransitStopFacility stop= trSched.getFacilities().get(entry.getKey());
				for (Volume volume : entry.getValue().getVolumes().values()){        
					if (volume.getHour() >= arStartTime && volume.getHour() <= arEndTime) {    //add volumes for each hour to calibrator
						int start_s = (volume.getHour() - 1) * 3600;
						int end_s = volume.getHour() * 3600 - 1;
						double val_passager_h = volume.getValue();
						calibrator.addMeasurement(stop, start_s, end_s, val_passager_h, SingleLinkMeasurement.TYPE.FLOW_VEH_H);
					}
				}
			}
	
			return calibrator ;
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
					Logger.getLogger(this.getClass()).warn("yyyy This may not work when the pt counts interval is different from 10 because I think I changed things at two "
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


}