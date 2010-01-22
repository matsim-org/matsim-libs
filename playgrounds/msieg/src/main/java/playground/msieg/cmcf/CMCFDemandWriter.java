/* *********************************************************************** *
 * project: org.matsim.*
 * CMCFDemandWriter.java
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.xml.sax.SAXException;

import playground.msieg.structure.Commodities;
import playground.msieg.structure.Commodity;

public class CMCFDemandWriter{

	private final NetworkLayer network;
	private final PopulationImpl plans;
	private final PopulationReader popReader;
	private final ScenarioImpl scenario;
	//The input tag has to be specified before converting starts,
	//it must equal the network name, otherwise CMCF won't run.
	private String inputNetwork = "notspecified";
	private final String networkPath, plansPath;

//	public CMCFDemandWriter(){
//		this.plans = new Population(Population.NO_STREAMING);
//		this.popReader = new MatsimPopulationReader(this.plans);
//	}

	public CMCFDemandWriter(final String configPath){
		this( 	Gbl.createConfig(new String[] { configPath, "config_v1.dtd" }));
	}
	
	private CMCFDemandWriter(final Config config) {
		this(config.network().getInputFile(), config.plans().getInputFile());
	}

	public CMCFDemandWriter(final String networkPath, final String plansPath){
		this.networkPath = networkPath == null ? Gbl.getConfig().network().getInputFile(): networkPath;
		this.plansPath = plansPath == null ? Gbl.getConfig().plans().getInputFile(): plansPath;

		this.scenario = new ScenarioImpl();
		this.network = scenario.getNetwork();
		this.plans = scenario.getPopulation();
		this.popReader = new MatsimPopulationReader(scenario);

		this.init();
	}

	private void init(){
		try {
			new MatsimNetworkReader(this.scenario).parse(this.networkPath);
			this.network.connect();
			this.popReader.readFile(this.plansPath);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Deprecated
	public void readFile()
	{
		this.init();
	}


	public void setInputNetwork(final String inputNetwork) {
		this.inputNetwork = inputNetwork;
	}


	/**
	 * Converts plan file and prints out to console, equivalent to call convert(null)
	 */
	public void convert(){
		this.convert(null);
	}

	public void convert(final int numberOfTimeSteps){
		this.convert(null, numberOfTimeSteps);
	}

	/**
	 * Converts the plans file into CMCF format and writes it to the specified Writer.
	 * If null is given, output is being printed out to the console.
	 * Make sure you have called read() before calling this function.
	 * @param out the Writer where to write the output, or null
	 */
	public void convert(final Writer out){
		this.convert(out, 1);
	}
	public void convert(final Writer out, final int numberOfTimeSteps){
		log("<demandgraph>\n", out);
		this.convertHeader(out, (byte) 1);
		this.convertDemands(out, (byte) 1, numberOfTimeSteps);
		log("</demandgraph>", out);
	}

	/**
	 * Make something like:
	 *       <header>
	 *               <name>anyname</name>
 	 *               <date>1226498697127</date>
 	 *               <author>msieg</author>
 	 *               <creator>CMCFDemandWriter</creator>
 	 *               <input>sourcenetwork</input>
 	 *       </header>
 	 *
 	 * Very important is the input tag, CMCF only accepts the demand file,
 	 * if the input tag equals the name of the network!
	 * @param out
	 * @param tabs
	 */
	protected void convertHeader(final Writer out, byte tabs){
		String tab="";
		while(tabs-- > 0)
			tab += '\t';
		log(tab+"<header>\n", out);
		log(tab+"\t<name>"+this.plans.getName()+"</name>\n", out);
		log(tab+"\t<date>"+System.currentTimeMillis()+"</date>\n", out);
		log(tab+"\t<creator>"+this.getClass().getSimpleName()+"</creator>\n", out);
		log(tab+"\t<input>"+this.inputNetwork+"</input>\n", out);
		log(tab+"</header>\n", out);
	}


	/**
	 * Make something like:
	 *          <demands>
     *            <commodity id="1">
     *                   <from>6</from>
     *                   <to>9</to>
     *                   <demand>400.0</demand>
     *           </commodity>
	 *			</demands>
	 * @param out
	 * @param tabs
	 */
	protected void convertDemands(final Writer out, final byte tabs){
		this.convertDemands(out, tabs, 1);
	}
	/**
	 * To create time dependent solutions for MATSIM, the parameter divisor is introduced
	 * For each commodity the demand value is divided by divisor.
	 * @param out
	 * @param tabs
	 * @param divisor
	 */
	protected void convertDemands(final Writer out, byte tabs, final int divisor){
		String tab="";
		while(tabs-- > 0)
			tab += '\t';
		//in advance, read input data and store all the demands accumulated:
		Commodities<Node> com = new Commodities<Node>();

		Plan plan;
		ActivityImpl act1, act2;
		LegImpl leg;
		for (Id id : this.plans.getPersons().keySet()) {
			plan = this.plans.getPersons().get(id).getSelectedPlan();
			act1 = ((PlanImpl) plan).getFirstActivity();
			leg = ((PlanImpl) plan).getNextLeg(act1);
			act2 = ((PlanImpl) plan).getNextActivity(leg);
			com.add( this.network.getLinks().get(act1.getLinkId()).getToNode(), this.network.getLinks().get(act2.getLinkId()).getFromNode(), 1);
		}

		//now write the output
		log(tab+"<demands>\n", out);
		int counter = 1;
		for(Commodity<Node> c: com){
			if(c.getOrigin() == c.getDestination()){
				log(tab+"\t<!--- Skipping commodity, because start node equals target --->", out);
				continue;
			}
			log(tab+"\t<commodity id=\""+(counter++)+"\">\n", out);
			log(tab+"\t\t<from>"+c.getOrigin().getId()+"</from>\n", out);
			log(tab+"\t\t<to>"+c.getDestination().getId()+"</to>\n", out);
			log(tab+"\t\t<demand>"+
					( divisor == 1 ? c.getDemand() : c.getDemand().doubleValue()/divisor )
					+"</demand>\n", out);
			log(tab+"\t</commodity>\n", out);
		}
		log(tab+"</demands>\n", out);
	}

	private void log(final String s, final Writer out){
		if(out == null){
			System.out.print(s);
		}
		else{
			try {
				out.write(s);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(final String[] args) {
		if(args.length==0){
			System.out.println("Usage: java CMCFDemandWriter config.xml \n\tOR: java CMCFDemandWriter network.xml plans.xml [outputFile] [input] \n" +
					"\t second argument is the plansFile to convert, if not given, then the plans file in the config is used." +
					"\t third argument is optional if not given, then output is written to console.\n" +
					"\t fourth argument is also optional, if not given then its set to 'unspecified'," +
					" this is the name of the network used, necessary for CMCF!");
			return;
		}
		CMCFDemandWriter cdw = null;
		if(args.length==1)
			cdw = new CMCFDemandWriter(args[0]);
		else
			cdw = new CMCFDemandWriter(args[0], args[1]);
		Writer out=null;
		if(args.length>2){
			System.out.print(" Trying to access output file '"+args[2]+"' ... ");
			try {
				out = new FileWriter(args[2]);
				System.out.println(" [DONE]");
			} catch (IOException e) {
				e.printStackTrace();
				out = null;
				System.out.println(" Sorry, but access denied, writing output to console.");
			}
		}
		if(args.length > 3)
			cdw.setInputNetwork(args[3]);
		//do it
		cdw.readFile();
		cdw.convert(out);
	}
}
