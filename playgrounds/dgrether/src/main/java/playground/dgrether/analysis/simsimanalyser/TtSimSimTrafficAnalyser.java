/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.simsimanalyser;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.CountSimComparison;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 */
public class TtSimSimTrafficAnalyser {

	private static final Logger log = Logger.getLogger(TtSimSimTrafficAnalyser.class);

	private SimSimCountsAnalysis countsAna; 
	
	private Network loadNetwork(String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		return scenario.getNetwork();
	}
	
	
	private TtVolumesAnalyzer loadVolumes(Network network , String eventsFile){
//		TtVolumesAnalyzer va = new TtVolumesAnalyzer(3600, 24 * 3600, network); // one time bin corresponds to one hour
		TtVolumesAnalyzer va = new TtVolumesAnalyzer(60*5, 24 * 3600, network); // one time bin corresponds to five minutes
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return va;
	}
	
	public void runAnalysis(String networkFile, String eventsFileCounts, String eventsFileSim, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		TtVolumesAnalyzer vaCounts = loadVolumes(network, eventsFileCounts);
		TtVolumesAnalyzer vaSim = loadVolumes(network, eventsFileSim);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		
		TtSimSimAnalysis countsAnalysis = new TtSimSimAnalysis();
		Map<Id, List<CountSimComparison>> countSimLinkLeaveCompMap = countsAnalysis.createCountSimLinkLeaveComparisonByLinkId(filteredNetwork, vaCounts, vaSim);
		Map<Id, List<CountSimComparison>> countSimLinkEnterCompMap = countsAnalysis.createCountSimLinkEnterComparisonByLinkId(filteredNetwork, vaCounts, vaSim);
		
		new SimSimMorningLeaveAndEnterShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile + ".shp", countSimLinkLeaveCompMap, countSimLinkEnterCompMap);
//		new SimSimMorningShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile + ".shp", countSimLinkLeaveCompMap); // old version

		
//		List<CountSimComparison> countSimComp = new ArrayList<CountSimComparison>();
//		for (List<CountSimComparison> list : countSimCompMap.values()){
//			countSimComp.addAll(list);
//		}
//		
//		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
//				countSimComp, network, TransformationFactory.getCoordinateTransformation(srs, TransformationFactory.WGS84));
//		kmlWriter.writeFile(outfile);
//
//		new CountsErrorTableWriter().writeErrorTable(countSimComp, outfile);


	}


	private Network applyNetworkFilter(Network network, CoordinateReferenceSystem networkSrs) {
		log.info("Filtering network...");
		log.info("Nr links in original network: " + network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature("C:/Users/Atany/Desktop/SHK/SVN/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		FeatureNetworkLinkCenterCoordFilter filter = new FeatureNetworkLinkCenterCoordFilter(networkSrs, cottbusFeatureTuple.getSecond(), cottbusFeatureTuple.getFirst());
		netFilter.addLinkFilter(filter);
		Network fn = netFilter.applyFilters();
		log.info("Nr of links in filtered network: " + fn.getLinks().size());
		return fn;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String net = null;
		String eventsFileCountValues = null;
		String eventsFileSimValues = null;
		String outfile = null;
		String srs = null;

		if (args == null || args.length == 0){
			
			String runNr1 = "1910";
			String runNr2 = "1913";
						
			net = DgPaths.REPOS + "runs-svn/run"+runNr1+"/"+runNr1+".output_network.xml.gz";
			eventsFileCountValues = DgPaths.REPOS + "runs-svn/run"+runNr1+"/ITERS/it.2000/"+runNr1+".2000.events.xml.gz";
			eventsFileSimValues = DgPaths.REPOS + "runs-svn/run"+runNr2+"/ITERS/it.2000/"+runNr2+".2000.events.xml.gz";
			outfile = DgPaths.REPOS + "runs-svn/run"+runNr1+"/shapefiles/"+runNr1+".2000vs"+runNr2+".2000_densityPer5min";
			
//			net = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/"+runNr1+".output_network.xml.gz";
//			eventsFileCountValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/ITERS/it.2000/"+runNr1+".2000.events.xml.gz";
//			eventsFileSimValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr2+"/ITERS/it.2000/"+runNr2+".2000.events.xml.gz";
//			outfile = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/shapefiles/"+runNr1+".2000vs"+runNr2+".2000_densityPer5min";

			srs = TransformationFactory.WGS84_UTM33N;
			
			
		}
		else {
			net = args[0];
			eventsFileCountValues = args[1];
			eventsFileSimValues = args[2];
			outfile = args[3];
			srs = args[4];
		}


		TtSimSimTrafficAnalyser analyser = new TtSimSimTrafficAnalyser();
		analyser.runAnalysis(net, eventsFileCountValues, eventsFileSimValues, srs, outfile);
	}

}
