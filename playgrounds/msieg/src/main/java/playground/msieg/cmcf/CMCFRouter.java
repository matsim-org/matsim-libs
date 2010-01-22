/* *********************************************************************** *
 * project: org.matsim.*
 * CMCFRouter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.msieg.cmcf;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.xml.sax.SAXException;

import playground.msieg.structure.HashPathFlow;
import playground.msieg.structure.PathFlow;

public abstract class CMCFRouter implements NetworkReader{

	private static final Logger log = Logger.getLogger(CMCFRouter.class);

	private final String networkFile, plansFile, cmcfFile;

	private final ScenarioImpl scenario;
	protected NetworkLayer network;
	protected PopulationImpl population;
	protected PathFlow<Node, Link> pathFlow;


	public CMCFRouter(final String networkFile, final String plansFile, final String cmcfFile) {
		super();
		this.networkFile = networkFile;
		this.plansFile = plansFile;
		this.cmcfFile = cmcfFile;
		this.scenario = new ScenarioImpl();
	}

	public void read() throws IOException{
		this.loadNetwork();
		this.loadPopulation();
		this.loadCMCFSolution();
	}

	public void loadEverything(){
		this.loadNetwork();
		this.loadPopulation();
		try {
			this.loadCMCFSolution();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void loadNetwork(){
		//this.network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		//new MatsimNetworkReader(network).readFile(networkFile);
		this.network = scenario.getNetwork();
		MatsimNetworkReader reader = null;
		try {
			reader = new MatsimNetworkReader(scenario);
			reader.parse(this.networkFile);
			this.network.connect();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			//throw e;
			e.printStackTrace();
		}
	}

	private void loadPopulation(){
		this.population = scenario.getPopulation();
		new MatsimPopulationReader(this.scenario).readFile(this.plansFile);
	}

	protected void loadCMCFSolution() throws NumberFormatException, IOException{
		this.pathFlow = new HashPathFlow<Node, Link>();
		BufferedReader in = new BufferedReader(new FileReader(this.cmcfFile));
		String line = null;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			/***
			 * Example of a line which has to be extracted:
			 * Flow 7.5 on path 1: 2 -> 12 (15000, 2): 7 16
			 */
			if(line.startsWith("Flow")){
				line = line.substring(5);
				Double flow = new Double(line.substring(0, line.indexOf(' ')));
				line = line.substring(line.indexOf(':')+2);
				String fromID = line.substring(0, line.indexOf(" ->"));
				line = line.substring(line.indexOf("-> ")+3);
				String toID = line.substring(0,line.indexOf(" ("));
				String pathString = line.substring(line.indexOf("): ")+3).trim();

				List<Link> path = new LinkedList<Link>();
				StringTokenizer st = new StringTokenizer(pathString);
				while(st.hasMoreTokens()){
					path.add(this.network.getLinks().get(new IdImpl(st.nextToken())));
				}
				this.pathFlow.add(this.network.getNodes().get(new IdImpl(fromID)), this.network.getNodes().get(new IdImpl(toID)), path, flow);
			}
		}
	}

	public void writePlans(final String outPlansFile){
		//MatsimIo.writePlans(this.population, outPlansFile);
		new PopulationWriter(this.population, this.network).writeFile(outPlansFile);
	}

	abstract public void route();
}

