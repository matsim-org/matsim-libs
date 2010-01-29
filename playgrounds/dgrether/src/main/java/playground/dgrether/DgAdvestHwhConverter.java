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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
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
  
  private String plansIn = DgPaths.IVTCHBASE + "baseCase/plans/plans_miv_zrh30km_10pct.xml.gz";
  
  private String plansOut = DgPaths.IVTCHBASE + "baseCase/plans/plans_miv_zrh30km_10pct_morning_peek_5am_to_10am.xml.gz";
  
  private String planOutHomeWorkHome = DgPaths.IVTCHBASE + "baseCase/plans/plans_miv_zrh30km_10pct_morning_peek_5am_to_10am_hwh.xml.gz";
  
  private boolean dohwh = true;
  
  public DgAdvestHwhConverter(){
    Scenario sc = new ScenarioImpl();
    sc.getConfig().network().setInputFile(net);
    sc.getConfig().plans().setInputFile(plansIn);
    
    ScenarioLoader loader = new ScenarioLoaderImpl(sc);
    loader.loadScenario();
    
    Scenario newScenario = new ScenarioImpl();
    Scenario nonWorkScenario = new ScenarioImpl();
    Population newPop = newScenario.getPopulation();
    Population nonWorkPop = nonWorkScenario.getPopulation();
    Person newPerson = null;
    Plan newPlan = null;
    
    int newPersonCount = 0;
    int removedPersonCount = 0;
//    int removedNonWorkPersonCount = 0;
    
    for (Person person : sc.getPopulation().getPersons().values()){
      for (Plan p : person.getPlans()){
        List<Activity> activitiesInTime = new ArrayList<Activity>();
        List<Leg> legsInTime = new ArrayList<Leg>();
        for (PlanElement pe : p.getPlanElements()){
          if (pe instanceof Activity){
            Activity a = (Activity) pe;
            if (!(a.getEndTime() > 5 * 3600
                && a.getEndTime() < 10 * 3600)) {
              break;
            }
            activitiesInTime.add(a);
          }
          else if (pe instanceof Leg){
            legsInTime.add((Leg)pe);
          }
        }
        if (activitiesInTime.size() > 2) {
          log.info("");
          for (Activity a : activitiesInTime){
            log.info(" " + a.getType());
          }
        }
        
        if (activitiesInTime.size() > 0){
          newPerson = newPop.getFactory().createPerson(person.getId());
          newPlan = newPop.getFactory().createPlan();
          newPerson.addPlan(newPlan);
          newPop.addPerson(newPerson);
          newPersonCount++;
          if (dohwh){
            newPlan.addActivity(activitiesInTime.get(0));
          }
          else {
            activitiesInTime.get(0).setType("h");
            newPlan.addActivity(activitiesInTime.get(0));
          }
          newPlan.addLeg(newPop.getFactory().createLeg(TransportMode.car));
          Activity lastAct = null;
          if (activitiesInTime.size() > 1){
            List<Activity> notHomeActs = activitiesInTime.subList(1, activitiesInTime.size());
            Activity longestAct = notHomeActs.get(0);
            for (Activity a : notHomeActs){
              if (getActDuration(longestAct) < getActDuration(a)){
                longestAct = a;
              }
            }
            
            lastAct = longestAct;
          }
          else {
            lastAct = (Activity) p.getPlanElements().get(2);
          }
          if (dohwh){
            newPlan.addActivity(lastAct);
            newPlan.addLeg(newPop.getFactory().createLeg(TransportMode.car));
            newPlan.addActivity((Activity) p.getPlanElements().get(p.getPlanElements().size() -1));
          }
          else{
            lastAct.setType("h");
            lastAct.setEndTime(24 * 3600.0);
            newPlan.addActivity(lastAct);
          }
          
        }
        else {
          removedPersonCount++;
        }
        
      }
    }
    
    PopulationWriter writer = new PopulationWriter(newPop, sc.getNetwork());
    if (dohwh){
      writer.write(planOutHomeWorkHome);
    }
    else {
      writer.write(plansOut);
    }
//    writer = new PopulationWriter(nonWorkPop, sc.getNetwork());
//    writer.write(planOutNonWork);
    
    
    log.info("written new population with " + newPersonCount + " new persons.");
    log.info("removed " + removedPersonCount + " persons due to time constraints.");
//    log.info("removed " + removedNonWorkPersonCount + " persons due to non work constraints");
  }
  

  private double getActDuration(Activity a){
    String dur = a.getType().substring(1);
    return Double.parseDouble(dur);
  }
  
  
  /**
   * @param args
   */
  public static void main(String[] args) {
    new DgAdvestHwhConverter();
  }

}
