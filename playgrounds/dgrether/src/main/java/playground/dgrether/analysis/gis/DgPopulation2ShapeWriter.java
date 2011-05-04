/* *********************************************************************** *
 * project: org.matsim.*
 * DgPopulation2ShapeWriter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.gis;

import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.FactoryConfigurationError;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeBuilder;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;


/**
 * @author dgrether
 *
 */
public class DgPopulation2ShapeWriter {

	
	private Population pop;
	private CoordinateReferenceSystem popCrs;


	public DgPopulation2ShapeWriter(Population pop, CoordinateReferenceSystem crs){
		this.pop = pop;
		this.popCrs = crs;
	}
	
	public void write(String filename, CoordinateReferenceSystem targetCrs){
		try {
			MathTransform transformation = CRS.findMathTransform(this.popCrs, targetCrs, true);
			FeatureType actFeatureType = this.createActFeatureType(targetCrs);
			List<Feature> features = new ArrayList<Feature>();
			Feature f = null;
			for (Person p : this.pop.getPersons().values()){
				Plan plan = p.getSelectedPlan();
				Activity homeActivity = (Activity) plan.getPlanElements().get(0);
				f = this.
				getActivityFeature(actFeatureType, homeActivity, p.getId(), transformation);
				features.add(f);
			}
			
			ShapeFileWriter.writeGeometries(features, filename);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	
	}
	
	private Feature getActivityFeature(FeatureType actFeatureType, Activity activity, Id personId, 
			MathTransform transformation) throws IllegalAttributeException, TransformException{
		String id = personId.toString();
		String type = activity.getType();
		Double startTime = activity.getStartTime();
		Double endTime = activity.getEndTime();

		Coordinate actCoordinate = MGC.coord2Coordinate(activity.getCoord());
		actCoordinate = JTS.transform(actCoordinate, actCoordinate, transformation);
		Point point = MGC.geoFac.createPoint(actCoordinate);
		
		return actFeatureType.create(new Object[] {point, id, type, startTime, endTime});
	}
	
	private FeatureType createActFeatureType(CoordinateReferenceSystem crs) throws FactoryConfigurationError, SchemaException{
		AttributeType[] attrAct = new AttributeType[5];
		attrAct[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, crs);
		attrAct[1] = AttributeTypeFactory.newAttributeType("person_id", String.class);
		attrAct[2] = AttributeTypeFactory.newAttributeType("activity_type", String.class);
		attrAct[3] = AttributeTypeFactory.newAttributeType("start_time", Double.class);
		attrAct[4] = AttributeTypeFactory.newAttributeType("end_time", Double.class);
		FeatureType featureTypeAct = FeatureTypeBuilder.newFeatureType(attrAct, "activity");
		return featureTypeAct;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

}
