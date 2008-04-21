/* *********************************************************************** *
 * project: org.matsim.*
 * LicenseOwnershipGenerator.java
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

package playground.lnicolas.ktiProject;

import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.world.World;
import org.matsim.world.Zone;

/**
 * Determines whether a person owns a driving license or not based on the person's
 * attributes (like age, sex etc.), information about its household and geographic
 * information (i.e. information about the municipality the
 * person lives in) based on a logit model with parameters computed by Biogeme.
 * @author lnicolas
 *
 */
public class LicenseOwnershipGenerator extends MobilityResourceGenerator implements UtilityComputer {

	static final double B_ALT = -1.9398888e-001;
	static final double B_ALT_LN = +6.9777346e+000;
	static final double B_A_G = +2.4028668e-002;
	static final double B_EINK = -1.0186076e-001;
	static final double B_HH = +4.5513247e-001;
	static final double B_HH_KIND = -8.5895102e-001;
	static final double B_NAT = +1.0185754e+000;
	static final double B_WO_RGV2 = -8.8675212e-001;
	static final double B_WO_RGV3 = -1.2863876e+000;
	static final double B_WO_RGV4 = -1.1348476e+000;
	static final double B_WO_RGV5 = -1.6941787e+000;
	static final double KONST = +1.7480172e+001;

	public LicenseOwnershipGenerator(World world,
			TreeMap<Id, MunicipalityInformation> municipalityInfo) {
		super(world, municipalityInfo);
	}

	/**
	 * Determines whether a person owns a driving license or not based on the person's
	 * attributes (like age, sex etc.), information about its household [{@code hInfo}]
	 * and geographic [{@code zone}] information (i.e. information about the municipality
	 * the person lives in).
	 * @param person
	 * @param hInfo Household information about the person.
	 * @param zone The zone the person lives in.
	 */
	public void addLicenseInformation(Person person, HouseholdI hInfo, Zone zone) {

		if (person.getAge() >= 18) {
			double[] utilities = new double[2];

			MunicipalityInformation mInfo = municipalityInfo.get(zone.getId());

			utilities[0] = computeYesUtility(person, hInfo, mInfo);
			utilities[1] = computeNoUtility(person, hInfo, mInfo);

			double hasLicenseProbability = getLogitProbability(utilities[0], utilities);

			double r = Gbl.random.nextDouble();
			if (r < hasLicenseProbability) {
				person.setLicense("yes");
			} else {
				person.setLicense("no");
			}
		} else {
			person.setLicense("no");
		}
	}

	public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
		double L2_HHGroesse = getHHSize(h);
		double L2_HHGroesse_Kind = getHHKidCount(h);
		double HH_Eink_1000 = getIncome1000PerMonth(h);
		double[] urbanization = getUrbanizationType(m);
		double WO_RGverk2 = urbanization[0];
		double WO_RGverk3 = urbanization[1];
		double WO_RGverk4 = urbanization[2];
		double WO_RGverk5 = urbanization[3];

		return KONST + B_HH * L2_HHGroesse + B_HH_KIND * L2_HHGroesse_Kind
			+ B_EINK * HH_Eink_1000 + B_WO_RGV2 * WO_RGverk2 + B_WO_RGV3 * WO_RGverk3
			+ B_WO_RGV4 * WO_RGverk4 + B_WO_RGV5 * WO_RGverk5;
	}

	public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
		double P1c_lAlter = getAge(p);
		double P3a_bSchweizerIn = isSwiss(p);
		double P1c_lAlter_ln = getAgeLn(p);
		double P_Alt_Ges = getAgeTimesSex(p);

		return B_ALT * P1c_lAlter + B_ALT_LN * P1c_lAlter_ln + B_A_G * P_Alt_Ges
			+ B_NAT * P3a_bSchweizerIn;
	}
}
