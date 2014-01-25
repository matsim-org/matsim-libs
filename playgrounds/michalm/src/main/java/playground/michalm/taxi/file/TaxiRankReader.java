/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.file;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.extensions.electric.*;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.michalm.taxi.TaxiData;
import playground.michalm.taxi.model.*;


public class TaxiRankReader
    extends MatsimXmlParser
{
    private final static String RANK = "rank";
    private final static String VEHICLE = "vehicle";
    private final static String CHARGER = "charger";

    private final Scenario scenario;
    private final TaxiData data;
    private Map<Id, ? extends Link> links;

    private final EnergyConsumptionModel ecm;

    private TaxiRank currentRank;


    public TaxiRankReader(Scenario scenario, TaxiData data, EnergyConsumptionModel ecm)
    {
        this.scenario = scenario;
        this.data = data;
        this.ecm = ecm;

        links = scenario.getNetwork().getLinks();
    }


    public void readFile(String filename)
    {
        parse(filename);
    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (RANK.equals(name)) {
            startRank(atts);
        }
        else if (VEHICLE.equals(name)) {
            startVehicle(atts);
        }
        else if (CHARGER.equals(name)) {
            startCharger(atts);
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    private void startRank(Attributes atts)
    {
        List<TaxiRank> depots = data.getTaxiRanks();

        Id id = scenario.createId(atts.getValue("id"));

        String name = atts.getValue("name");

        Id linkId = scenario.createId(atts.getValue("link"));
        Link link = scenario.getNetwork().getLinks().get(linkId);

        currentRank = new TaxiRank(id, name, link);
        depots.add(currentRank);
    }


    private void startVehicle(Attributes atts)
    {
        List<Vehicle> vehicles = data.getVehicles();

        Id id = scenario.createId(atts.getValue("id"));

        Id startLinkId = scenario.createId(atts.getValue("start_link"));
        Link startLink = links.get(startLinkId);

        double t0 = getDouble(atts, "t_0", 0);
        double t1 = getDouble(atts, "t_1", 24 * 60 * 60);

        double chargeInJoules = getDouble(atts, "battery_charge_kWh", 20) * 1000 * 3600;
        double capacityInJoules = getDouble(atts, "battery_capacity_kWh", 20) * 1000 * 3600;

        ElectricVehicle ev = new VrpAgentElectricTaxi(id, startLink, t0, t1, ecm);
        ev.setBattery(new BatteryImpl(chargeInJoules, capacityInJoules));
        vehicles.add(ev);
    }


    private void startCharger(Attributes atts)
    {
        List<Charger> chargers = data.getChargers();

        Id id = scenario.createId(atts.getValue("id"));

        double powerInWatts = getDouble(atts, "power_kW", 20) * 1000;

        chargers.add(new ChargerImpl(id, powerInWatts, currentRank.getLink()));
    }


    private double getDouble(Attributes atts, String qName, double defaultValue)
    {
        String val = atts.getValue(qName);

        if (val != null) {
            return Double.parseDouble(val);
        }
        else {
            return defaultValue;
        }
    }
}
