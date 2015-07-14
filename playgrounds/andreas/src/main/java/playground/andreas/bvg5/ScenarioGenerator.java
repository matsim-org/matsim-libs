/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.andreas.bvg5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.BVGLines2PtModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.VehicleReaderV1;
import org.opengis.feature.simple.SimpleFeature;

import playground.andreas.utils.pop.FilterPopulationByShape;
import playground.andreas.utils.pt.TransitLineRemover;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

/**
 * 
 * @author aneumann
 *
 */
public class ScenarioGenerator {

	private static final Logger log = Logger.getLogger(ScenarioGenerator.class);
	
	private ScenarioImpl baseScenario;
	private String outputDir;


	
	public ScenarioGenerator(String netFile, String scheduleFile, String vehiclesFile, String outputDir) {
		log.info("Network: " + netFile);
		log.info("Schedule: " + scheduleFile);
		log.info("Vehicles: " + vehiclesFile);
		log.info("OutputDir: " + outputDir);
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
		this.baseScenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(this.baseScenario).readFile(netFile);
		new TransitScheduleReaderV1(this.baseScenario).readFile(scheduleFile);
		new VehicleReaderV1(this.baseScenario.getTransitVehicles()).readFile(vehiclesFile);
		this.outputDir = outputDir;
	}

	public static void main(String[] args) {
//		String outputdir = "e:/_shared-svn/andreas/paratransit/b285/run/output/";
		String outputdir = "e:/_shared-svn/_projects/bvg_6_paratransit_scaling/berlin/run/input/";
		String targetCoordinateSystem = TransformationFactory.WGS84_UTM33N; // Berlin
		double distance = 100.0;
		
//		String netFile = "e:/_shared-svn/andreas/paratransit/b285/run/input/network.final.xml.gz";
//		String scheduleFile = "e:/_shared-svn/andreas/paratransit/b285/common_ana/baseSchedule/transitSchedule_basecase.xml.gz";
//		String vehiclesFile = "e:/_shared-svn/andreas/paratransit/b285/run/input/transitVehicles100.final.xml.gz";

		String netFile = "d:/Berlin/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz";
		String popInFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.selected.xml.gz";
		String eventsFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.events_enriched.xml.gz";
		String scheduleFile = "d:/Berlin/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitSchedule.xml.gz";
		String vehiclesFile = "d:/Berlin/berlin_bvg3/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitVehicles.final.xml.gz";
		
//		String popInFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.plans.selected.xml.gz";
//		String eventsFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run192.100pct/ITERS/it.100/bvg.run192.100pct.100.events_enriched.xml.gz";
		
//		String popInFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run190.25pct/ITERS/it.100/bvg.run190.25pct.100.plans.selected.xml.gz";
//		String eventsFile = "d:/Berlin/berlin_bvg3/berlin-bvg09_runs/bvg.run190.25pct/ITERS/it.100/bvg.run190.25pct.100.events_enriched.xml.gz";
		
		String scenarioAreaFilename = "e:/_shared-svn/_projects/bvg_6_paratransit_scaling/berlin/run/input/scenarioArea.shp";
//		String scenarioAreaFilename = "e:/_shared-svn/andreas/paratransit/b285/run/input/scenarioArea.shp";
//		String scenarioAreaFilename = "e:/_shared-svn/andreas/paratransit/txl/run/output_medium/scenarioArea.shp";
//		String scenarioAreaFilename = "e:/_shared-svn/andreas/paratransit/txl/run/output_huge/scenarioArea.shp";
		
		ScenarioGenerator sG = new ScenarioGenerator(netFile, scheduleFile, vehiclesFile, outputdir);
		
		Set<Id<TransitLine>> linesToSimulatePara = new TreeSet<>();
		// all bvg_bus
		for (TransitLine transitLine : sG.baseScenario.getTransitSchedule().getTransitLines().values()) {
			if(transitLine.getId().toString().contains("-B-")) {
				linesToSimulatePara.add(transitLine.getId());
			}
		}
		
		// B285
//		linesToSimulatePara.add(Id.create("285-B-285", TransitLine.class));

		// 100/200
//		linesToSimulatePara.add(new IdImpl("100-B-100"));
//		linesToSimulatePara.add(new IdImpl("200-B-200"));

		// TXL
//		linesToSimulatePara.add(new IdImpl("TXL-B-500"));
//		linesToSimulatePara.add(new IdImpl("X9-B-509"));
//		linesToSimulatePara.add(new IdImpl("109-B-109"));
//		linesToSimulatePara.add(new IdImpl("128-B-128"));
		
		TransitSchedule paraSchedule = sG.createParaSchedule(linesToSimulatePara, targetCoordinateSystem);
//		String scenarioAreaFilename = sG.createServiceArea(paraSchedule, targetCoordinateSystem, distance);
		sG.cutTransitSchedule(scenarioAreaFilename);

		sG = null;
		new FilterPopulationByShape(netFile, popInFile, eventsFile, scenarioAreaFilename, outputdir + "scenarioPopulation.xml.gz");
	}

	private void cutTransitSchedule(String scenarioAreaFilename) {
		BVGLines2PtModes lines2Modes = new BVGLines2PtModes();
		lines2Modes.setPtModesForEachLine(this.baseScenario.getTransitSchedule(), "none");
		TreeSet<String> modes2Cut = new TreeSet<String>();
		modes2Cut.add("bvg_bus");
		TransitScheduleAreaCut2 cutter = new TransitScheduleAreaCut2(this.baseScenario.getTransitSchedule(), scenarioAreaFilename, lines2Modes, modes2Cut, this.baseScenario.getTransitVehicles());
		cutter.run(this.outputDir);		
	}
	
