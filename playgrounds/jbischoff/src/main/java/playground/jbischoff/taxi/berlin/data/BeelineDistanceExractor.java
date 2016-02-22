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

package playground.jbischoff.taxi.berlin.data;

import java.io.*;
import java.text.DecimalFormat;
import java.util.TreeMap;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Point;

import playground.michalm.berlin.BerlinZoneUtils;

public class BeelineDistanceExractor
{

    TreeMap<Id<Zone>, Zone> zones;
    Scenario scenario;
    /**
     * @param args
     */
    public BeelineDistanceExractor()
    {
        this.zones = new TreeMap<>();
        this.zones.putAll(BerlinZoneUtils.readZones("C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/shp_merged/zones.xml",
                "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/shp_merged/zones.shp"));                 
        
        
    }
    
    
    
    
    public static void main(String[] args)
    {
        BeelineDistanceExractor bde = new BeelineDistanceExractor();
        bde.extractAndWriteTable("C:/local_jb/data/shp_merged/zonedistances.txt");
    }

   
    private void extractAndWriteTable(String filename)
    {
        Writer writer = IOUtils.getBufferedWriter(filename);
        DecimalFormat f = new DecimalFormat("#0.00"); 

        try {
            writer.append("Zone");
            for (Id<Zone> zoneID : this.zones.keySet()){
                writer.append("\t"+zoneID.toString());
            }
            writer.append("\n");
            
            for (Id<Zone> zoneID : this.zones.keySet()){
             
                writer.append(zoneID.toString());
                for (Id<Zone> secondZoneId : this.zones.keySet()){
                    double dist = calcDistance(zoneID, secondZoneId);
                    
                    writer.append("\t"+f.format(dist));
                }
                writer.append("\n");
            }
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }




    public double calcDistance(Id<Zone> zoneID, Id<Zone> secondZoneId)
    {
        Point p = this.zones.get(zoneID).getMultiPolygon().getCentroid();
        Coord z1 = new Coord(p.getX(), p.getY());
        Point p2 = this.zones.get(secondZoneId).getMultiPolygon().getCentroid();
        Coord z2 = new Coord(p2.getX(), p2.getY());
        double dist = DistanceUtils.calculateDistance(z1,z2) / 1000;
        return dist;
    }
    
    public Coord getZoneCentroid(Id<Zone> zoneID){
        return MGC.point2Coord(this.zones.get(zoneID).getMultiPolygon().getCentroid());
    }

}
