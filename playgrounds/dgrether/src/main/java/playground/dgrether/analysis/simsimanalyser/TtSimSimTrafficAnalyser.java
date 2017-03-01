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

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.koehlerstrehlersignal.analysis.TtDelayPerLink;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;


/**
 * @author tthunig
 */
public class TtSimSimTrafficAnalyser {

	private static final Logger log = Logger.getLogger(TtSimSimTrafficAnalyser.class);
	
	private Network loadNetwork(String networkFile){
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		return scenario.getNetwork();
	}
	
	
	private VolumesAnalyzer loadVolumes(Network network , String eventsFile){
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, network); // one time bin corresponds to one hour
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return va;
	}
	
	private TtDelayPerLink calculateDelay(Network network, String eventsFile){
		TtDelayPerLink delayPerLink = new TtDelayPerLink(network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(delayPerLink);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return delayPerLink;
	}
	
	public void runAnalysis(String networkFile, String eventsFileCounts, String eventsFileSim, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		VolumesAnalyzer vaCounts = loadVolumes(network, eventsFileCounts);
		VolumesAnalyzer vaSim = loadVolumes(network, eventsFileSim);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
//		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		Network filteredNetwork = network;
		
		SimSimAnalysis countsAnalysis = new SimSimAnalysis();
		Map<Id<Link>, List<CountSimComparison>> countSimLinkLeaveCompMap = countsAnalysis.createCountSimComparisonByLinkId(filteredNetwork, vaCounts, vaSim);
		
		Map<Id<Link>, Double> delayPerLink1 = calculateDelay(network, eventsFileCounts).getDelayPerLink();
		Map<Id<Link>, Double> delayPerLink2 = calculateDelay(network, eventsFileSim).getDelayPerLink();
		
		new SimSimMorningShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile + ".shp", countSimLinkLeaveCompMap, delayPerLink1, delayPerLink2);
	}


	private Network applyNetworkFilter(Network network, CoordinateReferenceSystem networkSrs) {
		log.info("Filtering network...");
		log.info("Nr links in original network: " + network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, SimpleFeature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature("../../../shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
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
			
//			String runNr1 = "1745"; // base case (based on 1712), changes in routes + times
//			String runNr2 = "1746"; // commodities > 10
//			String runNr2 = "1747"; // commodities > 50
//			String runNr2 = "1748"; // sylvia
			
			String runNr1 = "1910"; // base case (based on 1712), changes in routes only
			String runNr2 = "1911"; // commodities > 10
//			String runNr2 = "1912"; // commodities > 50
//			String runNr2 = "1913"; // sylvia
			
//			net = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/"+runNr1+".output_network.xml.gz";
//			eventsFileCountValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/ITERS/it.2000/"+runNr1+".2000.events.xml.gz";
//			eventsFileSimValues = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr2+"/ITERS/it.2000/"+runNr2+".2000.events.xml.gz";
//			outfile = "C:/Users/Atany/Desktop/SHK/SVN/runs-svn/run"+runNr1+"/shapefiles/"+runNr2+".2000-"+runNr1+".2000_morningPeakAnalysis";
			
			final String BASE_DIR = "../../../runs-svn/cottbus/createGridLock/";
			final String RUN1 = 
					"2017-02-3_100it_cap0.5_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_ALL_GREEN_INSIDE_ENVELOPE_5plans_WoMines_V1/";
//					"2017-02-15_100it_cap0.5_ReRoute0.1_tbs900_ChExp0.9_beta2_lanes_2link_CORDON_INNERCITY_5plans_WoMines_V1/";
//					"2017-02-15_100it_cap0.5_ReRoute0.1_tbs900_ChExp0.9_beta2_lanes_2link_CORDON_INNERCITY_5plans_WoMines_V1/";
			final String RUN2 = 
//					"2017-02-13_100it_cap0.5_ReRoute0.1_tbs900_ChExp0.9_beta2_lanes_2link_ALL_DOWNSTREAM_INSIDE_ENVELOPE_5plans_WoMines_V1/";
//					"2017-02-15_100it_cap0.5_ReRoute0.1_tbs900_ChExp0.9_beta2_lanes_2link_CORDON_RING_5plans_WoMines_V1/";
//					"2017-02-15_100it_cap0.5_ReRoute0.1_tbs900_ChExp0.9_beta2_lanes_2link_CORDON_INNERCITY_5plans_WoMines_V1/";
					"2017-02-15_100it_cap0.7_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_MS_SYLVIA_5plans_WoMines_V1/";
			net = BASE_DIR + RUN1 + "output_network.xml.gz";
			eventsFileCountValues = BASE_DIR + RUN1 + "output_events.xml.gz";
			eventsFileSimValues = BASE_DIR + RUN2 + "output_events.xml.gz";
			
			// get the current date in format "yyyy-mm-dd"
			Calendar cal = Calendar.getInstance ();
			// this class counts months from 0, but days from 1
			int month = cal.get(Calendar.MONTH) + 1;
			String monthStr = month + "";
			if (month < 10)
				monthStr = "0" + month;
			String date = cal.get(Calendar.YEAR) + "-" 
					+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
			
			outfile = BASE_DIR + "diffNets/" + date + "_cap0.5-7_greenEnvelopeVsCordonTollInnercity100_100it_0-24h";

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
