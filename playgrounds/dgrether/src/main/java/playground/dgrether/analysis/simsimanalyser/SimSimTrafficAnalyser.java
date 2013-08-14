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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
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
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;
import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.utils.DoubleArrayTableWriter;


/**
 * @author dgrether
 */
public class SimSimTrafficAnalyser {

	private static final Logger log = Logger.getLogger(SimSimTrafficAnalyser.class);

	
	private CalcLinkStats loadLinkStats(Network network, String linkAttributes) {
		CalcLinkStats linkStats = new CalcLinkStats(network);
		linkStats.readFile(linkAttributes);
		return linkStats;
	}

	private Network loadNetwork(String networkFile){
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().network().setInputFile(networkFile);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadNetwork();
		return scenario.getNetwork();
	}
	
	
	private Map<Id, List<CountSimComparison>> createCountSimComparison(Network network, VolumesAnalyzer va1, VolumesAnalyzer va2){
		Map<Id, List<CountSimComparison>> countSimComp = new HashMap<Id, List<CountSimComparison>>(network.getLinks().size());

		for (Link l : network.getLinks().values()) {
			double[] volumes = va1.getVolumesPerHourForLink(l.getId());
			double[] volumes2 = va2.getVolumesPerHourForLink(l.getId());

			if ((volumes.length == 0) || (volumes2.length == 0)) {
				log.warn("No volumes for link: " + l.getId().toString());
				continue;
			}
			ArrayList<CountSimComparison> cscList = new ArrayList<CountSimComparison>();
			countSimComp.put(l.getId(), cscList);
			for (int hour = 1; hour <= 24; hour++) {
				double sim1Value=volumes[hour-1];
				double sim2Value=volumes2[hour-1];
				countSimComp.get(l.getId()).add(new CountSimComparisonImpl(l.getId(), hour, sim1Value, sim2Value));
			}
			//sort the list
			Collections.sort(cscList, new Comparator<CountSimComparison>() {
				@Override
				public int compare(CountSimComparison c1, CountSimComparison c2) {
					return new Integer(c1.getHour()).compareTo(c2.getHour());
				}
			});
		}
		return countSimComp;
	}
	
	private void writeKML(Network network, List<CountSimComparison> countSimComp, String outfile, String srs){
		CountSimComparisonKMLWriter kmlWriter = new CountSimComparisonKMLWriter(
				countSimComp, network, TransformationFactory.getCoordinateTransformation(srs, TransformationFactory.WGS84));
		kmlWriter.writeFile(outfile);
	}
	
	private void writeErrorTable(List<CountSimComparison> countSimComp, String outfile){
		ComparisonErrorStatsCalculator errorStats = new ComparisonErrorStatsCalculator(countSimComp);

		double[] hours = new double[24];
		for (int i = 1; i < 25; i++) {
			hours[i-1] = i;
		}
		DoubleArrayTableWriter tableWriter = new DoubleArrayTableWriter();
		tableWriter.addColumn(hours);
		tableWriter.addColumn(errorStats.getMeanRelError());
		tableWriter.writeFile(outfile + "errortable.txt");
	}
	
	private VolumesAnalyzer loadVolumes(Network network , String eventsFile){
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600, network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(va);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		return va;
	}
	
	public void runAnalysis(String networkFile, String eventsFile1, String eventsFile2, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		VolumesAnalyzer va1 = loadVolumes(network, eventsFile1);
		VolumesAnalyzer va2 = loadVolumes(network, eventsFile2);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		Map<Id, List<CountSimComparison>> countSimCompMap = this.createCountSimComparison(filteredNetwork, va1, va2);
		
		new CountsShapefileWriter(filteredNetwork, networkSrs).writeShape(outfile, countSimCompMap);

		List<CountSimComparison> countSimComp = new ArrayList<CountSimComparison>();
		for (List<CountSimComparison> list : countSimCompMap.values()){
			countSimComp.addAll(list);
		}
		
//		this.writeKML(filteredNetwork, countSimComp, outfile, srs);

		this.writeErrorTable(countSimComp, outfile);


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
		String linkstats1 = null;
		String linkstats2 = null;
		String outfile = null;
		String srs = null;

		if (args == null || args.length == 0){
//			net = "/media/data/work/repos/runs-svn/run1216/1216.output_network.xml.gz";
//			linkstats1 = "/media/data/work/repos/runs-svn/run1216/ITERS/it.500/1216.500.linkstats.txt.gz";
//			linkstats2 = "/media/data/work/repos/runs-svn/run1217/ITERS/it.500/1217.500.linkstats.txt.gz";
//			outfile = "/media/data/work/repos/runs-svn/run1217/ITERS/it.500/1216.500vs1217.500";

			net = "/media/data/work/repos/runs-svn/run1733/1733.output_network.xml.gz";
			linkstats1 = "/media/data/work/repos/runs-svn/run1712/ITERS/it.1000/1712.1000.events.xml.gz";
			linkstats2 = "/media/data/work/repos/runs-svn/run1733/ITERS/it.1000/1733.1000.events.xml.gz";
			outfile = "/media/data/work/repos/runs-svn/run1733/ITERS/it.1000/1712.1000vs1733.1000";
			
			srs = TransformationFactory.WGS84_UTM33N;
			
			
		}
		else {
			net = args[0];
			linkstats1 = args[1];
			linkstats2 = args[2];
			outfile = args[3];
			srs = args[4];
		}

		String kreisShape = "/media/data/work/repos/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";

		SimSimTrafficAnalyser analyser = new SimSimTrafficAnalyser();
		analyser.runAnalysis(net, linkstats1, linkstats2, srs, outfile);
	}

}
