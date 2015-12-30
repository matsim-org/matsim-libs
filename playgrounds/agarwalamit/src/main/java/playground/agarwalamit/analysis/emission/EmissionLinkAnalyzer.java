/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.analysis.emission;

import java.io.BufferedWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.emission.sorting.FilteredColdEmissionPerLinkHandler;
import playground.agarwalamit.analysis.emission.sorting.FilteredWarmEmissionPerLinkHandler;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;
import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * @author amit
 *
 */
public class EmissionLinkAnalyzer extends AbstractAnalysisModule {
	private static final Logger logger = Logger.getLogger(EmissionLinkAnalyzer.class);
	private final String emissionEventsFile;
	private EmissionUtils emissionUtils;
	private FilteredWarmEmissionPerLinkHandler warmHandler;
	private FilteredColdEmissionPerLinkHandler coldHandler;
	private Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> link2WarmEmissions;
	private Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> link2ColdEmissions;
	private SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> link2TotalEmissions;
	private SortedMap<String,Double> totalEmissions = new TreeMap<>();

	/**
	 * This will compute the emissions only from links falling inside the given shape.
	 */
	public EmissionLinkAnalyzer(double simulationEndTime, String emissionEventFile, int noOfTimeBins, String shapeFile, Network network ) {
		super(EmissionLinkAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionEventFile;
		logger.info("Aggregating emissions for each "+simulationEndTime/noOfTimeBins+" sec time bin.");
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new FilteredWarmEmissionPerLinkHandler(simulationEndTime, noOfTimeBins, shapeFile, network);
		this.coldHandler = new FilteredColdEmissionPerLinkHandler(simulationEndTime, noOfTimeBins, shapeFile, network);
	}

	public EmissionLinkAnalyzer(double simulationEndTime, String emissionEventFile, int noOfTimeBins) {
		super(EmissionLinkAnalyzer.class.getSimpleName());
		this.emissionEventsFile = emissionEventFile;
		logger.info("Aggregating emissions for each "+simulationEndTime/noOfTimeBins+" sec time bin.");
		this.emissionUtils = new EmissionUtils();
		this.warmHandler = new FilteredWarmEmissionPerLinkHandler(simulationEndTime, noOfTimeBins);
		this.coldHandler = new FilteredColdEmissionPerLinkHandler(simulationEndTime, noOfTimeBins);
	}

	public static void main(String[] args) {
		String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/hEART/output/";
		String [] runCases =  {"bau","ei","5ei","10ei","15ei","20ei","25ei"};
		String shapeFile_city = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		String shapeFile_mma = "/Users/amit/Documents/repos/shared-svn/projects/detailedEval/Net/boundaryArea/munichMetroArea_correctedCRS_simplified.shp";
		
		Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(dir+"/bau/output_network.xml.gz");
		BufferedWriter writer = IOUtils.getBufferedWriter(dir+"/analysis/totalEmissionCosts_metroArea.txt");
		try{
			writer.write("scenario \t totalCostEUR \n");
			for(String str : runCases){
				String emissionEventFile = dir+str+"/ITERS/it.1500/1500.emission.events.xml.gz";

				EmissionLinkAnalyzer ela = new EmissionLinkAnalyzer(30*3600, emissionEventFile, 1, shapeFile_mma, sc.getNetwork());
				ela.preProcessData();
				ela.postProcessData();

				writer.write(str+"\t"+ela.getTotalEmissionsCosts()+"\n");
			}
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		return handler;
	}

	@Override
	public void preProcessData() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		emissionReader.parse(this.emissionEventsFile);
	}

	@Override
	public void postProcessData() {
		this.link2WarmEmissions = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		this.link2ColdEmissions = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		this.link2TotalEmissions = sumUpEmissionsPerTimeInterval(this.link2WarmEmissions, this.link2ColdEmissions);
	}

	@Override
	public void writeResults(String outputFolder) {
		SortedMap<Double,Double> time2cost = getTimebinToEmissionsCosts();
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/time2totalEmissionCosts.txt");
		try{
			writer.write("timebin \t totalCostEUR \n");
			double totalEmissionCost =0. ;
			for(double timebin : time2cost.keySet()){
				writer.write(timebin+"\t"+time2cost.get(timebin)+"\n");
				totalEmissionCost += time2cost.get(timebin);
			}
			writer.write("totalCost \t"+totalEmissionCost+"\n");
			writer.close();
		} catch (Exception e){
			throw new RuntimeException("Data is not written in the file. Reason - "+e);
		}
	}

	private SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {

		SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> time2totalEmissions = new TreeMap<>();

		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			Map<Id<Link>, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
			
			Map<Id<Link>, SortedMap<String, Double>> totalEmissions = this.emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		
			this.totalEmissions = MapUtils.addMaps(this.totalEmissions, this.emissionUtils.getTotalEmissions(totalEmissions));
		}
		return time2totalEmissions;
	}

	public SortedMap<Double, Map<Id<Link>, SortedMap<String, Double>>> getLink2TotalEmissions() {
		return this.link2TotalEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> getLink2WarmEmissions() {
		return link2WarmEmissions;
	}

	public Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> getLink2ColdEmissions() {
		return link2ColdEmissions;
	}

	public SortedMap<Double,Double> getTimebinToEmissionsCosts(){
		SortedMap<Double, Double> time2cost = new TreeMap<>();
		for(double time : this.link2TotalEmissions.keySet()){
			double cost = 0.;
			for (Id<Link> linkid : this.link2TotalEmissions.get(time).keySet()){
				for(EmissionCostFactors ecf:EmissionCostFactors.values()){
					if ( this.link2TotalEmissions.containsKey(time) && this.link2TotalEmissions.get(time).containsKey(linkid) 
							&& this.link2TotalEmissions.get(time).get(linkid).containsKey(ecf.toString()) )
						cost += this.link2TotalEmissions.get(time).get(linkid).get(ecf.toString()) * ecf.getCostFactor();
					else cost += 0.;
				}
			}
			time2cost.put(time, cost);
		}
		return time2cost;
	}
	
	public double getTotalEmissionsCosts(){
		double totalEmissionCosts = 0;
		for(EmissionCostFactors ecf:EmissionCostFactors.values()){
			totalEmissionCosts += ecf.getCostFactor() * this.totalEmissions.get(ecf.toString());
		}
		return totalEmissionCosts;
	}
	
	public SortedMap<String, Double> getTotalEmissions(){
		return this.totalEmissions;
	}
}