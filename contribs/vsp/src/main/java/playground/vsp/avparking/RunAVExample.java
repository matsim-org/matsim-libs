
/* *********************************************************************** *
* project: org.matsim.*
*                                                                         *
* *********************************************************************** *
*                                                                         *
* copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.avparking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import java.util.Random;

//import org.matsim.core.population.io.PopulationReader;

/**
* @author  jbischoff
* This is an example how to set different flow capacity consumptions for different vehicles. 
* Two groups of agents, one equipped with AVs (having an improved flow of factor 2), the other one using ordinary cars are traveling on two different routes in a grid network
* , highlighting the difference between vehicles.
* Network flow capacities are the same on all links.
* All agents try to depart at the same time. The queue is emptied twice as fast for the agents using an AV.
*  
*/
/**
*
*/
public class RunAVExample {
 
        /**
        * @param args
        */
        public static void main(String[] args) {
                
                new RunAVExample().run(false);
                
        }
        
        
        
        
        public void run (boolean otfvis){
               
                Config config = ConfigUtils.loadConfig("D:/Axer/MatsimDataStore/WOB_ACC/config.xml");
                config.plans().setInputFile("");
                Scenario scenario = ScenarioUtils.loadScenario(config);
                
                VehicleType avType = new VehicleTypeImpl(Id.create("autonomousVehicleType", VehicleType.class));
                avType.setFlowEfficiencyFactor(0.8);
                scenario.getVehicles().addVehicleType(avType);
                
                Random random = MatsimRandom.getRandom();
                double avprobability = 0.1;
                String identifier = "WB";
                for (Person p : scenario.getPopulation().getPersons().values()){
                        if (p.getId().toString().startsWith(identifier)){
                        	Plan plan = p.getSelectedPlan();
                        	boolean personUsesOnlyCar = true;
                        	for (PlanElement pe : plan.getPlanElements()){
                        		if (pe instanceof Leg){
                        			if (((Leg) pe).getMode()!=TransportMode.car){
                        				personUsesOnlyCar = false;
                        				break;
                        			}
                        		}
                        	}
                        	if (personUsesOnlyCar){
                        		if (random.nextDouble()<avprobability){
                        		Id<Vehicle> vid = Id.createVehicleId(p.getId());
                                Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, avType);
                                scenario.getVehicles().addVehicle(v);
                                System.out.println(p.getId().toString() + "added AV Vehicle");
                        	}
                        		}
                        }
                }
                
                Controler controler = new Controler(scenario);
                
                controler.run();
        } 
        
        
}
