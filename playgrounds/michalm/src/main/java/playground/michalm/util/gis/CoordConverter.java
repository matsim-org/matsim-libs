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

package playground.michalm.util.gis;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.*;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;


//WGS84 / UTM 33N 
public class CoordConverter
{
    public static void main(String[] args)
        throws FactoryException, TransformException
    {
        // double latitude = LocationUtils.DDD_MMToDegrees(52.24373);
        // double longitude = LocationUtils.DDD_MMToDegrees(16.49682); // lat, lon in degrees

        double[] longitudes = { 16.49685, 16.49673, 16.50145, 16.49941, 16.49881, 16.52067,
                16.50056, 16.50002, 16.50484, 16.50444 };
        double[] latitudes = { 52.24373, 52.24025, 52.23953, 52.24597, 52.24024, 52.22913,
                52.24021, 52.241, 52.23998, 52.24544 };

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);

        for (int i = 0; i < longitudes.length; i++) {
            double lon = longitudes[i];
            double lat = latitudes[i];

            Coord coord = ct.transform(new CoordImpl(lon, lat));

            System.out.printf("%f\t%f\t%f\t%f\n", lon, lat, coord.getX(), coord.getY());
        }
    }
}
