/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.utils;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class ExtendedPersonFilter extends PersonFilter {

	private final static String MUNICH_SHAPE_FILE  = "../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
	private PersonFilter pf = new PersonFilter();
	private Collection<SimpleFeature> munichFeatures;
	private boolean isSortingForShapeFile = false;

	/**
	 * Use this if do not want to load shape file.
	 */
	public ExtendedPersonFilter (){};

	/**
	 * @param shapeFile person will be soreted based on this shape file. In general this should be a polygon shape.
	 */
	public ExtendedPersonFilter (String shapeFile){
		this.isSortingForShapeFile = true;
		this.munichFeatures = ShapeFileReader.getAllFeatures(shapeFile);
	}

	/**
	 * @param isSortingForInsideMunich true if want to sort person for Munich city area.
	 */
	public ExtendedPersonFilter (final boolean isFilteringForInsideMunichCity){
		if(isFilteringForInsideMunichCity) {
			this.munichFeatures = ShapeFileReader.getAllFeatures(MUNICH_SHAPE_FILE);
			this.isSortingForShapeFile = true;
			Logger.getLogger(ExtendedPersonFilter.class).info("Reading Munich city area shape file...");
		}
	}

	public boolean isCellInsideMunichCityArea(Coord cellCentroid) {
		if(! this.isSortingForShapeFile) throw new RuntimeException("No shape file is assigned to check if the centroid falls inside it. Aborting ...");
		boolean isInsideMunich = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.munichFeatures){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInsideMunich = true;
				break;
			}
		}
		return isInsideMunich;
	}

	public UserGroup getUserGroupFromPersonId (final Id<Person> personId) {
		UserGroup outUG = UserGroup.URBAN;
		for(UserGroup ug : UserGroup.values()){
			if(pf.isPersonIdFromUserGroup(personId, ug)) {
				outUG =ug;
				break;
			}
		}
		return outUG;
	}

	/**
	 * @param personId
	 * @return Urban or (Rev) commuter or Freight from person id.
	 */
	public String getMyUserGroupFromPersonId(final Id<Person> personId) {
		return getMyUserGroup(getUserGroupFromPersonId(personId));
	}

	/**
	 * @param ug
	 * Helpful for writing data to files.
	 */
	public String getMyUserGroup(final UserGroup ug){
		if(ug.equals(UserGroup.URBAN)) return "Urban";
		else if(ug.equals(UserGroup.COMMUTER)) return "(Rev)commuter";
		else if(ug.equals(UserGroup.REV_COMMUTER)) return "(Rev)commuter";
		else if (ug.equals(UserGroup.FREIGHT)) return "Freight";
		else throw new RuntimeException("User group "+ug+" is not recongnised. Aborting ...");
	}
}
