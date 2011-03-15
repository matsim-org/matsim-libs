/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesXmlReader.java
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
package playground.thibautd.jointtripsoptimizer.population;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.thibautd.jointtripsoptimizer.run.config.CliquesConfigGroup;

/**
 * @author thibautd
 */
public class CliquesXmlReader extends MatsimXmlParser {
	private static final Logger log =
		Logger.getLogger(CliquesXmlReader.class);


	private Map<String, ArrayList<String>> cliques = new HashMap<String, ArrayList<String>>();
	private String currentCliqueId;
	private ArrayList<String> currentMembers;

	private final PopulationWithCliques population;
	private final PopulationOfCliques populationOfCliques;
	private final PopulationOfCliquesFactory factory;
	private final String filePath;

	// constructor
	public CliquesXmlReader(ScenarioWithCliques scenario) {
		//do not check XML for consistency (no dtd)
		super(false);
		this.population = (PopulationWithCliques) scenario.getPopulation();
		this.populationOfCliques = population.getCliques();
		this.factory = (PopulationOfCliquesFactory) this.populationOfCliques.getFactory();

		CliquesConfigGroup configGroup;
		configGroup = (CliquesConfigGroup) scenario.getConfig().getModule(CliquesConfigGroup.GROUP_NAME);
		this.filePath = configGroup.getInputFile();
		log.debug("cliques file initialized to "+this.filePath);
	}

	/*
	 * internal
	 */
	private void constructCliques() {
		Clique currentClique;
		Map<Id, ? extends Person> populationMembers = population.getPersons();

		// iterate over cliques ids
		log.debug("begin cliques construction");
		for (String id : cliques.keySet()) {
			//log.debug("constructs clique "+id);
			// search corresponding members in the population and put them in a clique
			currentClique = this.factory.createClique(new IdImpl(id));
			currentMembers = cliques.get(id);
			//log.debug("clique members: "+currentMembers);

			for (String memberId: currentMembers) {
				try {
					//log.debug("adding an individual to a clique");
					//log.debug("population: "+populationMembers);
					currentClique.addMember(populationMembers.get(new IdImpl(memberId)));
				} catch (NullPointerException e) {
					throw new RuntimeException("clique members and population members do not match");
				}
			}
			// add the clique to the population.
			currentClique.buildJointPlanFromIndividualPlans();
			populationOfCliques.addClique(currentClique);
		}
	}

	/*
	 * XML
	 */
	public void parse() throws SAXException, ParserConfigurationException, IOException {
		this.parse(this.filePath);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		//Quick implementation, no consistency check.
		if (name.equals(CliquesSchemaNames.CLIQUES)) {
			this.currentCliqueId = "";
			this.currentMembers = new ArrayList<String>();
		}
		else if (name.equals(CliquesSchemaNames.CLIQUE)) {
			this.currentCliqueId = atts.getValue(CliquesSchemaNames.CLIQUE_ID);
		}
		else if (name.equals(CliquesSchemaNames.MEMBER)) {
			this.currentMembers.add( atts.getValue(CliquesSchemaNames.MEMBER_ID) );
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		//log.debug("endtag called for tag "+name);
		if (name.equals(CliquesSchemaNames.CLIQUE)) {
			//log.debug("adding members "+this.currentMembers+" to clique "+this.currentCliqueId);
			this.cliques.put(
					new String(this.currentCliqueId),
					new ArrayList<String>(this.currentMembers));
			this.currentMembers.clear();
		}
		else if (name.equals(CliquesSchemaNames.CLIQUES)) {
			//log.debug("cliques to add: "+this.cliques);
			this.constructCliques();
		}
	}
}

