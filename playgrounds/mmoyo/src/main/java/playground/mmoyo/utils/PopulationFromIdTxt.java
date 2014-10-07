/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.io.TXT_IdReader;

/**Reads a text file with agents Ids and creates a population object from them. Read sequentially*/ 
public class PopulationFromIdTxt implements PersonAlgorithm {
	static DataLoader dataLoader = new DataLoader();
	private Population newPop;

    {
        ScenarioImpl sc = (ScenarioImpl) dataLoader.createScenario();
        newPop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());
    }

    PopulationWriter populationWriter;
	Network net;
	String outdir;
	private List<Id<Person>> persIds = new ArrayList<>();
	
	public PopulationFromIdTxt(final List<Id<Person>> persIds){
		this.persIds = persIds; 
	}
	
	final String dash = "/";
	
	@Override
	public void run(Person person) {
		if(this.persIds.contains(person.getId())){
			newPop.addPerson(person);
			populationWriter = new PopulationWriter(newPop, this.net);
			populationWriter.write(this.outdir + person.getId().toString() + ".xml.gz");
			newPop.getPersons().clear();
		}
	}
	
	public Population getPopulatio(){
		return this.newPop;
	}
	
	public void setNet (final Network net){
		this.net = net;
	}
	
	public void setOutDir (final String outdir){
		this.outdir = outdir;
	}
	
	
	public static void main(String[] args) {
		String populationFile = "../../";
		String netFile = "../../";
		String idsTxtFile = "../../";

		ScenarioImpl scn = (ScenarioImpl) dataLoader.createScenario();
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scn);
		matsimNetReader.readFile(netFile);
		
		List<Id<Person>> persIds = new TXT_IdReader().readAgentFromTxtFile(idsTxtFile);
		PopulationFromIdTxt populationFromIdTxt = new PopulationFromIdTxt(persIds); 
		populationFromIdTxt.setNet(scn.getNetwork());
		populationFromIdTxt.setOutDir("../../");
		
		PopSecReader popSecReader = new PopSecReader (scn, populationFromIdTxt);
		popSecReader.readFile(populationFile);
		
	}
	
}
