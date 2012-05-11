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

package playground.michalm.demand;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.xml.sax.SAXException;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.*;


public class AbstractDemandGenerator
{
    private static final Logger log = Logger.getLogger(AbstractDemandGenerator.class);

    protected Uniform uniform = new Uniform(new MersenneTwister());

    private Scenario scenario;
    private PopulationFactory pf;

    Map<Id, Zone> zones;
    List<Zone> fileOrderedZones;


    public AbstractDemandGenerator(String networkFileName, String zonesXMLFileName,
            String zonesShpFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException

    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        pf = scenario.getPopulation().getFactory();
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFileName);

        ZoneXmlReader xmlReader = new ZoneXmlReader(scenario);
        xmlReader.parse(zonesXMLFileName);
        zones = xmlReader.getZones();
        fileOrderedZones = xmlReader.getZoneFileOrder();

        ZoneShpReader shpReader = new ZoneShpReader(scenario, zones);
        shpReader.readZones(zonesShpFileName, idField);
    }


    Activity createActivity(Plan plan, String actType, Coord coord)
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        Link link = network.getNearestLink(coord);
        Activity act = pf.createActivityFromLinkId(actType, link.getId());
        plan.addActivity(act);
        return act;
    }


    Coord getRandomCoordInZone(Zone zone)
    {
        Feature ft = zone.getZonePolygon();

        Envelope bounds = ft.getBounds();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        Geometry geometry = ft.getDefaultGeometry();
        Point p = null;
        do {
            double x = uniform.nextDoubleFromTo(minX, maxX);
            double y = uniform.nextDoubleFromTo(minY, maxY);
            p = MGC.xy2Point(x, y);
        }
        while (!geometry.contains(p));

        return scenario.createCoord(p.getX(), p.getY());
    }


    private int id = 0;


    // private static int ID_LENGTH = 6;
    //
    //
    // private String createId(String strId)
    // {
    // StringBuilder sb = new StringBuilder(ID_LENGTH);
    //
    // for (int i = ID_LENGTH - strId.length(); i > 0; i--) {
    // sb.append(' ');
    // }
    //
    // sb.append(strId);
    //
    // return sb.toString();
    // }

    Plan createPlan()
    {
        String strId = Integer.toString(id);
        id++;

        Person person = pf.createPerson(scenario.createId(strId));
        scenario.getPopulation().addPerson(person);

        Plan plan = pf.createPlan();
        person.addPlan(plan);
        return plan;
    }


    PopulationFactory getPopulationFactory()
    {
        return scenario.getPopulation().getFactory();
    }


    public void write(String plansfileName)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                .writeV4(plansfileName);
        log.info("Generated population written to: " + plansfileName);
    }
}
