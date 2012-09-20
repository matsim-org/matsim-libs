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

import javax.naming.ConfigurationException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import playground.michalm.vrp.taxi.TaxiModeDepartureHandler;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;


public class SimpleDemandGenerator
{
    private static final Logger log = Logger.getLogger(SimpleDemandGenerator.class);

    private Scenario scenario;


    public SimpleDemandGenerator(String networkFileName, String idField)
        throws IOException, SAXException, ParserConfigurationException

    {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFileName);
    }


    private int id = 0;


    public void generate()
    {
        Population popul = scenario.getPopulation();
        PopulationFactory pf = popul.getFactory();
        NetworkImpl network = (NetworkImpl)scenario.getNetwork();

        List<Link> links = new ArrayList<Link>(network.getLinks().values());
        int linkCount = links.size();
        int planCount = 100;

        Uniform uniform = new Uniform(new MersenneTwister(new Date()));

        for (int p = 0; p < planCount; p++) {
            Person person = pf.createPerson(scenario.createId(Integer.toString(id++)));
            Plan plan = pf.createPlan();

            Link link = links.get(uniform.nextIntFromTo(0, linkCount - 1));
            Activity act = pf.createActivityFromLinkId("dummy", link.getId());
            act.setEndTime(uniform.nextIntFromTo(0, 23 * 60 * 60));
            plan.addActivity(act);

            plan.addLeg(pf.createLeg(TaxiModeDepartureHandler.TAXI_MODE));

            link = links.get(uniform.nextIntFromTo(0, linkCount - 1));
            act = pf.createActivityFromLinkId("dummy", link.getId());
            plan.addActivity(act);

            person.addPlan(plan);
            popul.addPerson(person);
        }
    }


    public void write(String plansfileName)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork())
                .writeV4(plansfileName);
        log.info("Generated population written to: " + plansfileName);
    }


    public static void main(String[] args)
        throws ConfigurationException, IOException, SAXException, ParserConfigurationException
    {
        String dirName;

        String networkFileName;
        String plansFileName;
        String idField;

        dirName = "D:\\PP-rad\\taxi\\siec1\\";
        networkFileName = dirName + "network.xml";
        plansFileName = dirName + "plans.xml";
        idField = "ID";

        SimpleDemandGenerator dg = new SimpleDemandGenerator(networkFileName, idField);
        dg.generate();
        dg.write(plansFileName);
    }
}
