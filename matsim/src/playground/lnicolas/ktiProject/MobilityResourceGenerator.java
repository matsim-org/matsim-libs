/* *********************************************************************** *
 * project: org.matsim.*
 * MobilityResourceGenerator.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.World;
import org.matsim.world.Zone;

/**
 * Determines the mobility resource tools (car availability, ch-GA and ch-HT ownership)
 * for a person based on its attributes (like age, sex etc.), information about its
 * household and geographic information (i.e. information about the municipality the
 * person lives in) based on a logit model with parameters computed by Biogeme.
 * @author lnicolas
 */
public class MobilityResourceGenerator extends Income2000Generator {

	UtilityComputer[] mobilityUtilityComputers;

	final int alternativeCount;

	World world = null;

	TreeMap<IdI, MunicipalityInformation> municipalityInfo = null;

	public MobilityResourceGenerator(World world,
			TreeMap<IdI, MunicipalityInformation> municipalityInfo) {
		super(world, municipalityInfo);

		this.world = world;
		this.municipalityInfo = municipalityInfo;

		alternativeCount = 6;

		mobilityUtilityComputers = new UtilityComputer[alternativeCount];

		mobilityUtilityComputers[0] = new Alternative0();
		mobilityUtilityComputers[1] = new Alternative1();
		mobilityUtilityComputers[2] = new Alternative2();
		mobilityUtilityComputers[3] = new Alternative3();
		mobilityUtilityComputers[4] = new Alternative4();
		mobilityUtilityComputers[5] = new Alternative5();

		Gbl.random.nextDouble(); // draw one because of strange "not-randomness" in the first draw...
	}

	/**
	 * Determines the mobility resource tools (car availability, ch-GA and ch-HT ownership)
	 * for the given person based on its attributes (like age, sex etc.), information about its
	 * household [{@code hInfo}] and geographic [{@code zone}] information (i.e. information
	 * about the municipality the person lives in).
	 * @param person
	 * @param hInfo Household information about the person.
	 * @param zone The zone the person lives in.
	 */
	public void addMobilityInformation(Person person, HouseholdI hInfo, Zone zone) {
		double[] alternativeProbability = new double[alternativeCount];

		MunicipalityInformation mInfo = municipalityInfo.get(zone.getId());

		double sumOfProbabilities = 0;
		for (int i = 0; i < alternativeCount; i++) {
			double[] utilities = new double[2];
			utilities[0] = mobilityUtilityComputers[i].computeYesUtility(person, hInfo, mInfo);
			utilities[1] = mobilityUtilityComputers[i].computeNoUtility(person, hInfo, mInfo);

			alternativeProbability[i] = getLogitProbability(utilities[0], utilities);

			sumOfProbabilities += alternativeProbability[i];
		}

		double r = Gbl.random.nextDouble() * sumOfProbabilities;
		int index = 0;
		sumOfProbabilities = alternativeProbability[index];
		// Pick the right mobility resource alternative
		while (r >= sumOfProbabilities) {
			index++;
			sumOfProbabilities += alternativeProbability[index];
		}

		addMobilityInformation(person, index);
	}

	private void addMobilityInformation(Person person, int index) {
		/* Index		Mobility information
		 * 0			No Car + No Public Transport Tickets
		 * 1			Car + No Public Transport Tickets
		 * 2			No Car + National Tickets + Regional Tickets
		 * 3			No Car + Half-Fare Discount Tickets
		 * 4			Car + National Tickets + Regional Tickets
		 * 5			Car + Half-Fare Discount Tickets */
		final String generalabonnement = "ch-GA";
		final String halbtax = "ch-HT";

		if (index == 0) {
			setHasCar(person, false);
		} else if (index == 1) {
			setHasCar(person, true);
		} else if (index == 2) {
			setHasCar(person, false);
			person.addTravelcard(generalabonnement);
		} else if (index == 3) {
			setHasCar(person, false);
			person.addTravelcard(halbtax);
		} else if (index == 4) {
			setHasCar(person, true);
			person.addTravelcard(generalabonnement);
		} else if (index == 5) {
			setHasCar(person, true);
			person.addTravelcard(halbtax);
		}
	}

