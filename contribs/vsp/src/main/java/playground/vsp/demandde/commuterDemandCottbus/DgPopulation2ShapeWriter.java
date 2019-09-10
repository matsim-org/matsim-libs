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
package playground.vsp.demandde.commuterDemandCottbus;

import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


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
	
	public void write(String activityType, String filename, CoordinateReferenceSystem targetCrs){
		try {
			MathTransform transformation = CRS.findMathTransform(this.popCrs, targetCrs, true);

			PointFeatureFactory factory = new PointFeatureFactory.Builder().
					setCrs(targetCrs).
					setName("activity").
					addAttribute("person_id", String.class).
					addAttribute("activity_type", String.class).
					addAttribute("start_time", Double.class).
					addAttribute("end_time", Double.class).
					create();
			
			List<SimpleFeature> features = new ArrayList<SimpleFeature>();
			SimpleFeature f = null;
			for (Person p : this.pop.getPersons().values()){
				Plan plan = p.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Activity activity = (Activity) pe;
						if (activity.getType().compareTo(activityType) == 0){
							
							String id = p.getId().toString();
							String type = activity.getType();
							Double startTime = activity.getStartTime();
							Double endTime = activity.getEndTime();

							Coordinate actCoordinate = MGC.coord2Coordinate(activity.getCoord());
							actCoordinate = JTS.transform(actCoordinate, actCoordinate, transformation);
							
							f = factory.createPoint(actCoordinate, new Object[] {id, type, startTime, endTime}, null);
							features.add(f);
						}
					}
				}
			}
			
			ShapeFileWriter.writeGeometries(features, filename);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	
	}
	
}
