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
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.io.DgAnalysisPopulationReader;
import playground.dgrether.analysis.io.DgHouseholdsAnalysisReader;
import playground.dgrether.utils.DgGrid;

import com.vividsolutions.jts.geom.Polygon;


public class DgIncomeDistributionAnalysis {

	private static final Logger log = Logger.getLogger(DgIncomeDistributionAnalysis.class);
	
	public static void main(String[] args) throws FactoryException, FactoryRegistryException, SchemaException, IllegalArgumentException, IOException{
		//run number creation
		String runNumber1 = "749";
		String runNumber2 = "869";
		// scenario files
		String netfile = DgPaths.RUNBASE + "run" +  runNumber1 + "/" + runNumber1 + "." + "output_network.xml.gz";
		String plans1file = DgPaths.RUNBASE + "run" +runNumber1 + "/" + runNumber1 + "."  + "output_plans.xml.gz";
		String plans2file = DgPaths.RUNBASE + "run" +runNumber2 + "/" + runNumber2 + "."  + "output_plans.xml.gz";
		//		String housholdsfile = DgPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/households.xml";
		String housholdsfile = DgPaths.STUDIESDG + "einkommenSchweiz/households_all_zrh30km_transitincl_10pct.xml.gz";
		String gridFile = DgPaths.RUNBASE + "run" + runNumber2 + "/" + runNumber1 + "vs" + runNumber2+ "grid450x375.shp";
		String singlePersonsFile = DgPaths.RUNBASE + "run" + runNumber2 + "/" + runNumber1 + "vs" + runNumber2+ "singlePersonsFrom100000.shp";
		//file io
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		DgAnalysisPopulation pop = new DgAnalysisPopulation();
		DgAnalysisPopulationReader pc = new DgAnalysisPopulationReader();
		pc.addFilter(new ExcludeZurichTransitFilter());
		pc.readAnalysisPopulation(pop, runNumber1, netfile, plans1file);
		pc.readAnalysisPopulation(pop, runNumber2, netfile, plans2file);
		//households io
		DgHouseholdsAnalysisReader hhr = new DgHouseholdsAnalysisReader(pop);
		hhr.readHousholds(housholdsfile);
		
//		writeGrid(pop, gridFile, runid1, runid2);
		writePersons(pop, singlePersonsFile, runNumber1, runNumber2);
		log.info("ya esta");
	}

	private static void writePersons(DgAnalysisPopulation pop, String file, String runid1, String runid2) throws SchemaException, IllegalArgumentException, IOException {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
		PointFeatureFactory factory = new PointFeatureFactory.Builder().
				setCrs(targetCRS).
				setName("geometry").
				addAttribute("Income", Double.class).
				addAttribute("Delta Utility", Double.class).
				create();
		//create features 
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
	  
	  for (DgPersonData pd : pop.getPersonData().values()){
	  	if (pd.getIncome().getIncome() >= 100000) {
	  		SimpleFeature feature = factory.createPoint(pd.getFirstActivity().getCoord(), new Object[] { pd.getIncome().getIncome(), pd.getDeltaScore(runid1, runid2) }, null);
	  		features.add(feature);
	  	}
	  }
		//write shape file
		ShapeFileWriter.writeGeometries(features, file);
	}

	
	
	private static void writeGrid(DgAnalysisPopulation pop, String file, String runid1, String runid2) throws SchemaException, IllegalArgumentException, IOException {
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
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		Iterator<Polygon> pi = grid.iterator();
	  CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.CH1903_LV03_GT);
	  
	  SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
	  b.setCRS(targetCRS);
	  b.add("location", Polygon.class);
	  b.add("avgIncome", Double.class);
	  b.add("deltaScore", Double.class);
	  b.add("persons", Integer.class);
	  SimpleFeatureBuilder builder = new SimpleFeatureBuilder(b.buildFeatureType());

		while (pi.hasNext()){
		  Polygon p = pi.next();
		  QuadTree.Rect rect = new QuadTree.Rect(p.getEnvelopeInternal().getMinX(), p.getEnvelopeInternal().getMinY(), 
		  																																p.getEnvelopeInternal().getMaxX(), p.getEnvelopeInternal().getMaxY());
		  List<DgPersonData> results = (List<DgPersonData>) quadTree.getRectangle(rect, new ArrayList<DgPersonData>());
		
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
		  	SimpleFeature feature = builder.buildFeature(null, new Object[] {avgIncome, avgDeltaUtility, numberOfPersons});
		  	//add to collection
		  	features.add(feature);
		  }
		}
		//write shape file
		ShapeFileWriter.writeGeometries(features, file);
	}
	
}
