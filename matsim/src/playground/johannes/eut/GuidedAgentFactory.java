/* *********************************************************************** *
 * project: org.matsim.*
 * GuidedAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.eut;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.controler.Controler;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Person;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.io.IOUtils;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.WithindayAgentLogicFactory;
import org.matsim.withinday.contentment.AgentContentmentI;
import org.matsim.withinday.routeprovider.RouteProvider;

/**
 * @author illenberger
 *
 */
public class GuidedAgentFactory extends WithindayAgentLogicFactory {

	private final double equipmentFraction;
	
	private static final ForceReplan forceReplan = new ForceReplan();
	
	private static final PreventReplan preventReplan = new PreventReplan();
	
	private final ReactRouteGuidance router;
	
	private Random random;
	
	private EUTRouterAnalyzer analyzer;
	
	private BenefitAnalyzer benefitAnalyzer;
	
	private BufferedWriter writer;
	
	private Set<Person> guidedPersons;
	/**#
	 * @param network
	 * @param scoringConfig
	 */
	public GuidedAgentFactory(NetworkLayer network,
			CharyparNagelScoringConfigGroup scoringConfig, TravelTimeI reactTTs, double fraction) {
		super(network, scoringConfig);
		router = new ReactRouteGuidance(network, reactTTs);
		equipmentFraction = fraction;
	}

	public void setRouteAnalyzer(EUTRouterAnalyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	public void setBenefitAnalyzer(BenefitAnalyzer benefitAnalyzer) {
		this.benefitAnalyzer = benefitAnalyzer;
	}
	
	@Override
	public AgentContentmentI createAgentContentment(WithindayAgent agent) {
		random.nextDouble();
		if(random.nextDouble() < equipmentFraction) {
			if(analyzer != null)
				analyzer.addGuidedPerson(agent.getPerson());
			if(benefitAnalyzer != null)
				benefitAnalyzer.addGuidedPerson(agent.getPerson());
			try {
				writer.write(agent.getPerson().getId().toString());
				writer.newLine();
			} catch (Exception e) {
				e.printStackTrace();
			}
			guidedPersons.add(agent.getPerson());
			return forceReplan;
		} else
			return preventReplan;
	}

	@Override
	public RouteProvider createRouteProvider() {
		return router;
	}
	
	public Set<Person> getGuidedPersons() {
		return guidedPersons;
	}

	public void reset() {
		try {
			if (writer != null)
				writer.close();

			writer = IOUtils.getBufferedWriter(Controler
					.getIterationFilename("guidedPersons.txt"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		random = new Random(10);
		guidedPersons = new HashSet<Person>();
	}

}
