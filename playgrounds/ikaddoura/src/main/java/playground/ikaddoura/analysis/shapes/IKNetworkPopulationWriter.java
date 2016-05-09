/* *********************************************************************** *
 * project: org.matsim.*
 * HomeLocationFilter.java
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

package playground.ikaddoura.analysis.shapes;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.utils.gis.matsim2esri.network.CapacityBasedWidthCalculator;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.matsim.utils.gis.matsim2esri.network.PolygonFeatureGenerator;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author dhosse, ikaddoura
 *
 */
public class IKNetworkPopulationWriter {
	
	private final static Logger log = Logger.getLogger(IKNetworkPopulationWriter.class);

	private Scenario scenario;
	
	private final String networkFile = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/output_network.xml.gz";
	private final String populationFile = null;
	private final String outputPath = "../../../public-svn/matsim/scenarios/countries/de/cottbus/cottbus-with-pt/output/cb02/gis/";
	
	private final String crs = TransformationFactory.WGS84_UTM33N;
//	private final String crs = TransformationFactory.DHDN_GK4;
//	private final String crs = TransformationFactory.WGS84;
	
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, crs);
//	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);


//	private final String networkFile = "../../shared-svn/studies/ihab/noiseTestScenario/output/output_network.xml.gz";
//	private final String populationFile = "../../shared-svn/studies/ihab/noiseTestScenario/output/output_plans.xml.gz";
//	private final String outputPath = "../../shared-svn/studies/ihab/noiseTestScenario/output/shapeFiles/";

	public static void main(String[] args) {
		
		IKNetworkPopulationWriter main = new IKNetworkPopulationWriter();	
		main.run();		
	}
	
	private void run() {
		
		loadScenario();
		
		File file = new File(outputPath);
		file.mkdirs();
		
		// network
		exportNetwork2Shp1();
		exportNetwork2Shp2();
		
		// population
//		exportActivities2Shp();
		
	}

	private void loadScenario() {
		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);
	}
	
	private void exportNetwork2Shp1(){
		
		if (this.scenario.getNetwork().getLinks().size() == 0) {
			new NetworkReaderMatsimV1(scenario.getNetwork()).parse(this.networkFile);
		}
		
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
		.setCrs(MGC.getCRS(crs))
		.setName("Link")
		.addAttribute("Id", String.class)
		.addAttribute("Length", Double.class)
		.addAttribute("capacity", Double.class)
		.addAttribute("lanes", Double.class)
		.addAttribute("Freespeed", Double.class)
		.addAttribute("Modes", String.class)
		.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
						
		for (Link link : scenario.getNetwork().getLinks().values()){
			if (link.getAllowedModes().contains("car")) {
				SimpleFeature feature = factory.createPolyline(
						new Coordinate[]{
//								new Coordinate(MGC.coord2Coordinate(link.getFromNode().getCoord())),
//								new Coordinate(MGC.coord2Coordinate(link.getToNode().getCoord()))
								new Coordinate(MGC.coord2Coordinate(ct.transform(link.getFromNode().getCoord()))),
								new Coordinate(MGC.coord2Coordinate(ct.transform(link.getToNode().getCoord())))
						}, new Object[] {link.getId(), link.getLength(), link.getCapacity(), link.getNumberOfLanes(), link.getFreespeed(), link.getAllowedModes()
						}, null
				);
				features.add(feature);
			}
		}
		
		log.info("Writing out network lines shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPath + "network.shp");
		log.info("Writing out network lines shapefile... Done.");
	}

	private void exportNetwork2Shp2() {
		
		if (this.scenario.getNetwork().getLinks().size() == 0) {
			new NetworkReaderMatsimV1(scenario.getNetwork()).parse(this.networkFile);
		}

		FeatureGeneratorBuilderImpl builder = new FeatureGeneratorBuilderImpl(scenario.getNetwork(), crs);

		boolean commonWealth = false;
		builder.setWidthCoefficient((commonWealth  ? -1 : 1) * 0.01);
		builder.setFeatureGeneratorPrototype(PolygonFeatureGenerator.class);
		builder.setWidthCalculatorPrototype(CapacityBasedWidthCalculator.class);
		
		new Links2ESRIShape(scenario.getNetwork(), outputPath + "net.shp", builder).write();
	}
	
	private void exportActivities2Shp(){
		
		new PopulationReaderMatsimV5(scenario).readFile(populationFile);
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(MGC.getCRS(crs))
		.setName("Activity")
		.addAttribute("Type", String.class)
		.addAttribute("Person Id", String.class)
		.create();
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (Person p : scenario.getPopulation().getPersons().values()){
			
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){

				if (pe instanceof Activity){
					
					Activity act = (Activity) pe;
					// TODO: Add coordinate transformation?
					SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(act.getCoord()), new Object[] {act.getType(), p.getId().toString()}, null);
					features.add(feature);
				}
			}
		}
		
		log.info("Writing out activity points shapefile... ");
		ShapeFileWriter.writeGeometries(features, outputPath + "activities.shp");
		log.info("Writing out activity points shapefile... Done.");		
	}
}
