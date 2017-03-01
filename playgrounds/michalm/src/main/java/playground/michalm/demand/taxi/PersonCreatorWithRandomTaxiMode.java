/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.demand.taxi;

import java.io.*;
import java.util.*;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.random.*;
import org.matsim.contrib.zone.Zone;

import playground.michalm.demand.DefaultPersonCreator;

public class PersonCreatorWithRandomTaxiMode extends DefaultPersonCreator {
	private final UniformRandom uniform = RandomUtils.getGlobalUniform();

	private final List<Person> taxiCustomers = new ArrayList<>();
	private final double taxiProbability;

	public PersonCreatorWithRandomTaxiMode(Scenario scenario, double taxiProbability) {
		super(scenario);
		this.taxiProbability = taxiProbability;
	}

	@Override
	public Person createPerson(Plan plan, Zone fromZone, Zone toZone) {
		if (isTaxiCustomer(fromZone, toZone)) {
			taxiCustomers.add(plan.getPerson());
		}

		return super.createPerson(plan, fromZone, toZone);
	}

	public boolean isTaxiCustomer(Zone fromZone, Zone toZone) {
		if (taxiProbability == 0 || !isInternalZone(fromZone) || !isInternalZone(toZone)) {
			return false;
		}

		return uniform.trueOrFalse(taxiProbability);
	}

	private static boolean isInternalZone(Zone zone) {
		String type = zone.getType();
		return type == null || type.toLowerCase().equals("internal");
	}

	public void writeTaxiCustomers(String taxiCustomersFile) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(taxiCustomersFile)))) {
			for (Person p : taxiCustomers) {
				bw.write(p.getId().toString());
				bw.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<String> readTaxiCustomerIds(String taxiCustomersFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(new File(taxiCustomersFile)))) {
			List<String> taxiCustomerIds = new ArrayList<>();

			String line;
			while ((line = br.readLine()) != null) {
				taxiCustomerIds.add(line);
			}

			return taxiCustomerIds;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