	private void setHasCar(Person person, boolean hasCar) {
		final String carAvail = "always";
		final String noCarAvail = "never";

		if (hasCar) {
			person.setCarAvail(carAvail);
		} else {
			person.setCarAvail(noCarAvail);
		}
	}

	protected double getLogitProbability(double referenceUtility, double[] utilities) {
		double expSumOfAlternatives = 0;
		for (double utility : utilities) {
			expSumOfAlternatives += Math.exp(utility);
		}

		return Math.exp(referenceUtility) / expSumOfAlternatives;
	}

	protected double isSwiss(Person p) {
		if (p.getNationality() == null
				|| p.getNationality().equals("swiss") == false) {
			return 0;
		} else {
			return 1;
		}
	}

	private double getAge2(Person p) {
		return getAge(p) * getAge(p);
	}

	protected double getAge(Person p) {
		return p.getAge();
	}

	protected double getAgeLn(Person p) {
		if (p.getAge() <= 0) {
			return 0;
		}
		return Math.log(p.getAge());
	}

	private double getIncomeLn(Person p, HouseholdI hInfo) {
		if (hInfo.getIncome() <= 0) {
			return 0;
		}
		return Math.log(hInfo.getIncome() / 12.0); // monthly income
	}

	private double getIncome2(HouseholdI h) {
		return getIncome1000PerMonth(h) * getIncome1000PerMonth(h);
	}

	protected double getIncome1000PerMonth(HouseholdI h) {
		return h.getIncome() / 1000.0 / 12.0;
	}

	protected double getHHKidCount(HouseholdI h) {
		return h.getKidCount();
	}

	private double getIsKidInHH(HouseholdI h) {
		if (getHHKidCount(h) > 0) {
			return 1;
		} else {
			return 0;
		}
	}

	protected double getHHSize(HouseholdI h) {
		return h.getPersonCount();
	}

	protected double getAgeTimesSex(Person p) {
		return getAge(p) * getSex(p);
	}

	private double getSex(Person p) {
		if (p.getSex().equals("m")) {
			return 1;
		} else {
			return 0;
		}
	}

	private double getBenzin95(MunicipalityInformation mInfo) {
		return mInfo.getAvgGasPrice();
	}

	private double getHasLicense(Person p) {
		if (p.getLicense().equals("yes")) {
			return 1;
		} else {
			return 0;
		}
	}

	protected double[] getUrbanizationType(MunicipalityInformation mInfo) {
		double[] urbanization = new double[4];
		for (int i = 0; i < urbanization.length; i++) {
			urbanization[i] = 0;
		}
		int index = mInfo.getUrbanizationIndex() - 1;
		if (index < 4) {
			urbanization[index] = 1;
		}
		return urbanization;
	}

	private double getPopulationDensity(MunicipalityInformation mInfo) {
		return mInfo.getPopulationDensity();
	}

	private double getPopulationCount1000(MunicipalityInformation mInfo) {
		return mInfo.getPersonCount() / 1000;
	}

