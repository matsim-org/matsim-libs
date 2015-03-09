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

package playground.sergioo.workplaceCapacities2012;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class MaximumNumberOfWorkers {

	//Constants
	private static final String POLYGONS_FILE = "./data/facilities/Masterplan_Areas.shp";
	
	//Main
	public static void main(String[] args) throws SQLException, NoConnectionException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Map<Id<ActivityFacility>, Double> maximums = new HashMap<Id<ActivityFacility>, Double>();
		Map<Id<ActivityFacility>, Polygon> masterAreas = new HashMap<Id<ActivityFacility>, Polygon>();
		ShapeFileReader shapeFileReader =  new ShapeFileReader();
		Collection<SimpleFeature> features = shapeFileReader.readFileAndInitialize(POLYGONS_FILE);
		for(SimpleFeature feature:features)
			masterAreas.put(Id.create((Integer) feature.getAttribute(1), ActivityFacility.class), (Polygon) ((MultiPolygon)feature.getDefaultGeometry()).getGeometryN(0));
		DataBaseAdmin dataBaseRealState  = new DataBaseAdmin(new File("./data/facilities/DataBaseRealState.properties"));
		ResultSet buildingsR = dataBaseRealState.executeQuery("SELECT type, longitude, latitude FROM building_directory");
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_TM);
		GeometryFactory factory = new GeometryFactory();
		while(buildingsR.next()) {
			Coord buildingCoord = coordinateTransformation.transform(new CoordImpl(buildingsR.getDouble(2), buildingsR.getDouble(3)));
			Point p = factory.createPoint(new Coordinate(buildingCoord.getX(), buildingCoord.getY()));
			for(Entry<Id<ActivityFacility>, Polygon> polygon:masterAreas.entrySet())
				if(p.within(polygon.getValue())) {
					Double maximum = maximums.get(polygon.getKey());
					if(maximum==null)
						maximum = 0.0;
					maximum += getMaximum(buildingsR.getString(1), null);
					maximums.put(polygon.getKey(), maximum);
				}
		}
		dataBaseRealState.close();
		DataBaseAdmin dataBaseAux  = new DataBaseAdmin(new File("./data/facilities/DataBaseAuxiliar.properties"));
		for(Entry<Id<ActivityFacility>, Double> maximum:maximums.entrySet())
			dataBaseAux.executeUpdate("INSERT INTO buildings VALUES");
		dataBaseAux.close();					
	}

	private static Double getMaximum(String string, Object object) {
		// TODO Auto-generated method stub
		return null;
	}

}
