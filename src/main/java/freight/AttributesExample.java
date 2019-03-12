package freight;/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class AttributesExample {

    public static void main(String[] args) {
        Config config = createConfig();
        Scenario scenario = createScenario(config);

        Person p = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("dummy"));
        Plan plan = PopulationUtils.createPlan();
        p.addPlan(plan);

        Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(0, 0));
        home.setEndTime(8 * 3600);

        home.getAttributes().putAttribute("deliveryType", "parcel");
        home.getAttributes().putAttribute("deliveryTimeStart", 6 * 3600);
        home.getAttributes().putAttribute("deliveryTimeEnd", 7 * 3600);

        // - zeitfenster  der lieferung
        // typ der Dienstleistung
        //

        plan.addActivity(home);
        Leg l = PopulationUtils.createLeg(TransportMode.car);

        plan.addLeg(l);

        scenario.getPopulation().addPerson(p);

        new PopulationWriter(scenario.getPopulation()).write("D:/dummy.xml");


    }
}