	static public void scaleDownGAOwnershipFraction(Plans plans, double targetFraction) {
		int popWithGASize = 0;
		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		// Count nof persons with a GA
		Collection<Person> persons = plans.getPersons().values();
		for (Person p : persons) {
			if (p.getTravelcards().contains("ch-GA")) {
				if (p.getAge() <= 6) {
					p.getTravelcards().remove("ch-GA");
				} else {
					popWithGASize++;
				}
			}
		}

		// With a certain probability, remove a person's GA such that we have the desired number
		// of persons owning a f*** GA at the end
		double withGAProb = targetFraction / ((double)popWithGASize / persons.size());
		int targetPopCount = (int)(persons.size() * targetFraction);
		ArrayList<Person> personsWithGA = new ArrayList<Person>();
		Iterator<Person> pIt = persons.iterator();
		int j = 0;
		while (pIt.hasNext() && popWithGASize > targetPopCount) {
			Person p = pIt.next();
			if (p.getTravelcards().contains("ch-GA")) {
				double r = Gbl.random.nextDouble();
				if (r >= withGAProb) {
					p.getTravelcards().remove("ch-GA");
					popWithGASize--;
				} else {
					personsWithGA.add(p);
				}
			}

			j++;
			if (j % (persons.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}

		while (popWithGASize > targetPopCount) {
			int i = Gbl.random.nextInt(personsWithGA.size());
			Person p = personsWithGA.remove(i);
			p.getTravelcards().remove("ch-GA");
			popWithGASize--;
		}
	}

	public static ArrayList<Zone> mapPersonsToZones(
			ArrayList<Person> persons, ArrayList<Zone> zones) {
		ArrayList<Zone> result = new ArrayList<Zone>();
		int i = 0;
		System.out.println("Mapping persons to zones...");
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		for (Person person : persons) {
			CoordI homeCoord = getHomeCoord(person);
			List<Zone> contZones = getContainingZones(homeCoord, zones);
			Zone zone = null;
			if (contZones.size() == 0) {
				zone = getNearestZone(homeCoord, zones);
			} else {
				// Get a random zone
				int zoneIndex = Gbl.random.nextInt(contZones.size());
				zone = contZones.get(zoneIndex);
			}
			result.add(zone);

			i++;
			if (i % (persons.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println("done");

		return result;
	}

	class Alternative5 implements UtilityComputer {
		static final double B_ALT = +3.4072543e-002;
		static final double B_EINK = -2.0305461e-001;
		static final double B_EINK_LN = +1.1263217e+000;
		static final double B_FA = -1.9047369e+001;
		static final double B_HH = +2.9098221e-001;
		static final double B_HH_K_ja = -8.9131401e-001;
		static final double B_NAT = +9.6063730e-001;
		static final double B_P_B95 = +9.0663438e+000;

		/**
		 * Computes the Car + Half-Fare Discount Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P1c_lAlter = getAge(p);
			double P3a_bSchweizerIn = isSwiss(p);

			return B_ALT * P1c_lAlter + B_NAT * P3a_bSchweizerIn;
		}

		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P10_FA = getHasLicense(p);
			double Benzin95 = getBenzin95(m);
			double L2_HHGroesse = getHHSize(h);
			double L2_HH_Kinder = getIsKidInHH(h);
			double HH_Eink_1000 = getIncome1000PerMonth(h);
			double HH_Eink_ln = getIncomeLn(p, h);

			return B_FA * P10_FA + B_P_B95 * Benzin95 + B_HH * L2_HHGroesse
				+ B_HH_K_ja * L2_HH_Kinder + B_EINK * HH_Eink_1000 +
				B_EINK_LN * HH_Eink_ln;
		}
	}

	class Alternative4 implements UtilityComputer {
		static final double B_ALT_LN = -1.8658099e+000;
		static final double B_EINK_LN = -3.3764402e-001;
		static final double B_HH = -2.7182249e-001;
		static final double B_HH_K_ja = +8.3991129e-001;
		static final double B_P_B95 = -1.4125773e+001;
		static final double KONST = +1.9156812e+001;

		/**
		 * Computes the Car + National Tickets + Regional Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P1c_lAlter_ln = getAgeLn(p);

			return B_ALT_LN * P1c_lAlter_ln;
		}

		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double Benzin95 = getBenzin95(m);
			double L2_HHGroesse = getHHSize(h);
			double L2_HH_Kinder = getIsKidInHH(h);
			double HH_Eink_ln = getIncomeLn(p, h);

			return KONST + B_P_B95 * Benzin95 + B_HH * L2_HHGroesse + B_HH_K_ja * L2_HH_Kinder
				+ B_EINK_LN * HH_Eink_ln;
		}
	}

	class Alternative3 implements UtilityComputer {
		static final double B_ALT = -1.5880483e-001;
		static final double B_ALT_LN = +8.0113009e+000;
		static final double B_EINK_LN = -5.2657558e-001;
		static final double B_FA = +2.7577949e+000;
		static final double B_HH = +2.8151987e-001;
		static final double B_P_B95 = +1.8767597e+001;

		/**
		 * Computes the No Car + Half-Fare Discount Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P1c_lAlter = getAge(p);
			double P1c_lAlter_ln = getAgeLn(p);

			return B_ALT * P1c_lAlter + B_ALT_LN * P1c_lAlter_ln;
		}

		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double HH_Eink_ln = getIncomeLn(p, h);
			double L2_HHGroesse = getHHSize(h);

			double P10_FA = getHasLicense(p);
			double Benzin95 = getBenzin95(m);

			return B_FA * P10_FA + B_P_B95 * Benzin95 + B_HH * L2_HHGroesse + B_EINK_LN * HH_Eink_ln;
		}
	}

	class Alternative2 implements UtilityComputer {
		static final double B_EINK = -1.8273032e+000;
		static final double B_EINK_2 = +5.1946166e-002;
		static final double B_EINK_LN = +5.1235599e+000;
		static final double B_FA = +3.3578280e+000;
		static final double B_HH = +3.7503464e-001;
		static final double B_HH_KIND = -4.1780736e-001;
		static final double B_NAT = +6.3093114e-001;
		static final double B_WO_RGV2 = +2.7364136e+000;
		static final double B_WO_RGV3 = +1.7693655e+000;
		static final double B_WO_RGV4 = +1.7284204e+000;
		static final double B_WO_RGV5 = +3.6608704e+000;
		static final double KONST = -3.6122072e+001;

		/**
		 * Computes the No Car + National Tickets + Regional Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P3a_bSchweizerIn = isSwiss(p);

			return B_NAT * P3a_bSchweizerIn;
		}

		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double L2_HHGroesse = getHHSize(h);
			double L2_HHGroesse_Kind = getHHKidCount(h);
			double HH_Eink_1000 = getIncome1000PerMonth(h);
			double HH_Eink_2 = getIncome2(h);
			double HH_Eink_ln = getIncomeLn(p, h);

			double[] urbanization = getUrbanizationType(m);
			double WO_RGverk2 = urbanization[0];
			double WO_RGverk3 = urbanization[1];
			double WO_RGverk4 = urbanization[2];
			double WO_RGverk5 = urbanization[3];
			double P10_FA = getHasLicense(p);

			return KONST + B_FA * P10_FA + B_HH * L2_HHGroesse + B_HH_KIND * L2_HHGroesse_Kind
				+ B_EINK * HH_Eink_1000 + B_EINK_2 * HH_Eink_2 + B_EINK_LN * HH_Eink_ln
				+ B_WO_RGV2 * WO_RGverk2 + B_WO_RGV3 * WO_RGverk3 + B_WO_RGV4 * WO_RGverk4
				+ B_WO_RGV5 * WO_RGverk5;
		}
	}

	class Alternative1 implements UtilityComputer {
		static final double B_ALT = -1.7196375e-001;
		static final double B_ALT_LN = +6.9981124e+000;
		static final double B_A_G = +1.3687082e-002;
		static final double B_EINK = +2.4757859e-001;
		static final double B_EINK_LN = -1.1854674e+000;
		static final double B_NAT = -5.9129810e-001;
		static final double B_WO_RGV2 = -7.8523452e-001;
		static final double B_WO_RGV3 = -1.1028788e+000;
		static final double B_WO_RGV4 = -9.1138311e-001;
		static final double B_WO_RGV5 = -1.4632017e+000;
		static final double KONST = +2.8287292e+001;

		/**
		 * Computes the Car + No Public Transport Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P1c_lAlter = getAge(p);
			double P1c_lAlter_ln = getAgeLn(p);
			double P3a_bSchweizerIn = isSwiss(p);

			double P_Alt_Ges = getAgeTimesSex(p);

			return B_ALT * P1c_lAlter + B_ALT_LN * P1c_lAlter_ln + B_A_G * P_Alt_Ges
				+ B_NAT * P3a_bSchweizerIn;
		}

		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double HH_Eink_1000 = getIncome1000PerMonth(h);
			double HH_Eink_ln = getIncomeLn(p, h);

			double[] urbanization = getUrbanizationType(m);
			double WO_RGverk2 = urbanization[0];
			double WO_RGverk3 = urbanization[1];
			double WO_RGverk4 = urbanization[2];
			double WO_RGverk5 = urbanization[3];

			return KONST + B_EINK * HH_Eink_1000 + B_EINK_LN * HH_Eink_ln
				+ B_WO_RGV2 * WO_RGverk2 + B_WO_RGV3 * WO_RGverk3 + B_WO_RGV4 * WO_RGverk4
				+ B_WO_RGV5 * WO_RGverk5;
		}

	}

	class Alternative0 implements UtilityComputer {

		static final double B_ALT = -2.2606104e-001;
		static final double B_ALT_2 = +2.1671132e-003;
		static final double B_NAT = -1.6172835e+000;
		static final double B_EINK = +1.7607264e+000;
		static final double B_EINK_2 = -4.7946289e-002;
		static final double B_EINK_LN = -4.1934308e+000;
		static final double B_FA = +4.4195238e+000;
		static final double B_WO_BEV = -2.9937595e-002;
		static final double B_WO_BEVD = +1.1858015e-001;
		static final double B_WO_RGV2 = -6.7553297e+000;
		static final double B_WO_RGV3 = -8.7425689e+000;
		static final double B_WO_RGV4 = -8.4469399e+000;
		static final double B_WO_RGV5 = -8.3395366e+000;
		static final double KONST = +2.8863137e+001;

		/**
		 * Computes the No Car + No Public Transport Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeYesUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double P1c_lAlter = getAge(p);
			double P1c_lAlter_2 = getAge2(p);
			double P3a_bSchweizerIn = isSwiss(p);

			return B_ALT * P1c_lAlter + B_ALT_2 * P1c_lAlter_2 + B_NAT * P3a_bSchweizerIn;
		}

		/**
		 * Computes the No Car + No Public Transport Tickets utility for this person
		 * @param p
		 * @return
		 */
		public double computeNoUtility(Person p, HouseholdI h, MunicipalityInformation m) {
			double HH_Eink_1000 = getIncome1000PerMonth(h);
			double HH_Eink_2 = getIncome2(h);
			double HH_Eink_ln = getIncomeLn(p, h);

			double P10_FA = getHasLicense(p);
			double[] urbanization = getUrbanizationType(m);
			double WO_RGverk2 = urbanization[0];
			double WO_RGverk3 = urbanization[1];
			double WO_RGverk4 = urbanization[2];
			double WO_RGverk5 = urbanization[3];
			double WO_Bev_1000 = getPopulationCount1000(m);
			double WO_BevDichte = getPopulationDensity(m);

			return KONST + B_FA * P10_FA + B_EINK * HH_Eink_1000 + B_EINK_2 * HH_Eink_2
				+ B_EINK_LN * HH_Eink_ln + B_WO_RGV2 * WO_RGverk2 + B_WO_RGV3 * WO_RGverk3
				+ B_WO_RGV4 * WO_RGverk4 + B_WO_RGV5 * WO_RGverk5 + B_WO_BEV * WO_Bev_1000
				+ B_WO_BEVD * WO_BevDichte;
		}
	}
}
