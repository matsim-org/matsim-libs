/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.andreas.utils.pop;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.*;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Reset all "personal" attributes of a person
 *
 * @author aneumann
 *
 */
public class StripPersonPlan extends NewPopulation {
	private int planswritten = 0;
	private int personshandled = 0;

	public StripPersonPlan(Network network, Population plans, String filename) {
		super(network, plans, filename);
	}

	@Override
	public void run(Person pp) {

		Person person = (Person) pp;

		this.personshandled++;

		PopulationUtils.changePersonId( person, Id.create("p" + personshandled, Person.class) ) ;
		PersonUtils.setAge(person, Integer.MIN_VALUE);
		PersonUtils.setCarAvail(person, null);
		PersonUtils.setEmployed(person, (Boolean) null);
		PersonUtils.setLicence(person, null);
		PersonUtils.setSex(person, null);
		
		for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
			if (pE instanceof Activity) {
				Activity act = (Activity) pE;
				int x = (int) (act.getCoord().getX() / 100.0);
				int y = (int) (act.getCoord().getY() / 100.0);
				act.setCoord(new Coord(x * 100.0, y * 100.0));
			}
		}

		this.popWriter.writePerson(person);
	}

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		String networkFile = "F:/bb_5_v_scaled_simple.xml.gz";
		String inPlansFile = "F:/plans.xml.gz";
		String outPlansFile = "F:/plans_stripped.xml.gz";

		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);

		Population inPop = sc.getPopulation();
		MatsimReader popReader = new PopulationReader(sc);
		popReader.readFile(inPlansFile);

		StripPersonPlan dp = new StripPersonPlan(net, inPop, outPlansFile);
		dp.run(inPop);
		System.out.println(dp.personshandled + " persons handled; " + dp.planswritten + " plans written to file");
		dp.writeEndPlans();

		Gbl.printElapsedTime();
	}
}
