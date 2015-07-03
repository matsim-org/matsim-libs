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

	private PersonFilter pf = new PersonFilter();
	private Collection<SimpleFeature> munichFeatures;
	private String shapeFile ;

	/**
	 * Use this if do not want to load shape file.
	 */
	public ExtendedPersonFilter (){};
	
	/**
	 * @param shapeFile person will be soreted based on this shape file. In general this should be a polygon shape.
	 */
	public ExtendedPersonFilter (String shapeFile){
		this.shapeFile = shapeFile;
		this.munichFeatures = ShapeFileReader.getAllFeatures(this.shapeFile);
	}

	/**
	 * @param isSortingForInsideMunich true if want to sort person for Munich city area.
	 */
	public ExtendedPersonFilter (boolean isSortingForInsideMunich){
		this.shapeFile = "../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
		Logger.getLogger(ExtendedPersonFilter.class).info("Reading Munich city area shape file...");
		this.munichFeatures = ShapeFileReader.getAllFeatures(this.shapeFile);
	}

	public boolean isCellInsideMunichCityArea(Coord cellCentroid) {
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

	public UserGroup getUserGroupFromPersonId (Id<Person> personId) {
		UserGroup outUG = UserGroup.URBAN;
		for(UserGroup ug : UserGroup.values()){
			if(pf.isPersonIdFromUserGroup(personId, ug)) {
				outUG =ug;
				break;
			}
		}
		return outUG;
	}
}
