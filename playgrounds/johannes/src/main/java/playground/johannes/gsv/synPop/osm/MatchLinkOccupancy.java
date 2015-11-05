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

package playground.johannes.gsv.synPop.osm;

import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;

/**
 * @author johannes
 *
 */
public class MatchLinkOccupancy {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		args = new String[3];
		args[0] = "/home/johannes/gsv/osm/network/germany-network-cat5.simpl2.xml";
		args[1] = "/home/johannes/gsv/ger/output/linkoccup.txt";
		args[2] = "/home/johannes/gsv/ger/output/linkoccup.osm.txt";

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(args[0]);
		Network network = scenario.getNetwork();
		
		TObjectDoubleHashMap<String> values = new TObjectDoubleHashMap<>();
		TObjectDoubleHashMap<String> matchedValues = new TObjectDoubleHashMap<>();
		
		BufferedReader reader = new BufferedReader(new FileReader(args[1]));
		String line = reader.readLine();
		while((line = reader.readLine()) != null) {
			String tokens[] = line.split("\t");
			values.put(tokens[0], Double.parseDouble(tokens[1]));
		}
		reader.close();
		
		TObjectDoubleIterator<String> it = values.iterator();
		for(int i = 0; i < values.size(); i++) {
			it.advance();
			String id = it.key();
			double value = it.value();
			
			Id<Link> linkId = Id.create(id, Link.class);
			Link link = network.getLinks().get(linkId);
			String osmIds[] = ((LinkImpl)link).getOrigId().split(",");
			for(String osmId : osmIds) {
				matchedValues.put(osmId, value);
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
		writer.write("link\toccupancy");
		writer.newLine();
		it = matchedValues.iterator();
		for(int i = 0; i < matchedValues.size(); i++) {
			it.advance();
			writer.write(it.key());
			writer.write("\t");
			writer.write(String.valueOf(it.value()));
			writer.newLine();
		}
		writer.close();
	}

}
