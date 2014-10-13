/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterByLegMode.java
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

package playground.pieter.singapore.utils.plans;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

/**
 * This algorithm filters out all persons having plans with legs with a certain
 * leg mode.
 * 
 * Plans which do not fulfill the filter criteria are removed from a person,
 * Persons with no plans are removed from the population.
 */
public class PlansFilterNoRoute {
	Scenario localScenario = ScenarioUtils.createScenario(ConfigUtils
			.createConfig());
	private PopulationImpl badPop;
	private PopulationWriter pw;

	public void run(Population plans, String badPopFileName, Network network) {
		badPop = (PopulationImpl) plans;
		badPop.setIsStreaming(true);
		pw = new PopulationWriter(badPop, network);
		pw.startStreaming(badPopFileName);
		this.run(plans);
	}

	public void run(Population plans) {
		int planCount = 0;
		System.out.println("    running " + this.getClass().getName()
				+ " algorithm...");

		try {

			DataBaseAdmin dba = new DataBaseAdmin(new File(
					"data/matsim2.properties"));
			dba.executeStatement("drop table if exists unrouteable_plans;");
			dba.executeStatement("create table unrouteable_plans (pid int default null, act varchar(45) default null, x_utm48n double default null, y_utm48n double default null);");

			TreeSet<Id<Person>> pid_set = new TreeSet<>(); // ids of persons to remove
			Iterator<Id<Person>> pid_it = plans.getPersons().keySet().iterator();
			while (pid_it.hasNext()) {
				Id<Person> personId = pid_it.next();
				Person person = plans.getPersons().get(personId);

				for (int i = person.getPlans().size() - 1; i >= 0; i--) {
					Plan plan = person.getPlans().get(i);
					boolean hasNoRoute = false;

					for (int j = 1; j < plan.getPlanElements().size(); j += 2) {
						LegImpl leg = (LegImpl) plan.getPlanElements().get(j);

						if (leg.getRoute() == null
								&& !leg.getMode().equals("transit_walk")) {
							hasNoRoute = true;
							// write origin and destination
							ActivityImpl act = (ActivityImpl) plan
									.getPlanElements().get(j - 1);
							String query = String
									.format("insert into unrouteable_plans values(%d,\'%s\',%f,%f);",
											Integer.parseInt(personId
													.toString()),
											act.getType(), act.getCoord()
													.getX(), act.getCoord()
													.getY());
							dba.executeStatement(query);
							act = (ActivityImpl) plan.getPlanElements().get(
									j + 1);
							query = String
									.format("insert into unrouteable_plans values(%d,\'%s\',%f,%f);",
											Integer.parseInt(personId
													.toString()),
											act.getType(), act.getCoord()
													.getX(), act.getCoord()
													.getY());
							dba.executeStatement(query);
						}

					}
					if (hasNoRoute) {
						pw.writePerson(person);
						person.getPlans().remove(i);
						i--; // otherwise, we would skip one plan
						planCount++;

					}

				}

				if (person.getPlans().isEmpty()) {
					// the person has no plans left. remove the person
					// afterwards (so we do not disrupt the Iterator)
					pid_set.add(personId);
				}
			}

			// okay, now remove in a 2nd step all persons we do no longer need
			pid_it = pid_set.iterator();
			while (pid_it.hasNext()) {

				Id pid = pid_it.next();
				plans.getPersons().remove(pid);
			}

			System.out.println("    done.");
			System.out.println("Number of plans removed:   " + planCount);
			System.out.println("Number of persons removed: " + pid_set.size());
			pw.closeStreaming();
		} catch (SQLException | IOException | ClassNotFoundException | IllegalAccessException | InstantiationException | NoConnectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

    }
}
