/* *********************************************************************** *
 * project: org.matsim.*
 * MicroCensus2005ActChainGenerator.java
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.utils.misc.Time;

public class MicroCensus2005ActChainGenerator {

	final static String actUnknownType = "X";
	
	final static String actMiscType = "m";
	
	final static String actHomeType = "h";
	
	public static Plans run(String filename) {
		System.out.println("Creating population...");
		Plans population = createPlans(filename);
		System.out.println("done (" + population.getPersons().size() + " persons)");
		
		revisePlans(population);
			
		return population;
	}
	
	public static void revisePlans(Plans population) {
		System.out.print("Converting unknown start acts to home acts..."); System.out.flush();
		int cnt = convertUnknownStartActivities(population);
		System.out.println("done (" + cnt + " plans converted)");
		
		System.out.print("Merging consecutive acts of same type..."); System.out.flush();
		cnt = mergeConsecutiveEqualActs(population);
		System.out.println("done (" + cnt + " acts merged)");

		System.out.print("Removing unknown acts..."); System.out.flush();
		cnt = removePlansWithUnknownActivities(population);
		System.out.println("done (" + cnt + " plans removed)");
		
		System.out.print("Removing misc acts..."); System.out.flush();
		cnt = removePlansWithMiscActivities(population);
		System.out.println("done (" + cnt + " plans removed)");
		
		System.out.print("Removing plans with only one act..."); System.out.flush();
		cnt = removeOneActPlans(population);
		System.out.println("done (" + cnt + " plans removed)");
		
		System.out.println(population.getPersons().size() + " persons left in population");
	}

	private static int removeOneActPlans(Plans population) {
		Iterator<Map.Entry<Id, Person> > personIt = population.getPersons().entrySet().iterator();
		int removeCnt = 0;
		
		while (personIt.hasNext()) {
			Map.Entry<Id, Person> entry = personIt.next();
			Iterator<Plan> planIt = entry.getValue().getPlans().iterator();
			while (planIt.hasNext()) {
				Plan plan = planIt.next();
				if (plan.getActsLegs().size() == 1) {
					planIt.remove();
					removeCnt++;
				}
			}
			if (entry.getValue().getPlans().size() == 0) {
				personIt.remove();
			}
		}
		
		return removeCnt;
	}

	private static int mergeConsecutiveEqualActs(Plans population) {
		Collection<Person> persons = population.getPersons().values();
		int mergeCnt = 0;
		
		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				ArrayList<Object> actLegs = plan.getActsLegs();
				Act lastAct = null;
				for (int i = 0; i < actLegs.size();) {
					Act currentAct = (Act)actLegs.get(i);
					
					if (i > 0 && currentAct.getType().equals(lastAct.getType())) {
						// Remove currentAct
						double dur = lastAct.getDur();
						if (currentAct.getDur() != Time.UNDEFINED_TIME) {
							dur += currentAct.getDur();
						}
						lastAct.setDur(dur);
//						lastAct.setEndTime(currentAct.getEndTime());
						// Remove leg and act
						plan.removeLeg(i - 1);
//						actLegs.remove(i - 1);
//						// Remove act
//						actLegs.remove(i - 1);
						mergeCnt++;
					} else {
						 i += 2;
						 lastAct = currentAct;
					}
				}
			}
		}
		
		return mergeCnt;
	}

	private static int removePlansWithMiscActivities(Plans population) {
		return removePlansByActivityType(population, actMiscType);
	}

	private static int removePlansWithUnknownActivities(Plans population) {
		return removePlansByActivityType(population, actUnknownType);
	}
	
	private static int removePlansByActivityType(Plans population, String typeToRemove) {
		Iterator<Map.Entry<Id, Person> > it =
			population.getPersons().entrySet().iterator();
		int removeCnt = 0;
		
		while (it.hasNext()) {
			Map.Entry<Id, Person> entry = it.next();
			for (Plan plan : entry.getValue().getPlans()) {
				BasicPlanImpl.ActIterator actIt = plan.getIteratorAct();
				while (actIt.hasNext()) {
					if (actIt.next().getType().equals(typeToRemove)) {
						it.remove();
						removeCnt++;
						break;
					}
				}
			}
			if (entry.getValue().getPlans().size() == 0) {
				it.remove();
			}
		}
		
		return removeCnt;
	}

	/**
	 * Converts acts of type X to acts of type h if they are at the beginning
	 * and converts the last act of the corresponding plan to type h as well.
	 * Converts the last act of plans whose first act is of type h to h as well.
	 * 
	 * @param population
	 */
	private static int convertUnknownStartActivities(Plans population) {
		Collection<Person> persons = population.getPersons().values();
		int convertCnt = 0;
		
		for (Person person : persons) {
			for (Plan plan : person.getPlans()) {
				ArrayList<Object> actLegs = plan.getActsLegs(); 
				Act firstAct = ((Act)actLegs.get(0));
				if (firstAct.getType().equals(actUnknownType)
						|| firstAct.getType().equals(actHomeType)) {
					Act lastAct = ((Act)actLegs.get(actLegs.size() - 1));
					lastAct.setType(actHomeType.intern());
					firstAct.setType(actHomeType.intern());
					convertCnt++;
				}
			}
		}
		
		return convertCnt;
	}

	public static Plans createPlans(String filename) {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		
		Plans population = new Plans();
		try {
			FileReader fileReader = new FileReader(filename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String currentLine;
			int lineIndex = 0;
			int lineCount = DatapulsPopulationGenerator.getLineCount(filename);
			int currLegNo = 0;
			int currPersonNo = 0;
			int currHHId = 0;
			int personId = -1;
			Person person = null;
			Plan plan = null;
			// Skip the first line
			currentLine = bufferedReader.readLine();
//			String[] tmpEntries = currentLine.split("\t", -1);
//			String tmp = "wmittel";
//			for (int i = 0; i < tmpEntries.length; i++) {
//				if (tmpEntries[i].equals(tmp)) {
//					System.out.println(tmp + ": position " + i);
//					System.exit(0);
//				}
//			}
			String[] lastEntries = null;
			int invalidActCount = 0;
			int invalidHomeActCount = 0;
			int actCount = 0;
			int homeEndingActCount = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {
				String[] entries = currentLine.split("\t", -1);
				int hhId = Integer.parseInt(entries[0].trim());
				int personNr = Integer.parseInt(entries[1].trim());
				int legNo = Integer.parseInt(entries[3].trim());

				if (hhId != currHHId || personNr != currPersonNo) {
					currHHId = hhId;
					currPersonNo = personNr;
					currLegNo = 1;
					if (plan != null) {
						Act act = getAct(lastEntries, null);
						if (act == null) {
							invalidHomeActCount++;
							act = getHomeAct(lastEntries);
						}
						if (act.getType().equals(actHomeType)) {
							homeEndingActCount++;
						}
						plan.addAct(act);
					}
					
					personId++;
					person = new Person(new IdImpl(Integer.toString(personId)), null, 0, null, null, null);
					plan = new Plan(person);
					population.addPerson(person);
					person.addPlan(plan);
					plan.addAct(getHomeAct(entries));
					plan.addLeg(getLeg(entries));
				} else {
					if (legNo != currLegNo + 1) {
						Gbl.errorMsg("Invalid leg no " + legNo + " for person (id=" + personId + ") no "
								+ currPersonNo + " in household " + currHHId + " (should be "
								+ (currLegNo + 1) + ")");
					}
					currLegNo++;
					
					Act act = getAct(lastEntries, entries);
					if (act != null) {
						plan.addAct(act);
						plan.addLeg(getLeg(entries));
					} else {
						invalidActCount++;
					}
					actCount++;
				}
				lastEntries = entries;
				
				lineIndex++;
				if (lineIndex % (lineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			bufferedReader.close();
			
			Act act = getAct(lastEntries, null);
			if (act == null) {
				invalidHomeActCount++;
				act = getHomeAct(lastEntries);
			}
			if (act.getType().equals(actHomeType)) {
				homeEndingActCount++;
			}
			plan.addAct(act);
			
			System.out.println(invalidActCount + " of " + actCount + " acts are invalid, " + 
					invalidHomeActCount + " home returning acts are invalid, "
					+ homeEndingActCount + " acts end at home");
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		
		return population;
	}

	private static Leg getLeg(String[] entries) {
		String legNo = entries[3].trim();
		int modeIndex = Integer.parseInt(entries[52].trim());
		String legMode = null;
		if (modeIndex == 2) {
			legMode = "train";
		} else if (modeIndex == 3) {
			legMode = "bus";
		} else if (modeIndex == 5) {
			legMode = "tram";
		} else if (modeIndex == 6) {
			legMode = "bus";
		} else if (modeIndex == 7) {
			legMode = "pt";
		} else if (modeIndex == 8) {
			legMode = "bus";
		} else if (modeIndex == 9) {
			legMode = "car";
		} else if (modeIndex == 10) {
			legMode = "car";
		} else if (modeIndex == 11) {
			legMode = "ride";
		} else if (modeIndex == 12) {
			legMode = "motorbike";
		} else if (modeIndex == 13) {
			legMode = "motorbike";
		} else if (modeIndex == 14) {
			legMode = "bike";
		} else if (modeIndex == 15) {
			legMode = "walk";
		} else if (modeIndex == 16) {
			legMode = "miv";
		} else if (modeIndex == 17) {
			legMode = "undef";
		} else {
			legMode = "undef";
		}
		int depTime = Integer.parseInt(entries[5].trim());
		int travTime = Integer.parseInt(entries[48].trim());
		int arrTime = Integer.parseInt(entries[41].trim());
		return new Leg(legNo, legMode, Integer.toString(depTime * 60),
				Integer.toString(travTime * 60), Integer.toString(arrTime * 60));
	}

	private static Act getHomeAct(String[] entries) {
		int legStartTime = Integer.parseInt(entries[5].trim());
		double xStartCoord = Double.parseDouble(entries[18].trim());
		double yStartCoord = Double.parseDouble(entries[19].trim());
		double xHomeCoord = Double.parseDouble(entries[6].trim());
		double yHomeCoord = Double.parseDouble(entries[7].trim());
		String actType = actUnknownType.intern();
		if (xStartCoord == xHomeCoord && yStartCoord == yHomeCoord
				&& xStartCoord != 0 && yStartCoord != 0) {
			actType = actHomeType.intern();
		}
		return new Act(actType, xHomeCoord, yHomeCoord,
				null, "0", Integer.toString(legStartTime * 60),
				Integer.toString(legStartTime * 60), null);
	}
	
	private static Act getAct(String[] formerEntries, String[] nextEntries) {
		int typeIndex = Integer.parseInt(formerEntries[55].trim());
		String actType = null;
		double xCoord = Double.parseDouble(formerEntries[30].trim());
		double yCoord = Double.parseDouble(formerEntries[31].trim());
		double xHomeCoord = Double.parseDouble(formerEntries[6].trim());
		double yHomeCoord = Double.parseDouble(formerEntries[7].trim());
		if (xCoord == xHomeCoord && yCoord == yHomeCoord
				&& xCoord != 0 && yCoord != 0) {
			actType = actHomeType.intern();
		} else if (typeIndex == 1) {
			actType = "c";
		} else if (typeIndex == 2 || typeIndex == 6 || typeIndex == 7) {
			actType = "w";
		} else if (typeIndex == 3) {
			actType = "e";
		} else if (typeIndex == 4 || typeIndex == 5) {
			actType = "s";
		} else if (typeIndex == 8) {
			actType = "l";
		} else if (typeIndex == 11) {
			actType = actHomeType.intern();
		} else if (typeIndex == 9 || typeIndex == 10 || typeIndex == 12) {
			actType = actMiscType.intern();
		} else {
			actType = actUnknownType.intern();
		}
		int startTime = Integer.parseInt(formerEntries[41].trim());
		if (nextEntries != null) {
			int endTime = Integer.parseInt(nextEntries[5].trim());
			return new Act(actType, xCoord, yCoord,
					null, Integer.toString(startTime * 60), Integer.toString(endTime * 60),
					Integer.toString((endTime - startTime) * 60), null);
		} else {
			return new Act(actType, xCoord, yCoord,
					null, Integer.toString(startTime * 60),
					null, null, null);
		}
	}
	
	public static void writeActChainDistribution(Plans population, String filename)  {
		writeActChainDistribution(getPlanArray(population), filename);
	}

	private static void writeActChainDistribution(Plan[] plans, String filename) {
		TreeMap<String, Integer> actChainDist = new TreeMap<String, Integer>();

		for (Plan plan : plans) {
			String actChain = "";
			BasicPlanImpl.ActIterator it = plan.getIteratorAct();
			while (it.hasNext()) {
				actChain += it.next().getType();
			}
			int cnt = 0;
			if (actChainDist.containsKey(actChain)) {
				cnt = actChainDist.get(actChain);
			}
			actChainDist.put(actChain, cnt + 1);
		}
		
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			Iterator<Map.Entry<String, Integer>> it = actChainDist.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = it.next();
				out.write(entry.getKey() + "\t" + entry.getValue() + "\n");
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
		System.out.println("Activity chain distribution written to " + filename);
	}
	
	public static void writeActChainDistributionWorkNoWork(Plans population, String filename)  {
		writeActChainDistributionWorkNoWork(getPlanArray(population), filename);
	}
	

	private static void writeActChainDistributionWorkNoWork(Plan[] plans, String filename) {
		TreeMap<String, Integer> actWorkChainDist = new TreeMap<String, Integer>();
		TreeMap<String, Integer> actNoWorkChainDist = new TreeMap<String, Integer>();

		for (Plan plan : plans) {
			String actChain = "";
			BasicPlanImpl.ActIterator it = plan.getIteratorAct();
			while (it.hasNext()) {
				actChain += it.next().getType();
			}
			int cnt = 0;
			if (containsWork(plan)) {
				if (actWorkChainDist.containsKey(actChain)) {
					cnt = actWorkChainDist.get(actChain);
				}
				actWorkChainDist.put(actChain, cnt + 1);
			} else {
				if (actNoWorkChainDist.containsKey(actChain)) {
					cnt = actNoWorkChainDist.get(actChain);
				}
				actNoWorkChainDist.put(actChain, cnt + 1);
			}
		}
		
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			Iterator<Map.Entry<String, Integer>> workIt = actWorkChainDist.entrySet().iterator();
			Iterator<Map.Entry<String, Integer>> noWorkIt = actNoWorkChainDist.entrySet().iterator();
			while (workIt.hasNext() || noWorkIt.hasNext()) {
				if (workIt.hasNext()) {
					Map.Entry entry = workIt.next();
					out.write(entry.getKey() + "\t" + entry.getValue() + "\t");
				} else {
					out.write("\t" + "\t");
				}
				if (noWorkIt.hasNext()) {
					Map.Entry entry = noWorkIt.next();
					out.write(entry.getKey() + "\t" + entry.getValue() + "\n");
				} else {
					out.write("\t" + "\n");
				}
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
				+ e.getMessage());
		}
		System.out.println("Activity chain distribution partitioned by work/no work chains written to " + filename);
	}

	public static Plan[] getPlanArray(Plans population) {
		int planCount = 0;
		for (Person person : population.getPersons().values()) {
			planCount += person.getPlans().size();
		}
		
		Plan[] plans = new Plan[planCount];
		int i = 0;
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				plans[i++] = plan;
			}
		}
		
		return plans;
	}
	
	private static boolean containsWork(Plan plan) {
		BasicPlanImpl.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			BasicAct act = it.next();
			if (act.getType().equals("w")) {
				return true;
			}
		}
		return false;
	}
}
