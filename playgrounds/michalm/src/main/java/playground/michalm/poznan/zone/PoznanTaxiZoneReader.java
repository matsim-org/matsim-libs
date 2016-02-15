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

package playground.michalm.poznan.zone;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.zone.*;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.*;


public class PoznanTaxiZoneReader
{
    private BufferedReader reader;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private final Map<Id<Zone>, Zone> zones = new TreeMap<>();


    public Map<Id<Zone>, Zone> getZones()
    {
        return zones;
    }


    private void read(String txtFile)
    {
        try (BufferedReader r = new BufferedReader(new FileReader(txtFile))) {
            this.reader = r;
            Map<String, Coord> coords = readCoords();
            readZones(coords);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Map<String, Coord> readCoords()
        throws IOException
    {
        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(
                TransformationFactory.WGS84, TransformationFactory.WGS84_UTM33N);
        Map<String, Coord> coords = new HashMap<>();

        String firstLine = reader.readLine();
        if (!firstLine.equals("PUNKTY")) {
            throw new RuntimeException();
        }

        while (true) {
            String line = reader.readLine();
            if (line.equals("POSTOJE;CCW")) {
                return coords;
            }

            StringTokenizer st = new StringTokenizer(line, ",xy=");
            String id = st.nextToken();
            double x = Double.parseDouble(st.nextToken());
            double y = Double.parseDouble(st.nextToken());
            coords.put(id, ct.transform(new Coord(x, y)));
        }
    }


    private void readZones(Map<String, Coord> coords)
        throws IOException
    {
        while (true) {
            StringTokenizer st = new StringTokenizer(reader.readLine(), ",");
            String token = st.nextToken();

            if (token.equals("KONIEC")) {
                break;
            }
            else if (token.equals("TARYFA")) {
                continue;// zone I
            }
            else if (token.equals("57")) {
                continue;// obsolete zone
            }

            Id<Zone> zoneId = Id.create(token, Zone.class);
            int count = st.countTokens();
            Coordinate[] ringCoords = new Coordinate[count + 1];

            for (int i = 0; i < count; i++) {
                String coordId = st.nextToken();
                ringCoords[i] = MGC.coord2Coordinate(coords.get(coordId));
            }
            ringCoords[count] = ringCoords[0];

            LinearRing shell = geometryFactory.createLinearRing(ringCoords);
            Polygon[] polygons = { geometryFactory.createPolygon(shell, null) };
            MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(polygons);
            zones.put(zoneId, new Zone(zoneId, "taxi_zone", multiPolygon));
        }

        removeInnerFromOuterZone("53", "47");//M1 + Franowo + Szpital Szwajcarska + Browar
        //removeInnerFromOuterZone("57", "31");//Male Garbary (obsolete zone)
    }


    private void removeInnerFromOuterZone(String innerId, String outerId)
    {
        Zone inner = zones.get(Id.create(innerId, Zone.class));
        Zone outer = zones.get(Id.create(outerId, Zone.class));
        Polygon polygon = (Polygon)outer.getMultiPolygon().difference(inner.getMultiPolygon());
        outer.setMultiPolygon(geometryFactory.createMultiPolygon(new Polygon[] { polygon }));
    }


    public static void main(String[] args)
    {
        String input = "d:/PP-rad/taxi/poznan-supply/dane/rejony/gps.txt";
        String zonesXmlFile = "d:/PP-rad/taxi/poznan-supply/dane/rejony/taxi_zones.xml";
        String zonesShpFile = "d:/PP-rad/taxi/poznan-supply/dane/rejony/taxi_zones.shp";

        PoznanTaxiZoneReader zoneReader = new PoznanTaxiZoneReader();
        zoneReader.read(input);
        Map<Id<Zone>, Zone> zones = zoneReader.getZones();
        Zones.writeZones(zones, TransformationFactory.WGS84_UTM33N, zonesXmlFile, zonesShpFile);
    }
}
