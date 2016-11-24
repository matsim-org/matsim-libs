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
package org.matsim.contrib.parking.parkingchoice.PC2.scoring;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.lib.DebugLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.parkingchoice.lib.utils.expr.Parser;
import org.matsim.contrib.parking.parkingchoice.lib.utils.expr.SyntaxException;
import org.matsim.core.population.PersonUtils;

public class ParkingBetas  extends AbstractParkingBetas{

	private String parkingWalkBeta;
	private Parser parkingCostBetaParser;
	private HashMap<Id, Parser> parkingWalkBetaCache;
	private DoubleValueHashMap<Id> parkingCostBetaCache;
	private DoubleValueHashMap<Id> income;

	public ParkingBetas(DoubleValueHashMap<Id> income){
		this.income = income;
		parkingWalkBetaCache = new HashMap<Id, Parser>();
		parkingCostBetaCache = new DoubleValueHashMap<Id>();
	}

	public void setParkingWalkBeta(String parkingWalkBeta) {
		if (parkingWalkBeta != null) {
			this.parkingWalkBeta = parkingWalkBeta;
		} else {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	public void setParkingCostBeta(String parkingCostBeta) {
		if (parkingCostBeta != null) {
			this.parkingCostBetaParser = new Parser(parkingCostBeta);
		} else {
			DebugLib.stopSystemAndReportInconsistency();
		}
	}

	public double getParkingWalkBeta(Person person, double activityDurationInSeconds) {
		Id personId = person.getId();
		Parser parser = null;
		if (!parkingWalkBetaCache.containsKey(personId)) {
			Parser pTmp = new Parser(parkingWalkBeta);
			Person persImpl = person;

			int isMale = 1;
			if (PersonUtils.getSex(persImpl) != null) {
				isMale = !PersonUtils.getSex(persImpl).contains("f") ? 1 : 0;
			}

			pTmp.setVariable("isMale", isMale);

			int age = PersonUtils.getAge(persImpl);

			pTmp.setVariable("ageInYears", age);
			parkingWalkBetaCache.put(personId, pTmp);
		}
		parser = parkingWalkBetaCache.get(personId);

		parser.setVariable("activityDurationInSeconds", activityDurationInSeconds);

		double result = 0;

		try {
			result = parser.parse();
		} catch (SyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}

	// TODO: in controller, add income to person
	public double getParkingCostBeta(Person person) {
		Id personId = person.getId();
		if (!parkingCostBetaCache.containsKey(personId)) {
			parkingCostBetaParser.setVariable("income", income.get(personId));
			try {
				parkingCostBetaCache.put(personId, parkingCostBetaParser.parse());
			} catch (SyntaxException e) {
				e.printStackTrace();
				DebugLib.stopSystemAndReportInconsistency();
			}
		}

		return parkingCostBetaCache.get(personId);
	}

}
