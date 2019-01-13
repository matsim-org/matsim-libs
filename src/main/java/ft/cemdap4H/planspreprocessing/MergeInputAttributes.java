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

/**
 *
 */
package ft.cemdap4H.planspreprocessing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import playground.vsp.demandde.cemdap.output.ActivityTypes;

import java.util.Random;

/**
 * @author jbischoff
 */

/**
 *
 */
public class MergeInputAttributes {

    public static void main(String[] args) {
        //mit korrekter Anzahl von Students
        String attributesPersonFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_input\\Hannover_big_wchildren\\plans1.xml.gz";

        //hier gibt es aktuell keine Students
        String personsFile = "D:/cemdap-vw/cemdap_output/mergedplans_filtered_0.01.xml.gz";
        String outputpath = "";
        new MergeInputAttributes().run(personsFile, attributesPersonFile, outputpath);
    }

    public void run(String personsFile, String attributesPersonFile, String outputpath) {
        // scenAtt enthält Infos über Studenten
        Scenario scenAtt = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        // enthält keine Infos über Studenten
        Scenario scenPop = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        new PopulationReader(scenAtt).readFile(attributesPersonFile);
        new PopulationReader(scenPop).readFile(personsFile);

        //Für eine Person ohne Schulinfos
        for (Person p : scenPop.getPopulation().getPersons().values()) {
            boolean isValidStudent = false;
            //Prüfe ob diese Person ein Schüler ist mit Hilfe des attributesPersonFile
            Person pAtt = scenAtt.getPopulation().getPersons().get(p.getId());

            //Prüfe ob Person auch im scenAtt enthalten ist
            if (pAtt != null) {

                PersonUtils.setAge(p, PersonUtils.getAge(pAtt));
                Boolean license = (Boolean) pAtt.getAttributes().getAttribute("hasLicense");
                if (license) {
                    PersonUtils.setLicence(p, "yes");
                } else {
                    PersonUtils.setLicence(p, "no");
                }

                String schoolLoc = (String) pAtt.getAttributes().getAttribute("locationOfSchool");
                if (!schoolLoc.equals("-99")) {

                    for (Plan plan : p.getPlans()) {
                        for (PlanElement pe : plan.getPlanElements()) {
                            if (pe instanceof Activity) {
                                Activity act = (Activity) pe;
                                if (act.getType().startsWith("work")) {
                                    act.setType("education");
                                    //If the student has a work activity, the plan can be substituted with the education activity
                                    isValidStudent = true;

                                }

                            }
                        }
                    }

                    //The student has no work activity, thus we recreate a suitable student
                    if (isValidStudent == false) {
                        //Create a new student
                        makeStudent(p);
                    }


                }
                PersonUtils.setEmployed(p, PersonUtils.isEmployed(pAtt));
                int genderBit = (Integer) pAtt.getAttributes().getAttribute("gender");
                if (genderBit == 0) PersonUtils.setSex(p, "male");
                else PersonUtils.setSex(p, "female");

                if (PersonUtils.getLicense(p).equals("no")) {
                    PersonUtils.setCarAvail(p, "never");
                    for (Plan plan : p.getPlans()) {
                        for (PlanElement pe : plan.getPlanElements()) {
                            if (pe instanceof Leg) {
                                if (((Leg) pe).getMode().equals(TransportMode.car)) {
                                    ((Leg) pe).setMode(TransportMode.ride);
                                }
                            }
                        }
                    }
                } else {
                    PersonUtils.setCarAvail(p, "always");

                }

            }


        }
        new PopulationWriter(scenPop.getPopulation()).write(outputpath);

    }


    public Person makeStudent(Person inputPerson) {
        Coord homeCoord = null;
        //Random r = MatsimRandom.getRandom();
        Random rand = new Random();
        double r = rand.nextGaussian();

        Plan plan = inputPerson.getPlans().get(0);

        for (PlanElement pe : plan.getPlanElements()) {
            if (pe instanceof Activity) {
                Activity act = (Activity) pe;

                if (act.getType().startsWith("home")) {
                    homeCoord = act.getCoord();
                    break;
                }
            }

        }

        inputPerson.getPlans().clear();

        //Initialize activities, coordinates will be refined in ReassignZOnebyAttractiveness
        Activity startHomeAct = PopulationUtils.createActivityFromCoord(ActivityTypes.HOME, homeCoord);
        Activity educationAct = PopulationUtils.createActivityFromCoord(ActivityTypes.EDUCATION, homeCoord);
        Activity leisureAct = PopulationUtils.createActivityFromCoord(ActivityTypes.LEISURE, homeCoord);
        Activity shoppingAct = PopulationUtils.createActivityFromCoord(ActivityTypes.SHOPPING, homeCoord);
        Activity otherAct = PopulationUtils.createActivityFromCoord(ActivityTypes.OTHER, homeCoord);
        Activity endHomeAct = PopulationUtils.createActivityFromCoord(ActivityTypes.HOME, homeCoord);

        Plan eduPlan1 = PopulationUtils.createPlan();
        Plan eduPlan2 = PopulationUtils.createPlan();
        Plan eduPlan3 = PopulationUtils.createPlan();
        Plan eduPlan4 = PopulationUtils.createPlan();

        //Assign durations
        educationAct.setStartTime(8 * 3600 + r * 0.5 * 3600);
        educationAct.setEndTime(15 * 3600 + r * 2.5 * 3600);
        leisureAct.setEndTime(23 * 3600);
        otherAct.setEndTime(23 * 3600);
        shoppingAct.setEndTime(22 * 3600);

        //Set 1
        eduPlan1.addActivity(startHomeAct);
        eduPlan1.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan1.addActivity(educationAct);
        eduPlan1.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan1.addActivity(endHomeAct);

        //Set 2
        eduPlan2.addActivity(startHomeAct);
        eduPlan2.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan2.addActivity(educationAct);
        eduPlan2.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan2.addActivity(shoppingAct);
        eduPlan2.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan2.addActivity(endHomeAct);

        //Set 3
        eduPlan3.addActivity(startHomeAct);
        eduPlan3.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan3.addActivity(educationAct);
        eduPlan3.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan3.addActivity(leisureAct);
        eduPlan3.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan3.addActivity(endHomeAct);

        //Set 4
        eduPlan4.addActivity(startHomeAct);
        eduPlan4.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan4.addActivity(educationAct);
        eduPlan4.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan4.addActivity(otherAct);
        eduPlan4.addLeg(PopulationUtils.createLeg(TransportMode.ride));
        eduPlan4.addActivity(endHomeAct);


        inputPerson.addPlan(eduPlan1);
        inputPerson.addPlan(eduPlan2);
        inputPerson.addPlan(eduPlan3);
        inputPerson.addPlan(eduPlan4);


        return inputPerson;


    }
}