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
package playground.dgrether.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.utils.DoubleArrayTableWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


/**
 * @author dgrether
 *
 */
public class SimSimTrafficAnalyser {


	private static final Logger log = Logger.getLogger(SimSimTrafficAnalyser.class);

	private CalcLinkStats loadData(Network network, String linkAttributes) {
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
	
	
	private Map<Id, List<CountSimComparison>> createCountSimComparison(Network network, CalcLinkStats linkstats1, CalcLinkStats linkstats2){
		Map<Id, List<CountSimComparison>> countSimComp = new HashMap<Id, List<CountSimComparison>>(network.getLinks().size());

		for (Link l : network.getLinks().values()) {
			double[] volumes = linkstats1.getAvgLinkVolumes(l.getId());
			double[] volumes2 = linkstats2.getAvgLinkVolumes(l.getId());

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
	
	
	public void runAnalysis(String networkFile, String linkAttributes1, String linkAttributes2, String srs, String outfile) {
		Network network = this.loadNetwork(networkFile);
		CalcLinkStats linkstats1 = loadData(network, linkAttributes1);
		CalcLinkStats linkstats2 = loadData(network, linkAttributes2);
		
		CoordinateReferenceSystem networkSrs = MGC.getCRS(srs);
		
		Network filteredNetwork = this.applyNetworkFilter(network, networkSrs);
		Map<Id, List<CountSimComparison>> countSimCompMap = this.createCountSimComparison(filteredNetwork, linkstats1, linkstats2);
		
		this.writeShape(filteredNetwork, networkSrs, outfile, countSimCompMap);

		List<CountSimComparison> countSimComp = new ArrayList<CountSimComparison>();
		for (List<CountSimComparison> list : countSimCompMap.values()){
			countSimComp.addAll(list);
		}
		
//		this.writeKML(filteredNetwork, countSimComp, outfile, srs);

		this.writeErrorTable(countSimComp, outfile);


	}

	private void writeShape(Network net, CoordinateReferenceSystem netCrs, String outfile, Map<Id, List<CountSimComparison>> countSimCompMap){
		PolylineFeatureFactory factory = createFeatureType(netCrs);
		GeometryFactory geofac = new GeometryFactory();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		for (Link link : net.getLinks().values()) {
			features.add(this.createFeature(link, geofac, factory, countSimCompMap.get(link.getId())));
		}
		ShapeFileWriter.writeGeometries(features, outfile + ".shp");
	}

	private SimpleFeature createFeature(Link link, GeometryFactory geofac, PolylineFeatureFactory factory, List<CountSimComparison> countSimComparisonList) {
		Coordinate[] coords = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())};
		
		Object [] attribs = new Object[59];
		attribs[0] = link.getId().toString();
		attribs[1] = link.getFromNode().getId().toString();
		attribs[2] = link.getToNode().getId().toString();
		attribs[3] = link.getLength();
		attribs[4] = link.getFreespeed();
		attribs[5] = link.getCapacity();
		attribs[6] = link.getNumberOfLanes();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = ((LinkImpl) link).getType();
		int i = 9;
		double sumRelativeError = 0.0;
		double relativeError = 0.0;
		for (CountSimComparison csc : countSimComparisonList) {
			log.error("hour: " + csc.getHour());
		}
		for (CountSimComparison csc : countSimComparisonList){
			if (csc.getHour() != i - 8) throw new IllegalStateException("List not sorted correctly. csc.getHour() returns " +csc.getHour());
			relativeError = csc.calculateRelativeError();
			attribs[i] = relativeError;
			sumRelativeError += relativeError;
			i++;
		}
		attribs[33] = sumRelativeError / 24.0;
		i = 34;
		double difference = 0;
		double sumDifference = 0;
		for (CountSimComparison csc : countSimComparisonList){
			if (csc.getHour() != i - 33) throw new IllegalStateException("List not sorted correctly");
			difference = csc.getSimulationValue() - csc.getCountValue();
			attribs[i] = difference;
			sumDifference += difference;
			i++;
		}
		attribs[58] = sumDifference;
		return factory.createPolyline(coords, attribs, link.getId().toString());
	}

	
	private PolylineFeatureFactory createFeatureType(CoordinateReferenceSystem crs) {
		PolylineFeatureFactory.Builder builder = new PolylineFeatureFactory.Builder();
		builder.setCrs(crs);
		builder.setName("link");
		builder.addAttribute("ID", String.class);
		builder.addAttribute("fromID", String.class);
		builder.addAttribute("toID", String.class);
		builder.addAttribute("length", Double.class);
		builder.addAttribute("freespeed", Double.class);
		builder.addAttribute("capacity", Double.class);
		builder.addAttribute("lanes", Double.class);
		builder.addAttribute("visWidth", Double.class);
		builder.addAttribute("type", String.class);
		for (int i = 0; i < 24; i++){
			builder.addAttribute("re h " + (i + 1), Double.class);
		}
		builder.addAttribute("re 24h", Double.class);
		for (int i = 0; i < 24; i++){
			builder.addAttribute("abdif_h " + (i + 1), Double.class);
		}
		builder.addAttribute("absdif24h", Double.class);
		return builder.create();
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
			linkstats1 = "/media/data/work/repos/runs-svn/run1712/ITERS/it.1000/1712.1000.linkstats.txt.gz";
			linkstats2 = "/media/data/work/repos/runs-svn/run1733/ITERS/it.1000/1733.1000.linkstats.txt.gz";
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
