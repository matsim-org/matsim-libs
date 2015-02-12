/* *********************************************************************** *
 * project: org.matsim.*
 * CadytsPlanStrategy.java
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

package playground.johannes.gsv.sim.cadyts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;

import playground.johannes.gsv.sim.LinkOccupancyCalculator;
import playground.johannes.gsv.sim.Simulator;
import playground.johannes.gsv.zones.KeyMatrix;
import playground.johannes.gsv.zones.ZoneCollection;
import playground.johannes.gsv.zones.io.KeyMatrixXMLReader;
import playground.johannes.gsv.zones.io.Zone2GeoJSON;
import cadyts.calibrators.analytical.AnalyticalCalibrator;

/**
 * {@link PlanStrategy Plan Strategy} used for replanning in MATSim which uses Cadyts to
 * select plans that better match to given occupancy counts.
 */
public class CadytsContext implements CadytsContextI<Link>, StartupListener, IterationEndsListener {

	private final static Logger log = Logger.getLogger(CadytsContext.class);

	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";
	
//	private final double countsScaleFactor;
	private final Counts counts;
	private final boolean writeAnalysisFile;
	private final CadytsConfigGroup cadytsConfig;
	
	private AnalyticalCalibrator<Link> calibrator;
	private PlanToPlanStepBasedOnEvents ptStep;
	private SimResultsAdaptor simResults;
	
	private final LinkOccupancyCalculator occupancy;
	
	private final double scale;
	
	private final Config config;
	
	public CadytsContext(Config config, Counts counts, LinkOccupancyCalculator occupancy) {
		this.config = config;
		this.scale = config.counts().getCountsScaleFactor();
		this.occupancy = occupancy;
//		this.countsScaleFactor = config.counts().getCountsScaleFactor();
		
		this.cadytsConfig = new CadytsConfigGroup();
		config.addModule(cadytsConfig);
		// addModule() also initializes the config group with the values read from the config file
		cadytsConfig.setWriteAnalysisFile(true);
		
		if ( counts==null ) {
			this.counts = new Counts();
			String occupancyCountsFilename = config.counts().getCountsFileName();
			new MatsimCountsReader(this.counts).readFile(occupancyCountsFilename);
		} else {
			this.counts = counts ;
		}
		
		Set<Id<Link>> countedLinks = new TreeSet<>();
		for (Id<Link> id : this.counts.getCounts().keySet()) {
			countedLinks.add(id);
		}
		
		cadytsConfig.setCalibratedItems(countedLinks);
		
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}
	
//	public CadytsContext(Config config ) {
//		this( config, null ) ;
//	}

	@Override
	public PlansTranslator<Link> getPlansTranslator() {
		return this.ptStep;
	}
	
	@Override
	public void notifyStartup(StartupEvent event) {
		
		Scenario scenario = event.getControler().getScenario();
		
//		VolumesAnalyzer volumesAnalyzer = event.getControler().getVolumes();
		
//		this.simResults = new SimResultsContainerImpl(volumesAnalyzer, this.countsScaleFactor);
		this.simResults = new SimResultsAdaptor(occupancy, scale);
		
		// this collects events and generates cadyts plans from it
		this.ptStep = new PlanToPlanStepBasedOnEvents(scenario, cadytsConfig.getCalibratedItems());
		event.getControler().getEvents().addHandler(ptStep);

		if(Boolean.parseBoolean(config.getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odCalibration"))) {
			KeyMatrixXMLReader reader = new KeyMatrixXMLReader();
			reader.setValidating(false);
			reader.parse(config.getParam(Simulator.GSV_CONFIG_MODULE_NAME, "odMatrixFile"));
			KeyMatrix m = reader.getMatrix();
			
			String data;
			try {
				data = new String(Files.readAllBytes(Paths.get(config.getParam(Simulator.GSV_CONFIG_MODULE_NAME, "zonesFile"))));
				ZoneCollection zones = new ZoneCollection();
				zones.addAll(Zone2GeoJSON.parseFeatureCollection(data));
				
				ODCalibrator odCalibrartor = new ODCalibrator(event.getControler().getScenario().getNetwork(), this, m, zones);
				event.getControler().getEvents().addHandler(odCalibrartor);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		
		// build the calibrator. This is a static method, and in consequence has no side effects
		Logger.getRootLogger().setLevel(Level.FATAL);
		this.calibrator = CadytsBuilder.buildCalibrator(scenario.getConfig(), this.counts , new LinkLookUp(scenario) /*, cadytsConfig.getTimeBinSize()*/, Link.class);
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}
	
	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), event.getControler())) {
				analysisFilepath = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}

