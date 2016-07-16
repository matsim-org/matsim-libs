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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author dgrether
 */
public class SimSimTrafficAnalyser {

	private static final Logger log = Logger.getLogger(SimSimTrafficAnalyser.class);

	private SimSimCountsAnalysis countsAna; 
	
	private Network loadNetwork(String networkFile){
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		return scenario.getNetwork();
	}
	
	
	private void writeKML(Network network, List<CountSimComparison> countSimComp, String outfile, String srs){
	}
	
	
	private VolumesAnalyzer loadVolumes(Network network , String eventsFile){
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return va;
	}
	
	public void runAnalysis(String networkFile, String eventsFileCounts, String eventsFileSim, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		VolumesAnalyzer vaCounts = loadVolumes(network, eventsFileCounts);
		VolumesAnalyzer vaSim = loadVolumes(network, eventsFileSim);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		
		SimSimCountsAnalysis countsAnalysis = new SimSimCountsAnalysis();
		Map<Id, List<CountSimComparison>> countSimCompMap = countsAnalysis.createCountSimComparisonByLinkId(filteredNetwork, vaCounts, vaSim);
		
		new CountsShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile + ".shp", countSimCompMap);

		List<CountSimComparison> countSimComp = new ArrayList<CountSimComparison>();
		for (List<CountSimComparison> list : countSimCompMap.values()){
			countSimComp.addAll(list);
		}
		
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
				countSimComp, network, TransformationFactory.getCoordinateTransformation(srs, TransformationFactory.WGS84));
		kmlWriter.writeFile(outfile);

		new CountsErrorTableWriter().writeErrorTable(countSimComp, outfile);


	}


	private Network applyNetworkFilter(Network network, CoordinateReferenceSystem networkSrs) {
		log.info("Filtering network...");
		log.info("Nr links in original network: " + network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature("/media/data/work/repos/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
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
//			net = "/media/data/work/repos/runs-svn/run1733/1733.output_network.xml.gz";
//			eventsFileCountValues = "/media/data/work/repos/runs-svn/run1712/ITERS/it.1000/1712.1000.events.xml.gz";
//			eventsFileSimValues = "/media/data/work/repos/runs-svn/run1733/ITERS/it.1000/1733.1000.events.xml.gz";
//			outfile = "/media/data/work/repos/runs-svn/run1733/ITERS/it.1000/1712.1000vs1733.1000";
//	
			
			net = "/media/data/work/repos/runs-svn/run1722/1722.output_network.xml.gz";
			eventsFileCountValues = "/media/data/work/repos/runs-svn/run1722/ITERS/it.1000/1722.1000.events.xml.gz";
			eventsFileSimValues = "/media/data/work/repos/runs-svn/run1740/ITERS/it.2000/1740.2000.events.xml.gz";
			outfile = "/media/data/work/repos/runs-svn/run1740/ITERS/it.2000/1722.1000vs1740.2000";
//
			srs = TransformationFactory.WGS84_UTM33N;
			
			
		}
		else {
			net = args[0];
			eventsFileCountValues = args[1];
			eventsFileSimValues = args[2];
			outfile = args[3];
			srs = args[4];
		}


		SimSimTrafficAnalyser analyser = new SimSimTrafficAnalyser();
		analyser.runAnalysis(net, eventsFileCountValues, eventsFileSimValues, srs, outfile);
	}

}
