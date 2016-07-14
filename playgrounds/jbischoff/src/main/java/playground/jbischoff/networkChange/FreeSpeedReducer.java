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

package playground.jbischoff.networkChange;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkFactory;
import org.matsim.core.network.LinkFactoryImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class FreeSpeedReducer
{

    /**
     * @param args
     */
    private static final double FACTOR = 0.5;
    
    public static void main(String[] args)
    {
    
        Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(sc.getNetwork()).readFile("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\svn-checkouts\\L013_matsim\\2014_ss\\ue\\ue6\\network.xml");
        NetworkImpl net1 = (NetworkImpl)sc.getNetwork();
        
        NetworkImpl net2 = (NetworkImpl)NetworkUtils.createNetwork();

        for (Node n : net1.getNodes().values()){
            Node newNode = n;
            newNode.getInLinks().clear();
            newNode.getOutLinks().clear();
            net2.addNode(newNode);
        }
        
        LinkFactory lf = new LinkFactoryImpl();
        for (Link l : net1.getLinks().values()){
            double oldFs = l.getFreespeed();
            double newFs = oldFs * FACTOR;
            l.setFreespeed(newFs);
            Link newLink = lf.createLink(l.getId(), l.getFromNode(), l.getToNode(), net2, l.getLength(), newFs, l.getCapacity(), l.getNumberOfLanes());
            net2.addLink(newLink);
        }
        
        
        new NetworkWriter(net2).write("\\\\vsp-nas\\jbischoff\\WinHome\\Docs\\svn-checkouts\\L013_matsim\\2014_ss\\ue\\ue6\\network_05.xml");
        
    }
    

}
