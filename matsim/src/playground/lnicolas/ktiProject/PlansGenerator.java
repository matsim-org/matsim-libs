/* *********************************************************************** *
 * project: org.matsim.*
 * PlansGenerator.java
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

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;

/**
 * First classifies the given reference plans based on whether they contain a shop, work and/or education activity
 * and then assigns these plans to the persons based on their age (e.g. people with age < 6 and age 
 * > 65 do no education), based on whether they are employed or not (the plan of an employed 
 * person must contain a work activity and vice versa) and based on the distribution of the 
 * reference plans. 
 * 
 * @author lnicolas
 *
 */
public class PlansGenerator {

	enum PlanType { noShopNoEdu, noWorkNoShopNoEdu, noShop, noWorkNoShop, noEdu, 
		noWorkNoEdu, noWork, work }
	
	final int shoppingMaxAge = Integer.MAX_VALUE;
	final int shoppingMinAge = 8;
	final int educationMaxAge = 65;
	final int educationMinAge = 6;
	
	public final static String workActType = "w";
	public final static String shopActType = "s";
	public final static String eduActType = "e";
	public final static String homeActType = "h";
	public final static String leisureActType = "l";
	
	public final static String[] actTypes = { workActType, shopActType, eduActType, homeActType,
		leisureActType };
	
	Plan[] referencePlans = null;
	PlanType[] referencePlanTypes = null;
//	ArrayList<Plan> noShopNoEduPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noWorkNoShopNoEduPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noShopPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noWorkNoShopPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noEduPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noWorkNoEduPlans = new ArrayList<Plan>();
//	ArrayList<Plan> noWorkPlans = new ArrayList<Plan>();
//	ArrayList<Plan> workPlans = new ArrayList<Plan>();
	
	
	public PlansGenerator(Plans population, Plan[] referencePlans) {
		this.referencePlans = referencePlans;
		referencePlanTypes = new PlanType[referencePlans.length];
		for (int i = 0; i < referencePlans.length; i++) {
			referencePlanTypes[i] = getPlanType(referencePlans[i]);
		}
		// Drawn random number...
		Gbl.random.nextInt();
//		initPlanLists(referencePlans);
	}

//	private void initPlanLists(Plan[] referencePlans) {
//		for (Plan p : referencePlans) {
//			PlanType pt = getPlanType(p);
//				switch (pt) {
//					case noShopNoEdu: 
//						noShopNoEduPlans.add(p);
//					case noWorkNoShopNoEdu:
//						noWorkNoShopNoEduPlans.add(p);
//					case noEdu:
//						noEduPlans.add(p);
//					case noWorkNoEdu:
//						noWorkNoEduPlans.add(p);
//					case noShop:
//						noShopPlans.add(p);
//					case noWorkNoShop:
//						noWorkNoShopPlans.add(p);
//					case
//			case SUNDAY: System.out.println("Weekends are best.");
//					     break;
//					     
//			default:	 System.out.println("Midweek days are so-so.");
//					     break;
//			if (pt == PlanType.noShopNoEdu) {
//				noShopNoEduPlans.add(p);
//			}
//			if (containsEdu == false) {
//				noEduPlans.add(p);
//			}
//			if (containsShop == false) {
//				noShopPlans.add(p);
//			}
//			if (containsWork == false) {
//				noWorkPlans.add(p);
//			} else {
//				workPlans.add(p);
//			}
//		}
//	}
	
