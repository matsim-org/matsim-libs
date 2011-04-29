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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.counts.ComparisonErrorStatsCalculator;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.CountSimComparisonImpl;
import org.matsim.counts.algorithms.CountSimComparisonKMLWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.cottbus.CottbusUtils;
import playground.dgrether.utils.DoubleArrayTableWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;


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
		FeatureType featureType = createFeatureType(netCrs);
		GeometryFactory geofac = new GeometryFactory();
		Collection<Feature> features = new ArrayList<Feature>();
		for (Link link : net.getLinks().values()) {
			features.add(this.createFeature(link, geofac, featureType, countSimCompMap.get(link.getId())));
		}
		try {
			ShapeFileWriter.writeGeometries(features, outfile + ".shp");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Feature createFeature(Link link, GeometryFactory geofac, FeatureType featureType, List<CountSimComparison> countSimComparisonList) {
		LineString ls = geofac.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
				MGC.coord2Coordinate(link.getToNode().getCoord())});
		
		Object [] attribs = new Object[35];
		attribs[0] = ls;
		attribs[1] = link.getId().toString();
		attribs[2] = link.getFromNode().getId().toString();
		attribs[3] = link.getToNode().getId().toString();
		attribs[4] = link.getLength();
		attribs[5] = link.getFreespeed();
		attribs[6] = link.getCapacity();
		attribs[7] = link.getNumberOfLanes();
		attribs[8] = link.getNumberOfLanes();
		attribs[9] = ((LinkImpl) link).getType();
		int i = 10;
		double sumRelativeError = 0.0;
		double relativeError = 0.0;
		for (CountSimComparison csc : countSimComparisonList){
			if (csc.getHour() != i - 9) throw new IllegalStateException("List not sorted correclty");
			relativeError = csc.calculateRelativeError();
			attribs[i] = relativeError;
			sumRelativeError += relativeError;
			i++;
		}
		attribs[34] = sumRelativeError / 24.0;
		try {
			return featureType.create(attribs);
		} catch (IllegalAttributeException e) {
			throw new RuntimeException(e);
		}
	}

	
	private FeatureType createFeatureType(CoordinateReferenceSystem crs) {
		FeatureType featureType = null;
		AttributeType [] attribs = new AttributeType[35];
		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("LineString",LineString.class, true, null, null, crs);
		attribs[1] = AttributeTypeFactory.newAttributeType("ID", String.class);
		attribs[2] = AttributeTypeFactory.newAttributeType("fromID", String.class);
		attribs[3] = AttributeTypeFactory.newAttributeType("toID", String.class);
		attribs[4] = AttributeTypeFactory.newAttributeType("length", Double.class);
		attribs[5] = AttributeTypeFactory.newAttributeType("freespeed", Double.class);
		attribs[6] = AttributeTypeFactory.newAttributeType("capacity", Double.class);
		attribs[7] = AttributeTypeFactory.newAttributeType("lanes", Double.class);
		attribs[8] = AttributeTypeFactory.newAttributeType("visWidth", Double.class);
		attribs[9] = AttributeTypeFactory.newAttributeType("type", String.class);
		for (int i = 0; i < 24; i++){
			attribs[10 + i] = AttributeTypeFactory.newAttributeType("re h " + (i + 1), Double.class);
		}
		attribs[34] = AttributeTypeFactory.newAttributeType("re 24h", Double.class);
		try {
			featureType = FeatureTypeBuilder.newFeatureType(attribs, "link");
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		}
		return featureType;
	}


	private Network applyNetworkFilter(Network network, CoordinateReferenceSystem networkSrs) {
		log.info("Filtering network...");
		log.info("Nr links in original network: " + network.getLinks().size());
		NetworkFilterManager netFilter = new NetworkFilterManager(network);
		Tuple<CoordinateReferenceSystem, Feature> cottbusFeatureTuple = CottbusUtils.loadCottbusFeature("/media/data/work/repos/shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp");
		FeatureNetworkLinkFilter filter = new FeatureNetworkLinkFilter(networkSrs, cottbusFeatureTuple.getSecond(), cottbusFeatureTuple.getFirst());
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
//			net = DgPaths.IVTCHNET;
			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.100/100.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.100/100.linkstats.txt.gz";
			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.200/200.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.200/200.linkstats.txt.gz";
			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.500/500.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.500/500.linkstats.txt.gz";
			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run610/it.550/550.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run612/it.550/550.linkstats.txt.gz";
			//		String outfile = DgPaths.VSPCVSBASE + "runs/run612/traffic612vs610.550";
			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run612/it.500/500.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run620/it.500/500.linkstats.txt.gz";
			//		String outfile = DgPaths.VSPCVSBASE + "runs/run620/traffic612vs620.500";

			//		String linkstats1 = DgPaths.VSPCVSBASE + "runs/run465/it.500/500.linkstats.txt.gz";
			//		String linkstats2 = DgPaths.VSPCVSBASE + "runs/run568/it.500/500.linkstats.txt.gz";
			//		String outfile = DgPaths.VSPCVSBASE + "runs/run568/traffic465vs568.500";
//			linkstats1 = DgPaths.RUNBASE + "run709/it.1000/1000.linkstats.txt.gz";
//			linkstats2 = DgPaths.RUNBASE + "run710/it.1000/1000.linkstats.txt.gz";
//			outfile = DgPaths.RUNBASE + "run710/traffic709vs710.500";
//			srs = TransformationFactory.CH1903_LV03;

			net = "/media/data/work/repos/runs-svn/run1216/1216.output_network.xml.gz";
			linkstats1 = "/media/data/work/repos/runs-svn/run1216/ITERS/it.500/1216.500.linkstats.txt.gz";
			linkstats2 = "/media/data/work/repos/runs-svn/run1217/ITERS/it.500/1217.500.linkstats.txt.gz";
			outfile = "/media/data/work/repos/runs-svn/run1217/ITERS/it.500/1216.500vs1217.500";

//			net = "/media/data/work/repos/runs-svn/run1217/1217.output_network.xml.gz";
//			linkstats1 = "/media/data/work/repos/runs-svn/run1217/ITERS/it.500/1217.500.linkstats.txt.gz";
//			linkstats2 = "/media/data/work/repos/runs-svn/run1218/ITERS/it.500/1218.500.linkstats.txt.gz";
//			outfile = "/media/data/work/repos/runs-svn/run1218/ITERS/it.500/1217.500vs1218.500";
			
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
