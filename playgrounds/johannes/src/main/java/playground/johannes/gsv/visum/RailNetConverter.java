/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.visum;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.johannes.gsv.visum.NetFileReader.TableHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author johannes
 *
 */
public class RailNetConverter {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		Map<String, String> modeMappings = new HashMap<String, String>();
		modeMappings.put("1", TransportMode.other);
		modeMappings.put("2", TransportMode.other);
		modeMappings.put("3", TransportMode.other);
		modeMappings.put("4", TransportMode.other);
		modeMappings.put("5", TransportMode.other);
		modeMappings.put("9", TransportMode.other);
		modeMappings.put("A", TransportMode.pt);
		modeMappings.put("B", TransportMode.pt);
		modeMappings.put("C", TransportMode.pt);
		modeMappings.put("D", TransportMode.pt);
		modeMappings.put("E", TransportMode.pt);
		modeMappings.put("F", TransportMode.walk);
		modeMappings.put("H", TransportMode.pt);
		modeMappings.put("I", TransportMode.pt);
		modeMappings.put("J", TransportMode.pt);
		modeMappings.put("K", TransportMode.pt);
		modeMappings.put("N", TransportMode.other);
		modeMappings.put("O", TransportMode.pt);
		modeMappings.put("R", TransportMode.pt);
		modeMappings.put("S", TransportMode.pt);
		modeMappings.put("T", TransportMode.pt);
		modeMappings.put("V", TransportMode.pt);
		modeMappings.put("W", TransportMode.pt);
		modeMappings.put("X", TransportMode.pt);
		
		PrefixIdGenerator idGen = new PrefixIdGenerator("rail.");
		
		Map<String, TableHandler> handlers = new HashMap<String, NetFileReader.TableHandler>(2);
		
		NodeTableHandler nHandler = new NodeTableHandler(scenario.getNetwork());
		nHandler.setIdGenerator(idGen);
		handlers.put("KNOTEN", nHandler);
		
		LinkTableHandler lHandler = new LinkTableHandler(scenario.getNetwork(), modeMappings);
		lHandler.setIdGenerator(idGen);
		handlers.put("STRECKE", lHandler);
		
		NetFileReader reader = new NetFileReader(handlers);
		reader.read("/home/johannes/gsv/matsim/studies/netz2030/data/raw/network.net");
		
		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.write("/home/johannes/gsv/matsim/studies/netz2030/data/network.rail.xml");
	}

}
