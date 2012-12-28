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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;
import org.xml.sax.SAXException;

import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;


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
        return createActivity(plan, actType, coord, null);
    }


    Activity createActivity(Plan plan, String actType, Coord coord, Id bannedLinkId)
    {
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();
        Link link = network.getNearestLink(coord);

        if (link.getId().equals(bannedLinkId)) {
            return null;
        }

        Activity act = pf.createActivityFromLinkId(actType, link.getId());
        plan.addActivity(act);
        return act;
    }


    Coord getRandomCoordInZone(Zone zone)
    {
        SimpleFeature ft = zone.getZonePolygon();

        BoundingBox bounds = ft.getBounds();
        double minX = bounds.getMinX();
        double maxX = bounds.getMaxX();
        double minY = bounds.getMinY();
        double maxY = bounds.getMaxY();

        Geometry geometry = (Geometry) ft.getDefaultGeometry();
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


    // private static final int ID_LENGTH = 6;
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
        return pf.createPlan();
    }


    Person createAndInitPerson(Plan plan)
    {
        String strId = Integer.toString(id);
        id++;

        Person person = pf.createPerson(scenario.createId(strId));
        person.addPlan(plan);

        scenario.getPopulation().addPerson(person);
        return person;
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