	private String createServiceArea(TransitSchedule paraSchedule, String targetCoordinateSystem, double distance) {
		log.info("Creating operation area and scenario area with buffer size: " + distance);
		log.info("Target coord system: " + targetCoordinateSystem);
		log.info("Lines in para schedule: " + paraSchedule.getTransitLines().keySet());
		
		// get all link ids
		Set<Id<Link>> linksToConvert = new TreeSet<>();
		for (TransitLine line : paraSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				linksToConvert.add(route.getRoute().getStartLinkId());
				linksToConvert.addAll(route.getRoute().getLinkIds());
				linksToConvert.add(route.getRoute().getEndLinkId());
			}
		}
		
		// create line Strings for all route links
		SimpleFeatureTypeBuilder featureBuilder = new SimpleFeatureTypeBuilder();
		featureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		featureBuilder.setName("lineStringFeature");
		featureBuilder.add("LineString", LineString.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureBuilder.buildFeatureType());
		Collection<SimpleFeature> lineStringFeatures = new ArrayList<SimpleFeature>();
		
		List<Geometry> polygons = new ArrayList<Geometry>();
		
		for(Id<Link> linkId: linksToConvert){
			List<Coordinate> coords = new ArrayList<Coordinate>();
			
			Coord fromNode = this.baseScenario.getNetwork().getLinks().get(linkId).getFromNode().getCoord();
			coords.add(new Coordinate(fromNode.getX(), fromNode.getY(), 0.));
			
			Coord toNode = this.baseScenario.getNetwork().getLinks().get(linkId).getToNode().getCoord();
			coords.add(new Coordinate(toNode.getX(), toNode.getY(), 0.));

			Coordinate[] coord = new Coordinate[coords.size()];
			coord = coords.toArray(coord);
			Geometry lineString = new GeometryFactory().createLineString(new CoordinateArraySequence(coord));
			
			// create line string feature
			Object[] featureAttribs =  new Object[1];
			featureAttribs[0] = lineString;
			try {
				lineStringFeatures.add(builder.buildFeature(linkId.toString(), featureAttribs));
			} catch (IllegalArgumentException e1) {
				e1.printStackTrace();
			}
			
			// create buffer
			Geometry buffer = lineString.buffer(distance);
			polygons.add(buffer);
		}
		
		String filename = this.outputDir + "linksForPara.shp";
		ShapeFileWriter.writeGeometries(lineStringFeatures, filename);
		log.info("Links of original pt line written to " + filename);
		
		// create buffer features
		GeometryCollection polygonCollection = new GeometryFactory().createGeometryCollection(polygons.toArray(new Geometry[polygons.size()]));
		Geometry union = polygonCollection.buffer(0);
		
		featureBuilder = new SimpleFeatureTypeBuilder();
		featureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		featureBuilder.setName("bufferFeature");
		featureBuilder.add("Polygon", Polygon.class);
		builder = new SimpleFeatureBuilder(featureBuilder.buildFeatureType());
		
		Object[] featureAttribs =  new Object[1];
		featureAttribs[0] = union;
		Collection<SimpleFeature> bufferFeatures = new ArrayList<SimpleFeature>();
		try {
			bufferFeatures.add(builder.buildFeature("paraArea", featureAttribs));
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}
		
		filename = this.outputDir + "paraArea.shp";
		ShapeFileWriter.writeGeometries(bufferFeatures, filename);
		log.info("Operation area shape written to " + filename);
		
		// create scenario area
		Geometry scenarioArea = union.buffer(distance);
		featureAttribs =  new Object[1];
		featureAttribs[0] = scenarioArea;
		bufferFeatures = new ArrayList<SimpleFeature>();
		try {
			bufferFeatures.add(builder.buildFeature("scenarioArea", featureAttribs));
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}
		
		filename = this.outputDir + "scenarioArea.shp";
		ShapeFileWriter.writeGeometries(bufferFeatures, filename);
		log.info("Scenario area file written to " + filename);
		log.info("...done");
		return filename;
	}

	private TransitSchedule createParaSchedule(Set<Id<TransitLine>> linesToSimulateAsPara, String targetCoordinateSystem) {
		log.info("Extracting the following lines from schedule: " + linesToSimulateAsPara);
		log.info("Target coord system: " + targetCoordinateSystem);
		TransitSchedule remainingSchedule = TransitLineRemover.removeTransitLinesFromTransitSchedule(this.baseScenario.getTransitSchedule(), linesToSimulateAsPara);
		new TransitScheduleWriterV1(remainingSchedule).write(this.outputDir + "remainingSchedule.xml.gz");

		Set<Id<TransitLine>> linesToRemove = new TreeSet<>();
		for (Id<TransitLine> lineId : remainingSchedule.getTransitLines().keySet()) {
			linesToRemove.add(lineId);
		}
		
		TransitSchedule linesToBeTransformedIntoPara = TransitLineRemover.removeTransitLinesFromTransitSchedule(this.baseScenario.getTransitSchedule(), linesToRemove);
		new TransitScheduleWriterV1(linesToBeTransformedIntoPara).write(this.outputDir + "paraSchedule.xml.gz");
	
		log.info("...done");
		return linesToBeTransformedIntoPara;
	}
}
