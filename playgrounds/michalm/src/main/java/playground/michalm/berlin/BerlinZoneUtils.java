/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.berlin;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.zone.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;


public class BerlinZoneUtils
{
    public static final String ZONE_COORD_SYSTEM = "EPSG:25833";//ETRS89_UTM33N
    public static final String NETWORK_COORD_SYSTEM = TransformationFactory.DHDN_GK4;
    public static final CoordinateTransformation ZONE_TO_NETWORK_COORD_TRANSFORMATION = //
    TransformationFactory.getCoordinateTransformation(ZONE_COORD_SYSTEM, NETWORK_COORD_SYSTEM);

    public static final Id<Zone> TXL_LOR_ID = Id.create("12214125", Zone.class);
    public static final Id<Zone> SXF_LOR_ID = Id.create("12061433", Zone.class);

    public static final Id<Link> FROM_TXL_LINK_ID = Id.create(-35695, Link.class);
    public static final Id<Link> TO_TXL_LINK_ID = Id.create(-35954, Link.class);

    public static final Id<Link> FROM_SXF_LINK_ID = Id.create(-35828, Link.class);
    public static final Id<Link> TO_SXF_LINK_ID = Id.create(-35829, Link.class);

    //Coord objects are mutable, thus private; use createXXXCoord methods instead 
    private static final Coord FROM_TXL_COORD = new Coord(4588010.58447, 5825269.27936);
    private static final Coord TO_TXL_COORD = new Coord(4588009.07923, 5825207.56463);
    private static final Coord TXL_CENTROID = new Coord((FROM_TXL_COORD.getX() + TO_TXL_COORD.getX()) / 2, (FROM_TXL_COORD.getY() + TO_TXL_COORD.getY()) / 2);
    private static final Coord SXF_CENTROID = new Coord(4603210.22153, 5807381.44468);


    public static Map<Id<Zone>, Zone> readZones(String zonesXmlFile, String zonesShpFile)
    {
        Map<Id<Zone>, Zone> zones = Zones.readZones(zonesXmlFile, zonesShpFile);

        Zone txlZone = zones.get(TXL_LOR_ID);
        if (txlZone != null) {
            txlZone.setCoord(createTxlCentroid());
        }

        Zone sxfZone = zones.get(SXF_LOR_ID);
        if (sxfZone != null) {
            sxfZone.setCoord(createSxfCentroid());
        }

        return zones;
    }


    public static boolean isInBerlin(String zone)
    {
        if (zone.length() != 8) {
            throw new IllegalStateException();
        }

        return !zone.startsWith("120");
    }


    //this is necessary for status files where the leading zeros are removed 
    public static Id<Zone> createZoneId(String id)
    {
        String _8digitZoneId = StringUtils.leftPad(id, 8, '0');//some ids lack leading 0's
        return Id.create(_8digitZoneId, Zone.class);
    }


    public static Coord createFromTxlCoord()
    {
        return new Coord(FROM_TXL_COORD.getX(), FROM_TXL_COORD.getY());
    }


    public static Coord createToTxlCoord()
    {
        return new Coord(TO_TXL_COORD.getX(), TO_TXL_COORD.getY());
    }


    public static Coord createTxlCentroid()
    {
        return new Coord(TXL_CENTROID.getX(), TXL_CENTROID.getY());
    }


    public static Coord createSxfCentroid()
    {
        return new Coord(SXF_CENTROID.getX(), SXF_CENTROID.getY());
    }
}
