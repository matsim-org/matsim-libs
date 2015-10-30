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

package playground.andreas.fggeoinfo;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.andreas.utils.pop.NewPopulation;

/**
 * Merges two population files into one
 *
 * @author aneumann
 *
 */
public class MergePopulations extends NewPopulation {
	private int planswritten = 0;
	private int personsAdded = 0;
	
	private Population additionalPop;
	boolean added = false;

	public MergePopulations(Network network, Population plans, Population additionalPop, String filename) {
		super(network, plans, filename);
		this.additionalPop = additionalPop;
	}

	@Override
	public void run(Person person) {
		
		if(!this.added){
			for (Person personToAdd : this.additionalPop.getPersons().values()) {
				this.popWriter.writePerson(personToAdd);
				this.personsAdded++;
			}
			this.added = true;
		}
		
		this.popWriter.writePerson(person);
		this.planswritten++;

	}
	
	@Override
	public String toString(){
		return this.personsAdded + " new persons added; " + this.planswritten + " old plans written to file - total pop should be " + (this.personsAdded + this.planswritten);
	}
	
	public static void mergePopulations(String network, String plansFileOne, String plansFileTwo, String plansOutFile){
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MutableScenario scA = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(network);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(plansFileOne);
		
		Population additionalPop = scA.getPopulation();
		PopulationReader additionalPopReader = new MatsimPopulationReader(scA);
		additionalPopReader.readFile(plansFileTwo);
		

		MergePopulations dp = new MergePopulations(net, inPop, additionalPop, plansOutFile);
		dp.run(inPop);
		System.out.println(dp.toString());
			
		dp.writeEndPlans();
		
		Gbl.printElapsedTime();
	}
}
