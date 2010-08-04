/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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

package playground.balmermi.datapuls.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;

public class PlansCreateFromDataPuls {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCreateFromDataPuls.class);
	private final String infile;
	private final Random random = MatsimRandom.getRandom();
	private final Population censusPopulation;
	private final ArrayList<QuadTree<Person>> censusPersonGroups;
	private final Knowledges censusKnowledges;
	private final ActivityFacilitiesImpl datapulsFacilities;
	private final Map<String,QuadTree<ActivityFacilityImpl>> datapulsFacilityGroups;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromDataPuls(String infile, ScenarioImpl censusScenario, ActivityFacilitiesImpl datapulsFacilities) {
		log.info("init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.censusPopulation = censusScenario.getPopulation();
		this.censusKnowledges = censusScenario.getKnowledges();
		this.censusPersonGroups = buildPersonGroups(this.censusPopulation);
		this.datapulsFacilities = datapulsFacilities;
		this.datapulsFacilityGroups = buildFacilityGroups(this.datapulsFacilities);
		random.nextInt();
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private ArrayList<QuadTree<Person>> buildPersonGroups(Population population) {
		log.info("  building a quadtree for each person group...");
		log.info("    calc spatial extent...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Person p : population.getPersons().values()) {
			Coord c = this.datapulsFacilities.getFacilities().get(((PlanImpl) p.getPlans().get(0)).getFirstActivity().getFacilityId()).getCoord();
			if (c.getX() < minx) { minx = c.getX(); }
			if (c.getY() < miny) { miny = c.getY(); }
			if (c.getX() > maxx) { maxx = c.getX(); }
			if (c.getY() > maxy) { maxy = c.getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		log.info("    => xrange("+ minx+","+maxx+"); yrange("+miny+","+maxy+")");
		log.info("    done.");

		ArrayList<QuadTree<Person>> qts = new ArrayList<QuadTree<Person>>(10);
		for (int i=0; i<10; i++) { qts.add(new QuadTree<Person>(minx,miny,maxx,maxy)); }

		log.info("    assinging persons to their group...");
		for (Person pp : population.getPersons().values()) {
			PersonImpl p = (PersonImpl) pp;
			Coord c = this.datapulsFacilities.getFacilities().get(((PlanImpl) p.getPlans().get(0)).getFirstActivity().getFacilityId()).getCoord();
			if (p.getSex().equals("m")) {
				if (p.getAge()<7) { qts.get(0).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<15) { qts.get(1).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<18) { qts.get(2).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<66) { qts.get(3).put(c.getX(),c.getY(),p); }
				else { qts.get(4).put(c.getX(),c.getY(),p); }
			}
			else if (p.getSex().equals("f")) {
				if (p.getAge()<7) { qts.get(5).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<15) { qts.get(6).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<18) { qts.get(7).put(c.getX(),c.getY(),p); }
				else if (p.getAge()<66) { qts.get(8).put(c.getX(),c.getY(),p); }
				else { qts.get(9).put(c.getX(),c.getY(),p); }
			}
			else { throw new RuntimeException("pid="+p.getId()+": gender not known."); }
		}
		for (int i=0; i<qts.size(); i++) {
			log.info("    => group "+i+": "+qts.get(i).size()+" persons assigned.");
		}
		log.info("    done.");
		log.info("  done.");
		return qts;
	}

	//////////////////////////////////////////////////////////////////////

	private Map<String,QuadTree<ActivityFacilityImpl>> buildFacilityGroups(ActivityFacilitiesImpl facilities) {
		log.info("  building a quadtree for each activity option group...");
		String[] types = { "education_higher","education_kindergarten","education_other","education_primary",
				"education_secondary","home","leisure","shop","tta","work_sector2","work_sector3" };
		Map<String,QuadTree<ActivityFacilityImpl>> facilityGroups = new TreeMap<String,QuadTree<ActivityFacilityImpl>>();
		for (int i=0; i<types.length; i++) {
			log.info("    building a quadtree for type '"+types[i]+"'...");
			double minx = Double.POSITIVE_INFINITY;
			double miny = Double.POSITIVE_INFINITY;
			double maxx = Double.NEGATIVE_INFINITY;
			double maxy = Double.NEGATIVE_INFINITY;
			for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
				if (f.getActivityOptions().get(types[i]) != null) {
					if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
					if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
					if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
					if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
				}
			}
			minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
			log.info("    => type="+types[i]+": xrange("+minx+","+maxx+"); yrange("+miny+","+maxy+")");
			QuadTree<ActivityFacilityImpl> qt = new QuadTree<ActivityFacilityImpl>(minx,miny,maxx,maxy);
			for (ActivityFacilityImpl f : facilities.getFacilities().values()) {
				if (f.getActivityOptions().get(types[i]) != null) { qt.put(f.getCoord().getX(),f.getCoord().getY(),f); }
			}
			log.info("    => "+qt.size()+" facilities of type="+types[i]+" added.");
			facilityGroups.put(types[i],qt);
			log.info("    => number of quad trees: "+facilityGroups.size());
			log.info("    done.");
		}
		log.info("  done.");
		return facilityGroups;
	}

	//////////////////////////////////////////////////////////////////////

	private final void parse(Population population, Knowledges kn) {
		log.info("  creating plans from "+infile+"...");
		int line_cnt = 1;
		try {
			FileReader fr = new FileReader(infile);
			BufferedReader br = new BufferedReader(fr);

			String curr_line = null;
			while ((curr_line = br.readLine()) != null) {
				// CDBID  VORNAME  NACHNAME  ZUNAME  STRASSE  HNR  PLZ  ORT  ADRZUSATZ  JR  UMZDAT  SPRACHE  SEX  HAUSHALT  TELEFON  MOBILE  NO_PUB  RT_KOORD  HOCH_KOORD  LINKID
				// 0      1        2         3       4        5    6    7    8          9   10      11       12   13        14       15      16      17        18          19
				String[] entries = curr_line.split("\t", -1);

				Id id = new IdImpl(entries[0].trim());
				int age;
				try { age = 2005-Integer.parseInt(entries[9].trim()); }
				catch (Exception e) { age = random.nextInt(81); }
				if (age < 0) { age = 0; }
				int gender = Integer.parseInt(entries[12].trim());
				if (gender == 0) { gender = random.nextInt(2)+1; }
				String sex = null;
				if (gender == 1) { sex = "m"; }
				else if (gender == 2) { sex = "f"; }
				else { throw new RuntimeException("line "+line_cnt+": gender is not 0,1 or 2."); }
				Id fid = new IdImpl(entries[19].trim());
				ActivityFacilityImpl af = datapulsFacilities.getFacilities().get(fid);
				if (af == null) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" not found in facilities."); }
				if (af.getActivityOptions().size() != 1) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" must have only one activity option."); }
				ActivityOptionImpl a = af.getActivityOptions().get("home");
				if (a == null) { throw new RuntimeException("line "+line_cnt+": fid="+fid+" does not contain 'home'."); }

				PersonImpl p = (PersonImpl)population.getFactory().createPerson(id);
				KnowledgeImpl k = kn.getKnowledgesByPersonId().get(p.getId());
				if (k != null) { throw new RuntimeException("pid="+p.getId()+": knowledge already exist."); }
				k = kn.getFactory().createKnowledge(p.getId(),null);
				kn.getKnowledgesByPersonId().put(p.getId(),k);
				k.addActivityOption(a,true);
				population.addPerson(p);
				p.setAge(age);
				p.setSex(sex);
				line_cnt++;
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////

	private final void assignCensus2datapuls(Population population, Knowledges kn) {
		log.info("  assing Census demand to datapuls population...");
		double maxDistance = 0;
		for (Person pp : population.getPersons().values()) {
			PersonImpl p = (PersonImpl) pp;
			Coord c = kn.getKnowledgesByPersonId().get(p.getId()).getActivities("home").get(0).getFacility().getCoord();
			QuadTree<Person> personGroup = null;
			if (p.getSex().equals("m")) {
				if (p.getAge()<7) { personGroup = censusPersonGroups.get(0); }
				else if (p.getAge()<15) { personGroup = censusPersonGroups.get(1); }
				else if (p.getAge()<18) { personGroup = censusPersonGroups.get(2); }
				else if (p.getAge()<66) { personGroup = censusPersonGroups.get(3); }
				else { personGroup = censusPersonGroups.get(4); }
			}
			else if (p.getSex().equals("f")) {
				if (p.getAge()<7) { personGroup = censusPersonGroups.get(5); }
				else if (p.getAge()<15) { personGroup = censusPersonGroups.get(6); }
				else if (p.getAge()<18) { personGroup = censusPersonGroups.get(7); }
				else if (p.getAge()<66) { personGroup = censusPersonGroups.get(8); }
				else { personGroup = censusPersonGroups.get(9); }
			}
			double distance = 100;
			List<Person> censusPersons = (List<Person>)personGroup.get(c.getX(),c.getY(),distance);
			while (censusPersons.isEmpty()) {
				distance = 2*distance;
				censusPersons = (ArrayList<Person>)personGroup.get(c.getX(),c.getY(),distance);
			}
			// some logging info
			if (maxDistance < distance) { maxDistance = distance; log.info("    pid="+p.getId()+": censusHome2datapulsHome distance="+distance); }

			Person censusPerson = censusPersons.get(random.nextInt(censusPersons.size()));
			mapDemand(p,kn.getKnowledgesByPersonId().get(p.getId()),(PersonImpl) censusPerson,this.censusKnowledges.getKnowledgesByPersonId().get(censusPerson.getId()));
		}
		log.info("  done.");
	}

	//////////////////////////////////////////////////////////////////////

	private final void mapDemand(PersonImpl dPerson, KnowledgeImpl dKnowledge, PersonImpl cPerson, KnowledgeImpl cKnowledge) {
		// demographics
		dPerson.setCarAvail(cPerson.getCarAvail());
		dPerson.setEmployed(cPerson.isEmployed());
		dPerson.setLicence(cPerson.getLicense());
		// travel cards
		if (cPerson.getTravelcards() != null) {
			for (String card : cPerson.getTravelcards()) { dPerson.addTravelcard(card); }
		}
		// desires
		Desires dDesires = dPerson.createDesires(null);
		for (String type : cPerson.getDesires().getActivityDurations().keySet()) {
			dDesires.putActivityDuration(type,cPerson.getDesires().getActivityDurations().get(type));
		}
		// knowledge
		for (ActivityOptionImpl cActivityOption : cKnowledge.getActivities(true)) {
			if (!cActivityOption.getType().equals("home")) {
				ActivityFacilityImpl cFacility = cActivityOption.getFacility();
				ActivityFacilityImpl dFacility = this.datapulsFacilityGroups.get(cActivityOption.getType()).get(cFacility.getCoord().getX(),cFacility.getCoord().getY());
				if (dFacility == null) { throw new RuntimeException("dpid="+dPerson.getId()+", cpid="+cPerson.getId()+", cfid="+cFacility.getId()+": no dFacility found."); }
				double distance = CoordUtils.calcDistance(cFacility.getCoord(),dFacility.getCoord());
				if (distance > 500.0) {
					log.warn("dpid="+dPerson.getId()+", cpid="+cPerson.getId()+", cfid="+cFacility.getId()+", dfid="+dFacility.getId()+", acttype="+cActivityOption.getType()+": distance="+distance+" > 500 meters.");
				}
				ActivityOptionImpl dActivityOption = new ActivityOptionImpl(cActivityOption.getType(),dFacility);
				dKnowledge.addActivityOption(dActivityOption,true);
			}
		}
		for (ActivityOptionImpl cActivityOption : cKnowledge.getActivities(false)) {
			if (!cActivityOption.getType().equals("home")) {
				ActivityFacilityImpl cFacility = cActivityOption.getFacility();
				ActivityFacilityImpl dFacility = this.datapulsFacilityGroups.get(cActivityOption.getType()).get(cFacility.getCoord().getX(),cFacility.getCoord().getY());
				if (dFacility == null) { throw new RuntimeException("dpid="+dPerson.getId()+", cpid="+cPerson.getId()+", cfid="+cFacility.getId()+": no dFacility found."); }
				double distance = CoordUtils.calcDistance(cFacility.getCoord(),dFacility.getCoord());
				if (distance > 500.0) {
					log.warn("dpid="+dPerson.getId()+", cpid="+cPerson.getId()+", cfid="+cFacility.getId()+", dfid="+dFacility.getId()+", acttype="+cActivityOption.getType()+": distance="+distance+" > 500 meters.");
				}
				ActivityOptionImpl dActivityOption = new ActivityOptionImpl(cActivityOption.getType(),dFacility);
				dKnowledge.addActivityOption(dActivityOption,false);
			}
		}
		// plan
		if (cPerson.getPlans().size() != 1) { throw new RuntimeException("cpid="+cPerson.getId()+": does not have one plan"); }
		Plan cPlan = cPerson.getPlans().get(0);
		PlanImpl dPlan = dPerson.createAndAddPlan(true);
		dPlan.copyPlan(cPlan);
		dPlan.setPerson(dPerson);
		for (PlanElement e : dPlan.getPlanElements()) {
			if (e instanceof ActivityImpl) {
				ActivityImpl a = (ActivityImpl)e;
				ArrayList<ActivityOptionImpl> acts = dKnowledge.getActivities(a.getType());
				if (acts.isEmpty()) { throw new RuntimeException("pid="+dPerson.getId()+", aType="+a.getType()+": not defined in knowledge!"); }
				ActivityFacilityImpl f = acts.get(random.nextInt(acts.size())).getFacility();
				a.setCoord(f.getCoord());
				a.setFacilityId(f.getId());
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(Population population, Knowledges kn) {
		log.info("running " + this.getClass().getName() + " module...");
		parse(population,kn);
		assignCensus2datapuls(population,kn);
		log.info("done.");
	}
}
