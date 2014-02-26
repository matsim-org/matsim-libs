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

package playground.michalm.taxi.data.file;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.file.ReaderUtils;
import org.matsim.contrib.dvrp.extensions.electric.ChargerImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.michalm.taxi.data.*;


public class TaxiRankReader
    extends MatsimXmlParser
{
    private final static String RANK = "rank";
    private final static String CHARGER = "charger";

    private final Scenario scenario;
    private final TaxiData data;
    private Map<Id, ? extends Link> links;

    private TaxiRank currentRank;


    public TaxiRankReader(Scenario scenario, TaxiData data)
    {
        this.scenario = scenario;
        this.data = data;

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
        else if (CHARGER.equals(name)) {
            startCharger(atts);
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    private void startRank(Attributes atts)
    {
        Id id = scenario.createId(atts.getValue("id"));
        String name = atts.getValue("name");

        Id linkId = scenario.createId(atts.getValue("link"));
        Link link = links.get(linkId);

        currentRank = new TaxiRank(id, name, link);
        data.getTaxiRanks().add(currentRank);
    }


    private void startCharger(Attributes atts)
    {
        Id id = scenario.createId(atts.getValue("id"));
        double powerInWatts = ReaderUtils.getDouble(atts, "power", 20) * 1000;

        data.getChargers().add(new ChargerImpl(id, powerInWatts, currentRank.getLink()));
    }
}