		this.calibrator.afterNetworkLoading(this.simResults);

		// write some output
		String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
//			new CadytsLinkCostOffsetsXMLFileIO(event.getControler().getScenario().getNetwork())
			new CadytsCostOffsetsXMLFileIO<Link>(new LinkLookUp(event.getControler().getScenario()), Link.class)
   			   .write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	/**
	 * for testing purposes only
	 */
	@Override
	public AnalyticalCalibrator<Link> getCalibrator() {
		return this.calibrator;
	}
	
	SimResultsAdaptor getSimResultsAdaptor() {
		return simResults;
	}
	
	Counts getCounts() {
		return counts;
	}
	
	double getScalingFactor() {
		return scale;
	}

	// ===========================================================================================================================
	// private methods & pure delegate methods only below this line

	@SuppressWarnings("static-method")
	private boolean isActiveInThisIteration(final int iter, final Controler controler) {
		return (iter > 0 && iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
//		return (iter % controler.getConfig().counts().getWriteCountsInterval() == 0);
	}
	
//	/*package*/ static class SimResultsContainerImpl implements SimResults<Link> {
//		private static final long serialVersionUID = 1L;
//		private final VolumesAnalyzer volumesAnalyzer;
//		private final double countsScaleFactor;
//
//		SimResultsContainerImpl(final VolumesAnalyzer volumesAnalyzer, final double countsScaleFactor) {
//			this.volumesAnalyzer = volumesAnalyzer;
//			this.countsScaleFactor = countsScaleFactor;
//		}
//
//		@Override
//		public double getSimValue(final Link link, final int startTime_s, final int endTime_s, final TYPE type) { // stopFacility or link
//
//			Id<Link> linkId = link.getId();
//			double[] values = volumesAnalyzer.getVolumesPerHourForLink(linkId);
//			
//			if (values == null) {
//				return 0;
//			}
//			
//			int startHour = startTime_s / 3600;
//			int endHour = (endTime_s-3599)/3600 ;
//			// (The javadoc specifies that endTime_s should be _exclusive_.  However, in practice I find 7199 instead of 7200.  So
//			// we are giving it an extra second, which should not do any damage if it is not used.) 
//			if (endHour < startHour) {
//				System.err.println(" startTime_s: " + startTime_s + "; endTime_s: " + endTime_s + "; startHour: " + startHour + "; endHour: " + endHour );
//				throw new RuntimeException("this should not happen; check code") ;
//			}
//			double sum = 0. ;
//			for ( int ii=startHour; ii<=endHour; ii++ ) {
//				sum += values[startHour] ;
//			}
//			switch(type){
//			case COUNT_VEH:
//				return sum * this.countsScaleFactor ;
//			case FLOW_VEH_H:
//				return 3600*sum / (endTime_s - startTime_s) * this.countsScaleFactor ;
//			default:
//				throw new RuntimeException("count type not implemented") ;
//			}
//
//		}
//
//		@Override
//		public String toString() {
//			final StringBuffer stringBuffer2 = new StringBuffer();
//			final String LINKID = "linkId: ";
//			final String VALUES = "; values:";
//			final char TAB = '\t';
//			final char RETURN = '\n';
//
//			for (Id linkId : this.volumesAnalyzer.getLinkIds()) { // Only occupancy!
//				StringBuffer stringBuffer = new StringBuffer();
//				stringBuffer.append(LINKID);
//				stringBuffer.append(linkId);
//				stringBuffer.append(VALUES);
//
//				boolean hasValues = false; // only prints stops with volumes > 0
//				int[] values = this.volumesAnalyzer.getVolumesForLink(linkId);
//
//				for (int ii = 0; ii < values.length; ii++) {
//					hasValues = hasValues || (values[ii] > 0);
//
//					stringBuffer.append(TAB);
//					stringBuffer.append(values[ii]);
//				}
//				stringBuffer.append(RETURN);
//				if (hasValues) stringBuffer2.append(stringBuffer.toString());
//			}
//			return stringBuffer2.toString();
//		}
//
//	}
}