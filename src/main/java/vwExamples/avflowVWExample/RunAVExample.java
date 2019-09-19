package vwExamples.avflowVWExample;


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
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Random;

//import org.matsim.core.population.io.PopulationReader;


/**
 * @author jbischoff
 * This is an example how to set different flow capacity consumptions for different vehicles.
 * Two groups of agents, one equipped with AVs (having an improved flow of factor 2), the other one using ordinary cars are traveling on two different routes in a grid network
 * , highlighting the difference between vehicles.
 * Network flow capacities are the same on all links.
 * All agents try to depart at the same time. The queue is emptied twice as fast for the agents using an AV.
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


    public void run(boolean otfvis) {

        Config config = ConfigUtils.loadConfig("D:/Axer/MatsimDataStore/WOB_ACC/config.xml", new OTFVisConfigGroup());
        config.plans().setInputFile("D:/Axer/MatsimDataStore/WOB_ACC/population/run124.100.output_plans.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);

        VehicleType avType = VehicleUtils.createVehicleType(Id.create("autonomousVehicleType", VehicleType.class));
//                avType.setFlowEfficiencyFactor(0.69);
        avType.setFlowEfficiencyFactor(0.33);
        scenario.getVehicles().addVehicleType(avType);

        Random random = MatsimRandom.getRandom();
        //Auswahlwahrscheinlichkeit ein Fahrzeug mit verbessertem ACC auszustatten
        double accprobability = 0.15;
        String identifier = "WB";

        config.controler().setOutputDirectory("D:/Axer/MatsimDataStore/WOB_ACC/output_" + accprobability);

        //Wir iterieren über jede Person in unser Population
        for (Person p : scenario.getPopulation().getPersons().values()) {
            //Wir prüfen, ob diese Person mit einem prefix startet
            if (p.getId().toString().startsWith(identifier)) {
                System.out.println("Found person: " + p.getId().toString() + " with:" + identifier);


                //Wir nehmen nur den selected plan! Dieser wird nun weiter analysiert
                Plan plan = p.getSelectedPlan();
                boolean carInPlan = false;
                for (PlanElement pe : plan.getPlanElements()) {
                    //Es kann unterschiedliche PlanElements geben z.B. activity und leg etc...
                    if (pe instanceof Leg) {
                        if (((Leg) pe).getMode() == TransportMode.car) {
                            System.out.println(p.getId().toString() + " owns a car");
                            //Sobald in einem Plan car verwendet wird, ist dieser Agent prinzipiell relevant.
                            //Nun entscheidet der Zufall mit einer WSK von x % darüber, ob dieser Nutzer ein neues ACC Fahrzeug erhält.
                            carInPlan = true;
                            break;
                        }
                    }
                }
                if (carInPlan) {
                    if (random.nextDouble() < accprobability) {
                        Id<Vehicle> vid = Id.createVehicleId(p.getId());
                        Vehicle v = scenario.getVehicles().getFactory().createVehicle(vid, avType);
                        scenario.getVehicles().addVehicle(v);
                        System.out.println(p.getId().toString() + " and has now a new acc system on board");
                    }
                }
            }
        }

        Controler controler = new Controler(scenario);

        controler.run();
    }


}
