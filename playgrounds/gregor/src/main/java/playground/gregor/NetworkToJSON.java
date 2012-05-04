/* *********************************************************************** *
 * project: org.matsim.*
 * NEtworkToJSON.java
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

package playground.gregor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkToJSON {
	public static void main (String [] args) throws IOException {
		String net = "/Users/laemmel/teach/2012/20120424/data/network.xml.gz";
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new NetworkReaderMatsimV1(sc).parse(net);

		//		StringBuffer sb = new StringBuffer();
		BufferedWriter sb = new BufferedWriter(new FileWriter(new File("/Users/laemmel/teach/2012/20120424/data/network.json")));

		
		sb.append("{\"network\":{\n");
		sb.append("\"nodes\":[\n");
		int idx = 0;
		for (Node n : sc.getNetwork().getNodes().values()) {
			sb.append("\t{\"node\":{\n");
			sb.append("\t\t\"id\":\""+n.getId()+"\",\n");
			sb.append("\t\t\"x\":\""+n.getCoord().getX()+"\",\n");
			sb.append("\t\t\"y\":\""+n.getCoord().getY()+"\"\n");
			if (idx < sc.getNetwork().getNodes().size()-1) {
				sb.append("\t}},\n");
			}else {
				sb.append("\t}}\n");
			}
			idx++;
		}
		sb.append("],\n");

		sb.append("\"links\":[\n");
		idx = 0;
		for (Link l : sc.getNetwork().getLinks().values()) {
			sb.append("\t{\"link\":{\n");
			sb.append("\t\t\"id\":\""+l.getId()+"\",\n");
			sb.append("\t\t\"from\":\""+l.getFromNode().getId()+"\",\n");
			sb.append("\t\t\"to\":\""+l.getToNode().getId()+"\",\n");
			sb.append("\t\t\"capacity\":\""+l.getCapacity()+"\",\n");
			sb.append("\t\t\"length\":\""+l.getLength()+"\"\n");
			if (idx < sc.getNetwork().getLinks().size()-1) {
				sb.append("\t}},\n");
			}else {
				sb.append("\t}}\n");
			}
			idx++;
		}
		sb.append("]\n");
		sb.append("}}\n");
		sb.close();
		System.out.println(sb.toString());


		//		{"menu": {
		//			  "id": "file",
		//			  "value": "File",
		//			  "popup": {
		//			    "menuitem": [
		//			      {"value": "New", "onclick": "CreateNewDoc()"},
		//			      {"value": "Open", "onclick": "OpenDoc()"},
		//			      {"value": "Close", "onclick": "CloseDoc()"}
		//			    ]
		//			  }
		//			}}
	}

}
