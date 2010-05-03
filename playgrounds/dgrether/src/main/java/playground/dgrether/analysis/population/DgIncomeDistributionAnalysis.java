/* *********************************************************************** *
 * project: org.matsim.*
 * DgIncomeDistributionAnalysis
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
package playground.dgrether.analysis.population;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.factory.FactoryConfigurationError;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.DgGrid;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class DgIncomeDistributionAnalysis {

	private static final Logger log = Logger.getLogger(DgIncomeDistributionAnalysis.class);
	
	public static void main(String[] args) throws FactoryException, FactoryRegistryException, SchemaException, IllegalAttributeException, IOException{
		//run number creation
		String runNumber1 = "749";
		String runNumber2 = "869";
		Id runid1 = new IdImpl(runNumber1);
		Id runid2 = new IdImpl(runNumber2);
		// scenario files
		String netfile = DgPaths.RUNBASE + "run" +  runid1.toString() + "/" + runid1 + "." + "output_network.xml.gz";
		String plans1file = DgPaths.RUNBASE + "run" +runid1.toString() + "/" + runid1 + "."  + "output_plans.xml.gz";
		String plans2file = DgPaths.RUNBASE + "run" +runid2.toString() + "/" + runid2 + "."  + "output_plans.xml.gz";
		//		String housholdsfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
		String housholdsfile = DgPaths.STUDIESDG + "einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
		String gridFile = DgPaths.RUNBASE + "run" + runid2.toString() + "/" + runid1.toString() + "vs" + runid2.toString()+ "grid450x375.shp";
		String singlePersonsFile = DgPaths.RUNBASE + "run" + runid2.toString() + "/" + runid1.toString() + "vs" + runid2.toString()+ "singlePersonsFrom100000.shp";
		//file io
		ScenarioImpl sc = new ScenarioImpl();
		DgAnalysisPopulation pop = new DgAnalysisPopulation();
		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader();
		pc.setExcludeTransit(true);
		pc.readAnalysisPopulation(pop, runid1, netfile, plans1file);
		pc.readAnalysisPopulation(pop, runid2, netfile, plans2file);
		//households io
		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(pop);
		hhr.readHousholds(housholdsfile);
		
//		writeGrid(pop, gridFile, runid1, runid2);
		writePersons(pop, singlePersonsFile, runid1, runid2);
		log.info("ya esta");
	}

	private static void writePersons(DgAnalysisPopulation pop, String file, Id runid1, Id runid2) throws FactoryConfigurationError, SchemaException, IllegalAttributeException, IOException {
		//create features 
		List<Feature> features = new ArrayList<Feature>();
	  CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
	  AttributeType pointAttribute = DefaultAttributeTypeFactory.newAttributeType("First Activity",Point.class, true, null, null, targetCRS);
	  AttributeType incomeAttribute = DefaultAttributeTypeFactory.newAttributeType("Income", Double.class);
	  AttributeType deltaScoreAttribute = DefaultAttributeTypeFactory.newAttributeType("Delta Utility", Double.class);
//	  AttributeType modeSwitch = DefaultAttributeTypeFactory.newAttributeType("Mode Switch", String.class);
	  FeatureType ftPolygon = FeatureTypeBuilder.newFeatureType(new AttributeType[] {pointAttribute, incomeAttribute, deltaScoreAttribute, /*modeSwitch*/}, "geometry");
	  GeometryFactory geofac = new GeometryFactory();
	  
	  for (DgPersonData pd : pop.getPersonData().values()){
	  	if (pd.getIncome().getIncome() >= 100000) {
	  		Coordinate coord = new Coordinate(pd.getFirstActivity().getCoord().getX(), pd.getFirstActivity().getCoord().getY());
	  		Point point = geofac.createPoint(coord);
//	  	String mode = pd.getPlanData().get(runid1).getPlan().getType() + "->" + pd.getPlanData().get(runid2).getPlan().getType();
	  		Feature feature = ftPolygon.create(new Object[]{point, pd.getIncome().getIncome(), pd.getDeltaScore(runid1, runid2), /*mode*/});
	  		//add to collection
	  		features.add(feature);
	  	}
	  }
		//write shape file
		ShapeFileWriter.writeGeometries(features, file);
		
	}

	
	
	private static void writeGrid(DgAnalysisPopulation pop, String file, Id runid1, Id runid2) throws FactoryConfigurationError, SchemaException, IllegalAttributeException, IOException {
		//create grid
		DgGrid grid = new DgGrid(450, 375, pop.getBoundingBox());
		//fill quad tree
		QuadTree<DgPersonData> quadTree = new QuadTree<DgPersonData>(pop.getBoundingBox().getMinX(),
																														pop.getBoundingBox().getMinY(), 
																														pop.getBoundingBox().getMaxX(),
																														pop.getBoundingBox().getMaxY());
		
		for (DgPersonData personData : pop.getPersonData().values()){
			quadTree.put(personData.getFirstActivity().getCoord().getX(), 
					personData.getFirstActivity().getCoord().getY(), personData);
		}

		//create features 
		List<Feature> features = new ArrayList<Feature>();
		Iterator<Polygon> pi = grid.iterator();
	  CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
	  AttributeType polygonAttribute = DefaultAttributeTypeFactory.newAttributeType("Polygon",Polygon.class, true, null, null, targetCRS);
	  AttributeType incomeAttribute = DefaultAttributeTypeFactory.newAttributeType("avgIncome", Double.class);
	  AttributeType deltaScoreAttribute = DefaultAttributeTypeFactory.newAttributeType("deltaScore", Double.class);
	  AttributeType numberOfPersonsAttribute = DefaultAttributeTypeFactory.newAttributeType("", Integer.class);
	  FeatureType ftPolygon = FeatureTypeBuilder.newFeatureType(new AttributeType[] {polygonAttribute, incomeAttribute, deltaScoreAttribute, numberOfPersonsAttribute}, "geometry");

		while (pi.hasNext()){
		  Polygon p = pi.next();
		  QuadTree.Rect rect = new QuadTree.Rect(p.getEnvelopeInternal().getMinX(), p.getEnvelopeInternal().getMinY(), 
		  																																p.getEnvelopeInternal().getMaxX(), p.getEnvelopeInternal().getMaxY());
		  List<DgPersonData> results = (List<DgPersonData>) quadTree.get(rect, new ArrayList<DgPersonData>());
		
		  //calc average income
		  double avgIncome = 0.0;
		  double avgDeltaUtility = 0.0;
		  for (DgPersonData pd : results){
		  	avgIncome += pd.getIncome().getIncome();
		  	avgDeltaUtility = pd.getDeltaScore(runid1, runid2);
		  }
		  if (!results.isEmpty()){
		  	int numberOfPersons = results.size();
		  	avgIncome /= numberOfPersons;
		  	avgDeltaUtility /= numberOfPersons;
		  	Feature feature = ftPolygon.create(new Object[]{p, avgIncome, avgDeltaUtility, numberOfPersons});
		  	//add to collection
		  	features.add(feature);
		  }
		}
		//write shape file
		ShapeFileWriter.writeGeometries(features, file);
	}
	
	
	
}