	private PlanType getPlanType(Plan p) {
		boolean containsShop = false;
		boolean containsEdu = false;
		boolean containsWork = false;
		BasicPlan.ActIterator it = p.getIteratorAct();
		while (it.hasNext()) {
			BasicAct act = it.next();
			if (act.getType().equals(workActType)) {
				containsWork = true;
			} else if (act.getType().equals(eduActType)) {
				containsEdu = true;
			} else if (act.getType().equals(shopActType)) {
				containsShop = true;
			}
		}
		if (containsEdu == false && containsShop == false) {
			if (containsWork == false) {
				return PlanType.noWorkNoShopNoEdu;
			} else {
				return PlanType.noShopNoEdu;
			}
		}
		if (containsEdu == false) {
			if (containsWork == false) {
				return PlanType.noWorkNoEdu;
			} else {
				return PlanType.noEdu;
			}
		}
		if (containsShop == false) {
			if (containsWork == false) {
				return PlanType.noWorkNoShop;
			} else {
				return PlanType.noShop;
			}
		}
		if (containsWork == false) {
			return PlanType.noWork;
		} else {
			return PlanType.work;
		}
	}

	public void run(Plans population) {
		Collection<Person> persons = population.getPersons().values();
		
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		int endStatus = persons.size() * 4;
		boolean[] personDone = new boolean[persons.size()];
		for (int i = 0; i < personDone.length; i++) {
			personDone[i] = false;
		}
		// Assign plans to persons that do not shop and do no education
		ArrayList<Plan> noWorkNoShopNoEduReferencePlans = new ArrayList<Plan>();
		ArrayList<Plan> noShopNoEduReferencePlans = new ArrayList<Plan>();
		for (int i = 0; i < referencePlans.length; i++) {
			switch (referencePlanTypes[i]) {
				case noShopNoEdu:
					noShopNoEduReferencePlans.add(referencePlans[i]);
					break;
				case noWorkNoShopNoEdu:
					noWorkNoShopNoEduReferencePlans.add(referencePlans[i]);
					break;
			}
		}
		int noWorkNoShopNoEduPersonCount = 0;
		int noShopNoEduPersonCount = 0;
		int i = 0;
		for (Person p : persons) {
			int age = p.getAge();
			// balmermi: if (age == [0-5])
			if (age < shoppingMinAge || age > shoppingMaxAge
					&& (age < educationMinAge || age > educationMaxAge)) {
				Plan oldPlan = null;
				if (p.getEmployed().equals("yes")) {
					int planIndex = Gbl.random.nextInt(noShopNoEduReferencePlans.size());
					oldPlan = noShopNoEduReferencePlans.get(planIndex);
					noShopNoEduPersonCount++;
				} else {
					int planIndex = Gbl.random.nextInt(noWorkNoShopNoEduReferencePlans.size());
					oldPlan = noWorkNoShopNoEduReferencePlans.get(planIndex);
					noWorkNoShopNoEduPersonCount++;
				}
				Plan newPlan = new Plan(p);
				newPlan.copyPlan(oldPlan);
				setPlan(p, newPlan);
				personDone[i] = true;
			}
			i++;
			if (i % (endStatus / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		
		ArrayList<Integer> noWorkNoShopReferencePlanIndexes = new ArrayList<Integer>();
		ArrayList<Integer> noShopReferencePlanIndexes = new ArrayList<Integer>();
		for (i = 0; i < referencePlans.length; i++) {
			switch (referencePlanTypes[i]) {
				case noShop:
				case noShopNoEdu:
					noShopReferencePlanIndexes.add(i);
					break;
				case noWorkNoShop:
				case noWorkNoShopNoEdu:
					noWorkNoShopReferencePlanIndexes.add(i);
					break;
			}
		}
		int noWorkNoShopPersonCount = 0;
		int noShopPersonCount = 0;
		i = 0;
		for (Person p : persons) {
			int age = p.getAge();
			// balmermi: age == [6-7]
			if (personDone[i] == false && (age < shoppingMinAge || age > shoppingMaxAge)) {
				int planIndex = -1;
				if (p.getEmployed().equals("yes")) {
					planIndex = noShopReferencePlanIndexes.get(
							Gbl.random.nextInt(noShopReferencePlanIndexes.size()));
					if (referencePlanTypes[planIndex] == PlanType.noShopNoEdu) {
						noShopNoEduPersonCount++;
					} else {
						noShopPersonCount++;
					}
				} else {
					planIndex = noWorkNoShopReferencePlanIndexes.get(
							Gbl.random.nextInt(noWorkNoShopReferencePlanIndexes.size()));
					if (referencePlanTypes[planIndex] == PlanType.noWorkNoShopNoEdu) {
						noWorkNoShopNoEduPersonCount++;
					} else {
						noWorkNoShopPersonCount++;
					}
				}
				Plan newPlan = new Plan(p);
				newPlan.copyPlan(referencePlans[planIndex]);
				setPlan(p, newPlan);
				personDone[i] = true;
			}
			i++;
			if (i % (endStatus / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		ArrayList<Integer> noWorkNoEduReferencePlanIndexes = new ArrayList<Integer>();
		ArrayList<Integer> noEduReferencePlanIndexes = new ArrayList<Integer>();
		for (i = 0; i < referencePlans.length; i++) {
			switch (referencePlanTypes[i]) {
				case noEdu:
				case noShopNoEdu:
					noEduReferencePlanIndexes.add(i);
					break;
				case noWorkNoEdu:
				case noWorkNoShopNoEdu:
					noWorkNoEduReferencePlanIndexes.add(i);
					break;
			}
		}
		int noWorkNoEduPersonCount = 0;
		int noEduPersonCount = 0;
		i = 0;
		for (Person p : persons) {
			int age = p.getAge();
			// balmermi: age == [66-xx]
			if (personDone[i] == false && (age < educationMinAge || age > educationMaxAge)) {
				int planIndex = -1;
				if (p.getEmployed().equals("yes")) {
					planIndex = noEduReferencePlanIndexes.get(
						Gbl.random.nextInt(noEduReferencePlanIndexes.size()));
					if (referencePlanTypes[planIndex] == PlanType.noShopNoEdu) {
						noShopNoEduPersonCount++;
					} else {
						noEduPersonCount++;
					}
				} else {
					planIndex = noWorkNoEduReferencePlanIndexes.get(
						Gbl.random.nextInt(noWorkNoEduReferencePlanIndexes.size()));
					if (referencePlanTypes[planIndex] == PlanType.noWorkNoShopNoEdu) {
						noWorkNoShopNoEduPersonCount++;
					} else {
						noWorkNoEduPersonCount++;
					}
				}
				Plan newPlan = new Plan(p);
				newPlan.copyPlan(referencePlans[planIndex]);
				setPlan(p, newPlan);
				personDone[i] = true;
			}
			i++;
			if (i % (endStatus / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		
		ArrayList<Integer> noWorkReferencePlanIndexes = new ArrayList<Integer>();
		ArrayList<Integer> workReferencePlanIndexes = new ArrayList<Integer>();
		for (i = 0; i < referencePlans.length; i++) {
			switch (referencePlanTypes[i]) {
				case work:
				case noShop:
				case noEdu:
				case noShopNoEdu:
					workReferencePlanIndexes.add(i);
					break;
				case noWork:
				case noWorkNoShop:
				case noWorkNoEdu:
				case noWorkNoShopNoEdu:
					noWorkReferencePlanIndexes.add(i);
					break;
			}
		}
		i = 0;
		for (Person p : persons) {
			if (personDone[i] == false) {
				int planIndex = -1;
				if (p.getEmployed().equals("yes")) {
					boolean ok = false;
					while (ok == false) {
						ok = true;
						planIndex = workReferencePlanIndexes.get(
							Gbl.random.nextInt(workReferencePlanIndexes.size()));
						if (referencePlanTypes[planIndex] == PlanType.noShopNoEdu
								&& noShopNoEduPersonCount > 0) {
							noShopNoEduPersonCount--;
							ok = false;
						} else if (referencePlanTypes[planIndex] == PlanType.noShop
								&& noShopPersonCount > 0) {
							noShopPersonCount--;
							ok = false;
						} else if (referencePlanTypes[planIndex] == PlanType.noEdu
								&& noEduPersonCount > 0) {
							noEduPersonCount--;
							ok = false;
						}
					}
				} else {
					boolean ok = false;
					while (ok == false) {
						ok = true;
						planIndex = noWorkReferencePlanIndexes.get(
							Gbl.random.nextInt(noWorkReferencePlanIndexes.size()));
						if (referencePlanTypes[planIndex] == PlanType.noWorkNoShopNoEdu
								&& noWorkNoShopNoEduPersonCount > 0) {
							noWorkNoShopNoEduPersonCount--;
							ok = false;
						} else if (referencePlanTypes[planIndex] == PlanType.noWorkNoShop
								&& noWorkNoShopPersonCount > 0) {
							noWorkNoShopPersonCount--;
							ok = false;
						} else if (referencePlanTypes[planIndex] == PlanType.noWorkNoEdu
								&& noWorkNoEduPersonCount > 0) {
							noWorkNoEduPersonCount--;
							ok = false;
						}
					}
				}
				Plan newPlan = new Plan(p);
				newPlan.copyPlan(referencePlans[planIndex]);
				setPlan(p, newPlan);
				personDone[i] = true;
			}
			i++;
			if (i % (endStatus / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		
		int workCount = 0;
		int noWorkCount = 0;
		for (Person p : persons) {
			// Each person has only one activity plan
//			if (p.getPlans().size() != 1) {
//				Gbl.errorMsg("Person " + p.getId() + " has " + p.getPlans().size() + " plans!");
//			}
			Plan plan = p.getSelectedPlan();
			PlanType pType = getPlanType(plan);
			switch (pType) {
				case work:
				case noShop:
				case noEdu:
				case noShopNoEdu:
					workCount++;
					break;
				case noWork:
				case noWorkNoShop:
				case noWorkNoEdu:
				case noWorkNoShopNoEdu:
					noWorkCount++;
					break;
			}
		}
		System.out.println("workCount: " + workCount + ", noWorkCount: " + noWorkCount);
	}
	
//	private ArrayList<Double> getWorkPlansDistribution(Plans population,
//			ArrayList<Plan> workPlans, int noShopNoEduPersonCount, 
//			int noShopPersonCount, int noEduPersonCount) {
//		int personCount = population.getPersons().values().size();
//		TreeMap<Integer, Integer> householdsPerSize = new TreeMap<Integer, Integer>();
//		int householdCount = 0;
//		int maxHouseholdSize = 0;
//
//		Iterator<Entry<String, HouseholdInformation>> it = hInfos.entrySet().iterator();
//		while (it.hasNext()) {
//			Entry<String, HouseholdInformation> entry = it.next();
//
//			int hSize = entry.getValue().getPersonCount();
//			int hSizeCount = 0;
//			if (householdsPerSize.containsKey(hSize)) {
//				hSizeCount = householdsPerSize.get(hSize);
//			}
//			householdsPerSize.put(hSize, hSizeCount + 1);
//			if (hSize > maxHouseholdSize) {
//				maxHouseholdSize = hSize;
//			}
//			householdCount++;
//		}
//		
//		ArrayList<Double> hSizeDistr = new ArrayList<Double>();
//		// create household size distribution
//		double householdFrac = 0;
//		for (int i = 0; i <= maxHouseholdSize; i++) {
//			if (householdsPerSize.containsKey(i)) {
//				householdFrac += (100 * householdsPerSize.get(i)) / (double)householdCount;
//			}
//			hSizeDistr.add(householdFrac);
//		}
//		
//		return hSizeDistr;
//	}
	
	private void setPlan(Person p, Plan newPlan) {
		// Get home coords of person, assuming that the current person already has
		// a plan that contains a home activity with coords
		Act initialAct = (Act)p.getPlans().get(0).getActsLegs().get(0);
		BasicPlan.ActIterator actIt = newPlan.getIteratorAct();
		while (actIt.hasNext()) {
			Act act = (Act) actIt.next();
			if (act.getType().equals(homeActType)) {
				act.setCoord(initialAct.getCoord());
			}
		}
		p.getPlans().set(0, newPlan);
		p.setSelectedPlan(newPlan);
	}
}
