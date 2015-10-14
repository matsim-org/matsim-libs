/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;
import playground.anhorni.choiceSetGeneration.helper.ChoiceSetFacility;

import com.vividsolutions.jts.geom.Point;


public class CSShapeFileWriter extends CSWriter {

	private final static Logger log = Logger.getLogger(CSShapeFileWriter.class);

	private SimpleFeatureBuilder featureBuilder;
	
	public CSShapeFileWriter() {	
	}

		
	@Override
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		this.writeTrips(outdir, name, choiceSets);
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn("No trip shape files created");
			return;
		}				
		this.writeChoiceSets(outdir, name, choiceSets);		
	}
		
	public void writeChoiceSets(String outdir, String name, List<ChoiceSet> choiceSets) {
				
		this.initGeometries();
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();	
		
		Iterator<ChoiceSet> choiceSet_it = choiceSets.iterator();
		while (choiceSet_it.hasNext()) {
			ChoiceSet choiceSet = choiceSet_it.next();
			
			ArrayList<SimpleFeature> singleFeatures = new ArrayList<SimpleFeature>();
			Iterator<ChoiceSetFacility> choiceSetFacilities_it = choiceSet.getFacilities().values().iterator();
			while (choiceSetFacilities_it.hasNext()) {
				ChoiceSetFacility choiceSetFacility = choiceSetFacilities_it.next();
				Coord coord = new Coord(choiceSetFacility.getFacility().getMappedPosition().getX(), choiceSetFacility.getFacility().getMappedPosition().getY());
				
				SimpleFeature feature = this.createFeature(coord, choiceSet.getId());
				features.add(feature);
				singleFeatures.add(feature);
			}
			if (!singleFeatures.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)singleFeatures, outdir +"/shapefiles/singlechoicesets/" + 
					name + choiceSet.getId()+ "_choiceSet.shp");
			}
			else {
				log.error("Empty choice set: " + choiceSet.getId());
			}
			
		}
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)features, outdir +"/shapefiles/" + name + "_choiceSets.shp");
		}
	}
	
	private void writeTrips(String outdir, String name, List<ChoiceSet> choiceSets) {
	
		this.initGeometries();
		ArrayList<SimpleFeature> featuresBefore = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresShop = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> featuresAfter = new ArrayList<SimpleFeature>();
		
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
			
			ArrayList<SimpleFeature> singleFeatures = new ArrayList<SimpleFeature>();

			Coord coordBefore = new Coord(choiceSet.getTrip().getBeforeShoppingAct().getCoord().getX(), choiceSet.getTrip().getBeforeShoppingAct().getCoord().getY());
			
			SimpleFeature featureBefore = this.createFeature(coordBefore, choiceSet.getId());
			featuresBefore.add(featureBefore);
			singleFeatures.add(featureBefore);

			Coord coordShopping = new Coord(choiceSet.getTrip().getShoppingAct().getCoord().getX(), choiceSet.getTrip().getShoppingAct().getCoord().getY());
			
			SimpleFeature featureShopping = this.createFeature(coordShopping, choiceSet.getId());
			featuresShop.add(featureShopping);
			singleFeatures.add(featureShopping);

			Coord coordAfter = new Coord(choiceSet.getTrip().getAfterShoppingAct().getCoord().getX(), choiceSet.getTrip().getAfterShoppingAct().getCoord().getY());
			
			SimpleFeature featureAfter = this.createFeature(coordAfter, choiceSet.getId());
			featuresAfter.add(featureAfter);
			singleFeatures.add(featureAfter);
			
			if (!singleFeatures.isEmpty()) {
				ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)singleFeatures, outdir +"/shapefiles/singletrips/" + name + 
					choiceSet.getId()+"_Trip.shp");
			}
			else {
				log.error("Empty trip : " + choiceSet.getId());
			}		
		}			
		if (!featuresBefore.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)featuresBefore, outdir +"/shapefiles/" + name + "_TripPriorLocations.shp");
		}
		if (!featuresShop.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)featuresShop, outdir +"/shapefiles/" + name + "_TripShopLocations.shp");
		}
		if (!featuresAfter.isEmpty()) {
			ShapeFileWriter.writeGeometries((Collection<SimpleFeature>)featuresAfter, outdir +"/shapefiles/" + name + "_TripPosteriorLocations.shp");
		}
	}
	
	private void initGeometries() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		b.add("location", Point.class);
		b.add("ID", String.class);
		this.featureBuilder = new SimpleFeatureBuilder(b.buildFeatureType());
	}
	
	private SimpleFeature createFeature(Coord coord, Id id) {
		return this.featureBuilder.buildFeature(id.toString(), new Object [] {MGC.coord2Point(coord), id.toString()});
	}
	
	
	/*
	private List<Geometry> createGeometryCollection(ChoiceSet choiceSet, GeometryFactory geometryFactory) {
				
		List<Geometry> geometryList = new Vector<Geometry>();
								
		Coordinate coord = new Coordinate(choiceSet.getTrip().getBeforeShoppingAct().getCoord().getX(), 
				choiceSet.getTrip().getBeforeShoppingAct().getCoord().getY());
		Point point = geometryFactory.createPoint(coord);
		geometryList.add(point);
				
		Coordinate coordShopping = new Coordinate(choiceSet.getTrip().getShoppingAct().getCoord().getX(), 
				choiceSet.getTrip().getShoppingAct().getCoord().getY());
		Point pointShopping = geometryFactory.createPoint(coordShopping);
		geometryList.add(pointShopping);
		
		return geometryList;
	}
	
	
	public void write(String outdir, String name, List<ChoiceSet> choiceSets)  {
		
		if (!super.checkBeforeWriting(choiceSets)) {
			log.warn("No trip shape files created");
			return;
		}
		
		List<Geometry> geometryListAll = new Vector<Geometry>();
		GeometryFactory geometryFactory = new GeometryFactory();
		
		RandomAccessFile shpAll = null;
		RandomAccessFile shxAll = null;
		try {
			shpAll = new RandomAccessFile(
					new File(outdir + "trip.shp"), "rw");
			shxAll = new RandomAccessFile(
					new File(outdir + "trip.shx"), "rw"); 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
				
		Iterator<ChoiceSet> choiceSets_it = choiceSets.iterator();
		while (choiceSets_it.hasNext()) {
			ChoiceSet choiceSet = choiceSets_it.next();
							
			RandomAccessFile shp = null;
			RandomAccessFile shx = null;
			try {
				shp = new RandomAccessFile(
						new File(outdir + "/persontrips/" + "person" + choiceSet.getDriverId()+".trip.shp"), "rw");
				shx = new RandomAccessFile(
						new File(outdir + "/persontrips/" + "person" + choiceSet.getDriverId()+".trip.shx"), "rw");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} 
		
			List<Geometry> geometryList = this.createGeometryCollection(choiceSet, geometryFactory);
			geometryListAll.addAll(geometryList);			
			
			GeometryCollection geometryCollection = new GeometryCollection(
					geometryList.toArray(new Geometry[geometryList.size()]), geometryFactory);
			
			ShapefileWriter writer;
			try {
				writer = new ShapefileWriter(shp.getChannel(),shx.getChannel(), new Lock());
				writer.write(geometryCollection, ShapeType.POINT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		GeometryCollection geometryCollectionAll = new GeometryCollection(
				geometryListAll.toArray(new Geometry[geometryListAll.size()]), geometryFactory);
		
		ShapefileWriter allWriter;
		try {
			allWriter = new ShapefileWriter(shpAll.getChannel(),shxAll.getChannel(), new Lock());
			allWriter.write(geometryCollectionAll, ShapeType.POINT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	*/

}
