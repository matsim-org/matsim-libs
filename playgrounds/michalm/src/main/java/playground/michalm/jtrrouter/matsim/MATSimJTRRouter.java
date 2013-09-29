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

package playground.michalm.jtrrouter.matsim;

import java.io.*;
import java.util.*;

import org.apache.commons.configuration.*;
import org.apache.velocity.*;
import org.apache.velocity.app.*;
import org.apache.velocity.context.*;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.core.basic.v01.*;
import org.matsim.core.config.*;
import org.matsim.core.network.*;
import org.matsim.core.scenario.*;

import playground.michalm.jtrrouter.*;


/**
 * @author michalm
 */
public class MATSimJTRRouter
    extends JTRRouter
{
    private static final int TRAVEL_TIME = 1800;

    private final List<MATSimPlan> plans = new ArrayList<MATSimPlan>();
    private Map<Id, ? extends Link> idToLinkMap;


    public void readNetwork(String dir, String networkFile)
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(dir + "\\" + networkFile);
        idToLinkMap = scenario.getNetwork().getLinks();
    }


    protected void initFlow(HierarchicalConfiguration flowCfg)
    {
        // int node = flowCfg.getInt("[@node]");

        int inLink = flowCfg.getInt("[@inLink]", -1);
        int outLink = flowCfg.getInt("[@outLink]", -1);

        // int next = flowCfg.getInt("[@next]");
        int no = flowCfg.getInt("[@no]", 0);

        Node node;
        Node next;

        if (inLink != -1) {
            Link link = idToLinkMap.get(new IdImpl(inLink));

            node = link.getFromNode();
            next = link.getToNode();
        }
        else {
            Link link = idToLinkMap.get(new IdImpl(outLink));

            node = link.getToNode();
            next = link.getFromNode();
        }

        int nodeId = Integer.parseInt(node.getId().toString());
        int nextId = Integer.parseInt(next.getId().toString());

        flows[nodeId] = new MATSimFlow(nodeId, inLink, outLink, nextId, no);
    }


    protected void addPlan(int id, int startTime, Route route, int subFlow)
    {
        plans
                .add(new MATSimPlan(id, route, startTime, startTime
                        + TRAVEL_TIME));
    }


    protected void writePlans(String dir)
        throws Exception
    {
        Properties p = new Properties();
        p.setProperty("resource.loader", "class");
        p
                .setProperty("class.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        Velocity.init(p);
        Context context = new VelocityContext();
        context.put("plans", plans);

        Template template = Velocity
                .getTemplate("pl/poznan/put/transims/demand/matsim/plans.xml.vm");
        File planFile = new File(dir + "\\plans.xml");

        Writer writer = new BufferedWriter(new FileWriter(planFile));
        template.merge(context, writer);
        writer.close();
    }


    public static void main(String[] args)
        throws Exception
    {
        String dir = System.getProperty("dir");
        String flowsFile = System.getProperty("flows");
        String turnsFile = System.getProperty("turns");
        String networkFile = System.getProperty("network");

        dir = "d:\\PP-rad\\inz-matsim\\input";
        // dir = "C:\\inzynierka\\demand";
        flowsFile = "flows.xml";
        turnsFile = "turns.xml";
        networkFile = "network.xml";

        MATSimJTRRouter jtrRouter = new MATSimJTRRouter();
        jtrRouter.readNetwork(dir, networkFile);
        jtrRouter.generate(dir, flowsFile, turnsFile);
    }
}
