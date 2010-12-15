/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsNearStop.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.analysis.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.population.PopulationImpl;
import org.matsim.run.OTFVis;

import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.PlanFragmenter;

/**Finds agents near a station who do not use that station. Optional by mode: pt, car or pt+car*/ 
public class FindAgentRejectNearStop {
	private ScenarioImpl scn;
	private final double distance;
	private final String type;
	
	public FindAgentRejectNearStop (final ScenarioImpl scn, final double distance, final String type){
		this.scn= scn;
		this.distance=distance;
		this.type = type;
	}
	
	public Population findPersRejectStop (String strStopId){
		Id stopId = new IdImpl(strStopId);
		Coord stopCoord = scn.getTransitSchedule().getFacilities().get(stopId).getCoord();

		//Create a new population where persons will be stored 
		ScenarioImpl tempScenario =new ScenarioImpl();
		PopulationImpl outputPopulation = new PopulationImpl(tempScenario);
		
		//create set of nodes coordinates around the station
		Collection <Node> nearNodes = scn.getNetwork().getNearestNodes(stopCoord, this.distance);
		Set <Coord> nearNodesCoord = new HashSet<Coord>();
		for (Node node : nearNodes){
			nearNodesCoord.add(node.getCoord());
		}
		
		/*
		if (this.type.equals("pt")){
			findNearAgentsPt (stopCoord, outputPopulation);
		}else if (this.type.equals("car")){
			findNearAgentsCar (stopCoord, outputPopulation);
		}else if (this.type.equals("pt+car")){
			findNearAgentsPt (stopCoord, outputPopulation);
			findNearAgentsCar(stopCoord, outputPopulation);
		}*/

		//fragment plans
		Population fragPop = scn.getPopulation(); //new PlanFragmenter().run(scn.getPopulation());					
		
		//look for persons who use stations around the given stop
		final String ptInteraction = "pt interaction";
		for (Person person : fragPop.getPersons().values()){
			//for (Plan plan : person.getPlans()){  // all plans or only selected plan?
				Plan plan = person.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()){
					if ((pe instanceof Activity)) {
						Activity act = (Activity)pe;
						if (nearNodesCoord.contains(act.getCoord())){
							if (act.getType().equals(ptInteraction)){
								if (!act.getCoord().equals(stopCoord)){  
									if ( !outputPopulation.getPersons().containsKey(person.getId())  ){
										outputPopulation.addPerson(person);	
									}
								}
							}
						}
					}
				}
			//}
		}
		System.out.println("persons around area: " +  outputPopulation.getPersons().size());
		
		return outputPopulation;	
	}
	
	
	///////"output" methods////////
	public void playAvoidingPopulation (String strStopId){
		Population pop = findPersRejectStop(strStopId);  

		//write new population and config files 
		String outPopFile = writePopulation (pop,strStopId );
		this.scn.getConfig().setParam("plans", "inputPlansFile", outPopFile);
		String outConfig = this.scn.getConfig().getParam("controler", "outputDirectory") + "config_avoid_" + strStopId + this.type + ".xml";
		new ConfigWriter(this.scn.getConfig()).write(outConfig);

		//set data containers to null, the OTFVis may need that memory
		pop= null;
		this.scn = null;
		
		OTFVis.playConfig(outConfig);
	}

	private void writePlan (final String strStopId){
		Population pop = findPersRejectStop(strStopId);
		writePopulation(pop, strStopId);
	}
	
	//writes the population file in the output directory of config
	private String writePopulation(Population population, String fileName){
		String outputFile = this.scn.getConfig().getParam("controler", "outputDirectory")+ "persAvoid_" +  fileName + "_" + this.type + ".xml";
		System.out.println("writing output plan file..." + outputFile);
		PopulationWriter popwriter = new PopulationWriter(population, this.scn.getNetwork());
		popwriter.write(outputFile) ;
		System.out.println("done");
		
		popwriter= null;
		return outputFile;
	}
	
	public static void main(String[] args) {
		String configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		String [] badStopsArray = {"812020.3", "812550.1", "812030.1", "812560.1" , "812570.1", "812013.1" };
		double distance = 500.0;
		final String type =  "pt";   //options:  "pt" , "car"  "pt+car"
		
		ScenarioImpl scenario = new DataLoader().loadScenarioWithTrSchedule(configFile);
		FindAgentRejectNearStop agentsNearStop = new FindAgentRejectNearStop(scenario, distance, type);
		//agentsNearStop.writePlan(badStopsArray[0]);                
		agentsNearStop.playAvoidingPopulation(badStopsArray[0]);    
	}

}




/*
public void findNearAgentsCar (final Coord stopCoord, Population outputPopulation){
	final String CAR = "car"; 
	for (Person person : scn.getPopulation().getPersons().values()){
		//for (Plan plan : person.getPlans()){  // all plans or only selected plan?
			Plan plan = person.getSelectedPlan();
			
			//find first act and leg
			Activity firstAct= null;
			Leg firstLeg= null;
			int i=0;
			while ((firstAct== null || firstLeg== null) && i< plan.getPlanElements().size()){
				PlanElement pe = plan.getPlanElements().get(i++);
				if (pe instanceof Activity ) {
					if(firstAct==null){
						firstAct = (Activity) pe;	
					}
				}else {
					if(firstLeg==null){
						firstLeg = (Leg) pe;	
					}
				}
			}
			
			//Add the agent to new population if the firstAct is near the stop and the first leg is car
			double dist1thAct2Stop = CoordUtils.calcDistance(firstAct.getCoord(), stopCoord);
			if (dist1thAct2Stop <= this.distance && firstLeg.getMode().equals(CAR)){
				outputPopulation.addPerson(person);
			}
	}
	
}*/