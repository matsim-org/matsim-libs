/* *********************************************************************** *
 * project: org.matsim.*
 * DgAdvestHwhConverter
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
package playground.dgrether;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.scenario.ScenarioLoaderImpl;


/**
 * @author dgrether
 *
 */
public class DgAdvestHwhConverter {
  
  private static final Logger log = Logger
      .getLogger(DgAdvestHwhConverter.class);
  
  private String net = DgPaths.IVTCHNET;
  
  private String plansIn = DgPaths.IVTCHBASE + "baseCase/plans_miv_zrh30km_10pct.xml.gz";
  
  private String plansOut = DgPaths.IVTCHBASE + "baseCase/plans_miv_zrh30km_10pct_morningPeek.xml.gz";
  
  public DgAdvestHwhConverter(){
    Scenario sc = new ScenarioImpl();
    sc.getConfig().network().setInputFile(net);
    sc.getConfig().plans().setInputFile(plansIn);
    
    ScenarioLoader loader = new ScenarioLoaderImpl(sc);
    loader.loadScenario();
    
    Scenario newScenario = new ScenarioImpl();
    Population newPop = newScenario.getPopulation();
    Person newPerson = null;
    Plan newPlan = null;
    
    int newPersonCount = 0;
    int removedPersonCount = 0;
    
    for (Person person : sc.getPopulation().getPersons().values()){
      for (Plan p : person.getPlans()){
        Activity firstAct = (Activity) p.getPlanElements().get(0);
        Leg firstLeg = (Leg) p.getPlanElements().get(1);
        Activity secondAct = (Activity) p.getPlanElements().get(2);
        
        if (firstAct != null 
            && firstAct.getEndTime() > 5 * 3600
            && firstAct.getEndTime() < 10 * 3600) {
          
          newPerson = newPop.getFactory().createPerson(person.getId());
          
          newPop.addPerson(newPerson);
          newPersonCount++;
        }
        else {
          removedPersonCount++;
        }
      }
    }
    
    PopulationWriter writer = new PopulationWriter(newPop, sc.getNetwork());
    writer.write(plansOut);
    
    log.info("written new population with " + newPersonCount + " new persons and removed " + removedPersonCount + "persons.");
    
  }
  
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    new DgAdvestHwhConverter();
  }

}
