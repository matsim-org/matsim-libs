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

package playground.michalm.chargerlocation;

import java.io.*;
import java.util.Map;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.berlin.BerlinZoneUtils;
import playground.michalm.ev.*;
import playground.michalm.taxi.data.*;


public class ChargerReader
{
    private final NetworkImpl network;
    private final ETaxiData data;
    private final Map<Id<Zone>, Zone> zones;
    private final double power;


    public ChargerReader(Scenario scenario, ETaxiData data, Map<Id<Zone>, Zone> zones, double power)
    {
        this.network = (NetworkImpl)scenario.getNetwork();
        this.data = data;
        this.zones = zones;
        this.power = power;
    }


    public void readFile(String file)
    {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            //zoneId,chargerCount

            String line = null;
            while ( (line = br.readLine()) != null) {
                //01011101,2
                String[] vals = line.split(",");
                String id = vals[0];
                int capacity = Integer.parseInt(vals[1]);

                Zone zone = zones.get(Id.create(id, Zone.class));
                Coord coord = BerlinZoneUtils.ZONE_TO_NETWORK_COORD_TRANSFORMATION
                        .transform(zone.getCoord());
                Link link = network.getNearestLinkExactly(coord);
                data.addCharger(
                        new ChargerImpl(Id.create(id, Charger.class), power, capacity, link));

                //???
                data.addTaxiRank(
                        new TaxiRank(Id.create(id, TaxiRank.class), id, link, 10 * capacity));
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args)
    {
        String dir = "d:/svn-vsp/sustainability-w-michal-and-dlr/data/";
        String networkFile = dir + "network/only_berlin.xml.gz";
        String zonesXmlFile = dir + "shp_merged/berlin_zones.xml";
        String zonesShpFile = dir + "shp_merged/berlin_zones.shp";
        String file = dir
                + "scenarios/2015_08_only_berlin_v1/chargers_out_of_231_COLD_WINTER_FOSSIL_HEATING_noDeltaSOC.csv";
        double power = 50_000;//50kW 

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

        Map<Id<Zone>, Zone> zones = BerlinZoneUtils.readZones(zonesXmlFile, zonesShpFile);

        ETaxiData data = new ETaxiData();

        new ChargerReader(scenario, data, zones, power).readFile(file);
        System.out.println(data.getChargers().size());
    }
}
