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
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.michalm.ev.*;
import playground.michalm.taxi.data.*;


public class TaxiRankReader
    extends MatsimXmlParser
{
    private final static String RANK = "rank";
    private final static String CHARGER = "charger";

    private final static int DEFAULT_RANK_CAPACITY = Integer.MAX_VALUE;
    private final static int DEFAULT_CHARGER_CAPACITY = 1;
    private final static int DEFAULT_CHARGER_POWER = 50;//50kW

    private final ETaxiData data;
    private Map<Id<Link>, ? extends Link> links;

    private TaxiRank currentRank;


    public TaxiRankReader(Scenario scenario, ETaxiData data)
    {
        this.data = data;
        links = scenario.getNetwork().getLinks();
    }


    @Override
    public void startTag(String name, Attributes atts, Stack<String> context)
    {
        if (RANK.equals(name)) {
            TaxiRank currentRank = createRank(atts);
            data.addTaxiRank(currentRank);
        }
        else if (CHARGER.equals(name)) {
            data.addCharger(createCharger(atts));
        }
    }


    @Override
    public void endTag(String name, String content, Stack<String> context)
    {}


    private TaxiRank createRank(Attributes atts)
    {
        Id<TaxiRank> id = Id.create(atts.getValue("id"), TaxiRank.class);
        String name = ReaderUtils.getString(atts, "name", id + "");
        Link link = links.get(Id.createLinkId(atts.getValue("link")));
        int capacity = ReaderUtils.getInt(atts, "capacity", DEFAULT_RANK_CAPACITY);
        return new TaxiRank(id, name, link, capacity);
    }


    private Charger createCharger(Attributes atts)
    {
        Id<Charger> id = Id.create(atts.getValue("id"), Charger.class);
        double power = ReaderUtils.getDouble(atts, "power", DEFAULT_CHARGER_POWER) * 1000;//kW --> W
        int capacity = ReaderUtils.getInt(atts, "capacity", DEFAULT_CHARGER_CAPACITY);
        return new ChargerImpl(id, power, capacity, currentRank.getLink());
    }
}
