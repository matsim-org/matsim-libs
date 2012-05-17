/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package input;

import java.io.IOException;
import java.util.Random;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class AdaptPlans {
	
public static void main(String[] args) throws IOException {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
        
		Random random = new Random(4711);

		for (Person p : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
				if (pe instanceof Activity) {
					((PersonImpl)p).createDesires("desired activity durations");
                    ActivityImpl act = (ActivityImpl)pe;                         
                    if (act.getType().startsWith("s")) {
                    	if (random.nextDouble()<0.95){
                    		String OLD_ACT_TYPE = act.getType();
        					String DE = OLD_ACT_TYPE.substring(1);
        					double desire = Double.parseDouble(DE)*3600;
                        	((PersonImpl)p).getDesires().putActivityDuration("shop_retail", desire);
                    		act.setType("shop_retail");
                    	}
                    	else {
                    		String OLD_ACT_TYPE = act.getType();
        					String DE = OLD_ACT_TYPE.substring(1);
        					double desire = Double.parseDouble(DE)*3600;
                        	((PersonImpl)p).getDesires().putActivityDuration("shop_service", desire);
                    		act.setType("shop_service");
                    	}
                    }
                    if (act.getType().startsWith("l")) {
                    	if (random.nextDouble()<0.5){
                    		String OLD_ACT_TYPE = act.getType();
        					String DE = OLD_ACT_TYPE.substring(1);
        					double desire = Double.parseDouble(DE)*3600;
                        	((PersonImpl)p).getDesires().putActivityDuration("sports_fun", desire);
                    		act.setType("sports_fun");
                    	}
                    	else {
                    		String OLD_ACT_TYPE = act.getType();
        					String DE = OLD_ACT_TYPE.substring(1);
        					double desire = Double.parseDouble(DE)*3600;
                        	((PersonImpl)p).getDesires().putActivityDuration("gastro_culture", desire);
                    		act.setType("gastro_culture");
                    	}
                    }
                    if (act.getType().startsWith("w")) {
                    	String OLD_ACT_TYPE = act.getType();
    					String DE = OLD_ACT_TYPE.substring(1);
    					double desire = Double.parseDouble(DE)*3600;
                    	((PersonImpl)p).getDesires().putActivityDuration("work", desire);
                    	act.setType("work");
                    }
                    if (act.getType().startsWith("h")) {
                    	String OLD_ACT_TYPE = act.getType();
    					String DE = OLD_ACT_TYPE.substring(1);
    					double desire = Double.parseDouble(DE)*3600;
                    	((PersonImpl)p).getDesires().putActivityDuration("home", desire);
                    	act.setType("home");
                    }
                    if (act.getType().startsWith("e")) {
                    	String OLD_ACT_TYPE = act.getType();
    					String DE = OLD_ACT_TYPE.substring(1);
    					double desire = Double.parseDouble(DE)*3600;
                    	((PersonImpl)p).getDesires().putActivityDuration("education", desire);
                    	act.setType("education");
                    }
				}
			}
		}	
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write("./output/plans.xml");	
	}
}