/* *********************************************************************** *
 * project: org.matsim.*
 * DgNet2Tex
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.dgrether.daganzosignal.DaganzoScenarioGenerator;


/**
 * @author dgrether
 *
 */
public class DgNet2Tex {

	private static final Logger log = Logger.getLogger(DgNet2Tex.class);

	public DgNet2Tex() {}



	public void convert(NetworkImpl net, String texnet) {
		log.info("starting conversion...");
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(texnet);
			writer.write("\\begin{figure}[htp]");
			writer.newLine();
			writer.write("\\begin{center}");
			writer.newLine();
			writer.write("\\begin{tabular}[c]{|l|r|r|r|r|r|r|}");
			writer.newLine();
			this.hline(writer);
			writer.write("	Links 		& Length (m)& Lanes &$v_{fs}$ (m/s) & Capacity (veh/h) 	& $C_{storage}$ & $tt_{fs}$ (s) \\\\");
			writer.newLine();
			this.hline(writer);
			this.writeNetwork(net, writer);

			writer.write("\\end{tabular}");
			writer.newLine();
			writer.write("\\label{fig:}");
			writer.newLine();
			writer.write("\\end{center}");
			writer.newLine();
			writer.write("\\caption{}");
			writer.newLine();
			writer.write("\\end{figure}");
			writer.newLine();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("latex output written!");
	}

	private void writeNetwork(NetworkImpl net, BufferedWriter writer) throws IOException {
		List<Set<Link>> samePropertyLinksList = this.classifyNetwork(net);
		for (Set<Link> linkSet : samePropertyLinksList){
			Iterator<Link> it = linkSet.iterator();
			Link l = null;
			while (it.hasNext()){
				l = it.next();
				writer.write(l.getId().toString());
				if (it.hasNext()) {
					writer.write(" \\& ");
				}
			}
			writer.write("\t & \t");
			writer.write(Double.toString(l.getLength()));
			writer.write("\t & \t");
			writer.write(Double.toString(l.getNumberOfLanes()));
			writer.write("\t & \t");
			writer.write(Double.toString(l.getFreespeed()));
			writer.write("\t & \t");
			writer.write(Double.toString(l.getCapacity()));
			writer.write("\t & \t");
			writer.write(Double.toString(l.getLength() * l.getNumberOfLanes() / 7.5));
			writer.write("\t & \t");
			writer.write(Double.toString(l.getLength() / l.getFreespeed()));
			writer.write("\t \\\\");
			writer.newLine();
			this.hline(writer);
			writer.newLine();
		}
	}


	private List<Set<Link>> classifyNetwork(Network net){
		List<Set<Link>> samePropertyLinksList = new LinkedList<Set<Link>>();
		for (Link l : net.getLinks().values()){
			boolean added = false;
			for (Set<Link> linkSet : samePropertyLinksList) {
				Link compareLink = linkSet.iterator().next();
				if ((compareLink.getLength() == l.getLength())
						&& (compareLink.getNumberOfLanes() == l.getNumberOfLanes())
						&& (compareLink.getFreespeed() == l.getFreespeed())
						&& (compareLink.getCapacity() == l.getCapacity())){
					linkSet.add(l);
					added = true;
				}
			}
			if (!added) {
				Set<Link> linkSet = new HashSet<Link>();
				linkSet.add(l);
				samePropertyLinksList.add(linkSet);
			}
		}
		return samePropertyLinksList;
	}






	private void hline(BufferedWriter w) throws IOException{
		w.write("\\hline");
		w.newLine();
	}


	public void convert(String net, String texnet) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		NetworkImpl network = scenario.getNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(scenario);
		reader.readFile(net);
		this.convert(network, texnet);
	}



	public static void main(String[] args) {
		String net, texnet;
//		net = args[0];
//		texnet = args[1];
		net = DaganzoScenarioGenerator.DAGANZONETWORKFILE;
		texnet = net + ".tex";
		new DgNet2Tex().convert(net, texnet);
	}


}
