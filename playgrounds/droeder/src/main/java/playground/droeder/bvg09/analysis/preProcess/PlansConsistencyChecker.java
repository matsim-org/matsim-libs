/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.bvg09.analysis.preProcess;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ListIterator;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.filters.AbstractPersonFilter;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

/**
 * @author droeder
 *
 */
public class PlansConsistencyChecker {
	
	
	public static void main(String[] args){
		final String PATH = DaPaths.VSP + "BVG09_Auswertung/input/"; 
		final String PLANS = PATH + "bvg.run128.25pct.100.plans.selected.xml.gz";
		final String NETWORK = PATH + "network.final.xml.gz";
		
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		try {
			new NetworkReaderMatsimV1(sc).parse(NETWORK);
		} catch (SAXException e1) {
			e1.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		((PopulationImpl) sc.getPopulation()).setIsStreaming(true);
		PlanElementConsistencyFilter filter = new PlanElementConsistencyFilter();
		((PopulationImpl) sc.getPopulation()).addAlgorithm(filter);
		
		InputStream in = null;
		try{
			if(PLANS.endsWith("xml.gz")){
				in = new GZIPInputStream(new FileInputStream(PLANS));
			}else{
				in = new FileInputStream(PLANS);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			new PopulationReaderMatsimV4(sc).parse(in);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

class PlanElementConsistencyFilter extends AbstractPersonFilter{
	private static final Logger log = Logger
			.getLogger(PlanElementConsistencyFilter.class);

	@Override
	public void run(Person p){
		if(this.judge(p)){
			this.count();
		}
	}
	
	@Override
	public boolean judge(Person person) {
		ListIterator<PlanElement> it = person.getSelectedPlan().getPlanElements().listIterator();
		PlanElement pe;
		
		while(it.hasNext()){
			pe = it.next();
			
			if(it.previousIndex()%2 == 0){
				if(pe instanceof Leg){
					log.error("Agent: " + person.getId() + ", planElement " + it.previousIndex() + " should be an ativity!");
					return false;
				}
//				else{
//					System.out.print(((Activity) pe).getType().toString()+ " ");
//				}
			}else{
				if(pe instanceof Activity){
					log.error("Agent: " + person.getId() + ", planElement " + it.previousIndex() + " should be a leg!");
					return false;
				}
//				else{
//					System.out.print(((Leg) pe).getMode().toString() + " ");
//				}
			}
		}
//		System.out.println();
		return true;
	}
	
	
}
