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

package playground.balmermi.census2000v2.modules;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.Location;
import org.matsim.world.Zone;

import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.data.Households;

public class PlansCreateFromCensus2000 {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PlansCreateFromCensus2000.class);

	private final String infile;
	private final Households households;
	private final Facilities facilities;
	private final Map<String,QuadTree<Facility>> fqts = new HashMap<String, QuadTree<Facility>>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCreateFromCensus2000(final String infile, final Households households, final Facilities facilities) {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		this.infile = infile;
		this.households = households;
		this.facilities = facilities;
		this.buildQuadTrees();
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private init methods
	//////////////////////////////////////////////////////////////////////

	private void buildQuadTrees() {
		log.info("      building a quadtree for each activity type...");
		String[] types = { CAtts.ACT_HOME, CAtts.ACT_W2, CAtts.ACT_W3, CAtts.ACT_EKIGA, CAtts.ACT_EPRIM, 
				CAtts.ACT_ESECO, CAtts.ACT_EHIGH, CAtts.ACT_EOTHR, CAtts.ACT_S1, 
				CAtts.ACT_S2, CAtts.ACT_S3, CAtts.ACT_S4, CAtts.ACT_S5, 
				CAtts.ACT_SOTHR, CAtts.ACT_LC, CAtts.ACT_LG, CAtts.ACT_LS };
		for (int i=0; i<types.length; i++) {
			log.info("        building a quadtree for type '"+types[i]+"'...");
			double minx = Double.POSITIVE_INFINITY;
			double miny = Double.POSITIVE_INFINITY;
			double maxx = Double.NEGATIVE_INFINITY;
			double maxy = Double.NEGATIVE_INFINITY;
			for (Facility f : this.facilities.getFacilities().values()) {
				if (f.getActivity(types[i]) != null) {
					if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
					if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
					if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
					if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
				}
			}
			minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
			log.info("        type="+types[i]+": xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
			QuadTree<Facility> qt = new QuadTree<Facility>(minx,miny,maxx,maxy);
			for (Facility f : this.facilities.getFacilities().values()) {
				if (f.getActivity(types[i]) != null) { qt.put(f.getCenter().getX(),f.getCenter().getY(),f); }
			}
			log.info("        "+qt.size()+" facilities of type="+types[i]+" added.");
			this.fqts.put(types[i],qt);
			log.info("        done.");
		}
		log.info("      done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private Activity chooseEducActBasedOnSchoolType(final Person p, String act_type) {
		if (act_type.equals(CAtts.ACT_EKIGA) || act_type.equals(CAtts.ACT_EPRIM)) { // assign nearest act
			QuadTree<Facility> qt = this.fqts.get(act_type);
			Facility home_f = p.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).getFacility();
			Facility educ_f = qt.get(home_f.getCenter().getX(),home_f.getCenter().getY());
			return educ_f.getActivity(act_type);
		}
		else if (act_type.equals(CAtts.ACT_ESECO)) { // search in home zone and expanding
			List<Activity> acts = new ArrayList<Activity>();
			Facility home_f = p.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).getFacility();
			Zone zone = (Zone)home_f.getUpMapping().values().iterator().next();
			Coord max = new CoordImpl(((Zone)zone).getMax());
			Coord min = new CoordImpl(((Zone)zone).getMin());
			while (acts.isEmpty()) {
				QuadTree<Facility> qt = this.fqts.get(act_type);
				List<Facility> fs = new ArrayList<Facility>();
				qt.get(min.getX(),min.getY(),max.getX(),max.getY(),fs);
				if (!fs.isEmpty()) {
					for (Facility f : fs) {
						Activity a = f.getActivity(act_type);
						for (int i=0; i<a.getCapacity(); i++) { acts.add(a); }
					}
					Activity act = acts.get(MatsimRandom.random.nextInt(acts.size()));
					if (act == null) { Gbl.errorMsg("That should not happen!"); }
					return act;
				}
				max.setX(max.getX()+1000.0);
				max.setY(max.getY()+1000.0);
				min.setX(min.getX()-1000.0);
				min.setY(min.getY()-1000.0);
			}
			Gbl.errorMsg("pid="+p.getId()+": should never happen!"); 
			return null;
		}
		else if (act_type.equals(CAtts.ACT_EOTHR) || act_type.equals(CAtts.ACT_EHIGH)) { // assigning weighted
			QuadTree<Facility> qt = this.fqts.get(act_type);
			List<Facility> fs = new ArrayList<Facility>();
			qt.get(qt.getMinEasting()-1.0,qt.getMinNorthing()-1.0,qt.getMaxEasting()+1.0,qt.getMaxNorthing()+1.0,fs);
			List<Activity> acts = new ArrayList<Activity>();
			for (Facility f : fs) {
				Activity a = f.getActivity(act_type);
				for (int i=0; i<a.getCapacity(); i++) { acts.add(a); }
			}
			Activity act = acts.get(MatsimRandom.random.nextInt(acts.size()));
			if (act == null) { Gbl.errorMsg("That should not happen!"); }
			return act;
		}
		else { Gbl.errorMsg("pid="+p.getId()+": should never happen!"); return null; }
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void chooseEducFacility(final Person p, String[] entries) {
		
		// 1. get the school activity type
		int gegw = Integer.parseInt(entries[CAtts.I_GEGW]);
		String act_type = null;
		if (gegw == -8) { // no educ at the moment, but still assign mandatory education!
			if (p.getAge()<5) { return; } // too young for school
			else if (p.getAge()<7) { act_type = CAtts.ACT_EKIGA; } // duty
			else if (p.getAge()<15) { act_type = CAtts.ACT_EPRIM; }  // duty
			else { return; } // rest ignore
		}
		else if (gegw == -9) {
			if (p.getAge()<5) { return; } // too young for school
			else if (p.getAge()<7) { act_type = CAtts.ACT_EKIGA; }
			else { Gbl.errorMsg("pid="+p.getId()+": data inconsistency: person should be 'schulpflichtig'"); }
		}
		else if (gegw == -7) {
			if (p.getAge()<5) { return; } // too young for school
			else if (p.getAge()<7) { act_type = CAtts.ACT_EKIGA; } // duty
			else if (p.getAge()<15) { act_type = CAtts.ACT_EPRIM; }  // duty
			else { return; } // rest ignore
		}
		else if ((gegw == 11) || (gegw == 12)) { act_type = CAtts.ACT_EPRIM; }
		else if ((gegw == 21) || (gegw == 22) || (gegw == 23)) { act_type = CAtts.ACT_ESECO; }
		else if ((gegw == 31) || (gegw == 32)) { act_type = CAtts.ACT_EOTHR; }
		else if ((gegw == 33) || (gegw == 34)) { act_type = CAtts.ACT_EHIGH; }
		else { Gbl.errorMsg("pid="+p.getId()+",gegw="+gegw+": not known!"); }
		
		// 3. gathering the educ actvity
		int sgde = Integer.parseInt(entries[CAtts.I_SGDE]);
		Location zone = Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY).getLocation(new IdImpl(sgde));
		if (zone != null) {
			List<Activity> acts = new ArrayList<Activity>();
			for (Location l : zone.getDownMapping().values()) {
				Activity a = ((Facility)l).getActivity(act_type);
				if (a != null) { acts.add(a); }
			}
			if (!acts.isEmpty()) {
				List<Activity> acts_weighted = new ArrayList<Activity>();
				for (Activity a : acts) { for (int i=0; i<a.getCapacity(); i++) { acts_weighted.add(a); } }
				Activity act = acts_weighted.get(MatsimRandom.random.nextInt(acts_weighted.size()));
				if (act == null) { Gbl.errorMsg("That should not happen!"); }
				p.getKnowledge().addActivity(act, false); // set activity in given zone
			}
			else {
				log.debug("        pid="+p.getId()+", act_type="+act_type+", zone="+zone.getId()+": no facilities for educ found in that zone!");
				Coord max = new CoordImpl(((Zone)zone).getMax());
				Coord min = new CoordImpl(((Zone)zone).getMin());
				while (acts.isEmpty()) {
					max.setX(max.getX()+1000.0);
					max.setY(max.getY()+1000.0);
					min.setX(min.getX()-1000.0);
					min.setY(min.getY()-1000.0);
					log.debug("          searching for educ facilities in area: min="+min+", max="+max+"...");
					QuadTree<Facility> qt = this.fqts.get(act_type);
					List<Facility> fs = new ArrayList<Facility>();
					qt.get(min.getX(),min.getY(),max.getX(),max.getY(),fs);
					if (!fs.isEmpty()) {
						for (Facility f : fs) {
							Activity a = f.getActivity(act_type);
							for (int i=0; i<a.getCapacity(); i++) { acts.add(a); }
						}
						Activity act = acts.get(MatsimRandom.random.nextInt(acts.size()));
						if (act == null) { Gbl.errorMsg("That should not happen!"); }
						p.getKnowledge().addActivity(act, false); // set activity in expanded given zone
					}
				}
			}
		}
		else {
			log.debug("        pid="+p.getId()+", act_type="+act_type+": no educ zone defined! Assigning according act_type!");
			Activity act = this.chooseEducActBasedOnSchoolType(p,act_type);
			if (act == null) { Gbl.errorMsg("That should not happen!"); }
			p.getKnowledge().addActivity(act, false);
			p.getKnowledge().setDesc(p.getKnowledge().getDesc()+"("+CAtts.P_SGDE+":"+sgde+")");
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void chooseWorkFacility(final Person p, final String agde, final String pber) {

		// 1. examine the job type
		// possible job numbers: job = -1,0,1,2,3,4,5,6,7,81,82,83,84,85,86,87,9
		int job = Integer.parseInt(pber);
		if ((job == -6)|| (job == -7)) { job = 0; } // unspecified jobs
		else if ((job == -8)|| (job == -9)) { job = -1; } // no job
		else if (job > 0) {
			job = Integer.parseInt(pber.substring(0,1));
			if (job == 8) {
				job = Integer.parseInt(pber.substring(0,2));
				if (job > 87) { Gbl.errorMsg("pber="+pber+" not known!"); }
			}
		}
		else { Gbl.errorMsg("pber="+pber+" not known!"); }

		// 2. assigning the activity types to the job types
		List<String> w_acts = new ArrayList<String>();
		List<String> o_acts = new ArrayList<String>();
		if (job == -1) { return; } // no job.: stop
		else if (job == 0) { w_acts.add(CAtts.ACT_W2); w_acts.add(CAtts.ACT_W3); }
		else if (job == 1) { w_acts.add(CAtts.ACT_W2); }
		else if (job == 2) { w_acts.add(CAtts.ACT_W2); }
		else if (job == 3) { w_acts.add(CAtts.ACT_W2); w_acts.add(CAtts.ACT_W3); }
		else if (job == 4) { w_acts.add(CAtts.ACT_W2); }
		else if (job == 5) { w_acts.add(CAtts.ACT_W2); o_acts.add(CAtts.ACT_S1); o_acts.add(CAtts.ACT_S2); o_acts.add(CAtts.ACT_S3); o_acts.add(CAtts.ACT_S4); o_acts.add(CAtts.ACT_S5); o_acts.add(CAtts.ACT_SOTHR); }
		else if (job == 6) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_LG); }
		else if (job == 7) { w_acts.add(CAtts.ACT_W3); }
		else if (job == 81) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_LC); o_acts.add(CAtts.ACT_EHIGH); o_acts.add(CAtts.ACT_EOTHR); }
		else if (job == 82) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_LC); o_acts.add(CAtts.ACT_EHIGH); o_acts.add(CAtts.ACT_EOTHR); }
		else if (job == 83) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_EKIGA); o_acts.add(CAtts.ACT_EPRIM); }
		else if (job == 84) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_EKIGA); o_acts.add(CAtts.ACT_EPRIM); o_acts.add(CAtts.ACT_ESECO); }
		else if (job == 85) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_EHIGH); o_acts.add(CAtts.ACT_EOTHR); }
		else if (job == 86) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_EHIGH); o_acts.add(CAtts.ACT_EOTHR); o_acts.add(CAtts.ACT_LS); }
		else if (job == 87) { w_acts.add(CAtts.ACT_W3); o_acts.add(CAtts.ACT_EHIGH); o_acts.add(CAtts.ACT_EOTHR); o_acts.add(CAtts.ACT_LS); o_acts.add(CAtts.ACT_LC); }
		else if (job == 9) { w_acts.add(CAtts.ACT_W2); w_acts.add(CAtts.ACT_W3); }
		else { Gbl.errorMsg("job="+job+" not known!"); }
		
		// 3. gathering the work zone
		int i_agde = Integer.parseInt(agde);
		Location zone = Gbl.getWorld().getLayer(Municipalities.MUNICIPALITY).getLocation(new IdImpl(agde));
		if (zone == null) {
			if ((i_agde == -7) || (i_agde > 8100)) {
				log.info("        pid="+p.getId()+", jobnr="+job+": no work muni defined");
				Facility home_f = p.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).getFacility();
				zone = (Zone)home_f.getUpMapping().values().iterator().next();
				log.info("        => work muni = home muni (zid="+agde+") assigned.");
				p.getKnowledge().setDesc(p.getKnowledge().getDesc()+"("+CAtts.P_AGDE+":"+agde+")");
			}
			else { Gbl.errorMsg("pid="+p.getId()+", jobnr="+job+": Zone id="+agde+" not found!"); }
		}

		// 4. try to get work facilities in the given zone with other suitable activity types (i.e. teacher --> work & education facility)
		List<Facility> facs = new ArrayList<Facility>();
		for (Location l : zone.getDownMapping().values()) {
			Facility f = (Facility)l;
			Set<String> f_acts = f.getActivities().keySet();
			boolean has_work = false; for (String w_act : w_acts) { if (f_acts.contains(w_act)) { has_work = true; } }
			boolean has_othr = false; for (String o_act : o_acts) { if (f_acts.contains(o_act)) { has_othr = true; } }
			if (has_work && has_othr) { facs.add(f); }
		}
		
		// 5. no appropriate word facility found. therefore, assigning work facilities in that zone without further act types
		if (facs.isEmpty()) {
			log.trace("        pid="+p.getId()+", jobnr="+job+": no facilities for work with additional acts found!");
			// getting all possible work facilities with additional other act type
			for (Location l : zone.getDownMapping().values()) {
				Facility f = (Facility)l;
				Set<String> f_acts = f.getActivities().keySet();
				boolean has_work = false; for (String w_act : w_acts) { if (f_acts.contains(w_act)) { has_work = true; } }
				if (has_work) { facs.add(f); }
			}
		}

		// 6. if no work facilities in that zone found, try to find some by extension of the search area until something found
		if (facs.isEmpty()) { log.debug("        pid="+p.getId()+", jobnr="+job+", zone="+zone.getId()+": no facilities for work found in that zone!"); }
		Coord max = new CoordImpl(((Zone)zone).getMax());
		Coord min = new CoordImpl(((Zone)zone).getMin());
		while (facs.isEmpty()) {
			max.setX(max.getX()+1000.0);
			max.setY(max.getY()+1000.0);
			min.setX(min.getX()-1000.0);
			min.setY(min.getY()-1000.0);
			log.trace("          searching for facilities in area: min="+min+", max="+max+"...");
			// gathering all work facilities of the given types
			List<Facility> tmp_w_fs = new ArrayList<Facility>();
			for (String act : w_acts) {
				QuadTree<Facility> w_qt = this.fqts.get(act);
				List<Facility> w_fs = new ArrayList<Facility>();
				w_qt.get(min.getX(),min.getY(),max.getX(),max.getY(),w_fs);
				tmp_w_fs.addAll(w_fs);
			}
			if (!tmp_w_fs.isEmpty()) {
				// work facilities found. Gathering all facilities of appropriate additional activities
				List<Facility> tmp_o_fs = new ArrayList<Facility>();
				for (String act : o_acts) {
					QuadTree<Facility> o_qt = this.fqts.get(act);
					List<Facility> o_fs = new ArrayList<Facility>();
					o_qt.get(min.getX(),min.getY(),max.getX(),max.getY(),o_fs);
					tmp_o_fs.addAll(o_fs);
				}
				// intersect the two lists (facilities with work and appropriate additional acts)
				List<Facility> tmp_wo_fs = new ArrayList<Facility>();
				for (Facility f : tmp_w_fs) { if (tmp_o_fs.contains(f)) { tmp_wo_fs.add(f); } }
				if (tmp_wo_fs.isEmpty()) {
					// intersect is empty. use the work list for work facility choice
					facs.addAll(tmp_w_fs);
					log.trace("            added "+facs.size()+" work acts (no additional acts).");
				}
				else {
					// work-other facilities found. use that list for the work facility choice
					facs.addAll(tmp_wo_fs);
					log.trace("            added "+facs.size()+" work acts with additional acts.");
				}
			}
			log.trace("          done.");
		}
		log.trace("          done. # facilities found: " + facs.size());
		if (facs.isEmpty()) { Gbl.errorMsg("THAT SHOULD NEVER HAPPEN!"); }
		
		// 7. choose an activity at a facility (weighted for capacity)
		List<Activity> acts_weighted = new ArrayList<Activity>();
		for (Facility f : facs) {
			for (String a : w_acts) {
				Activity act = f.getActivity(a);
				if (act != null) { for (int i=0; i<act.getCapacity(); i++) { acts_weighted.add(act); } }
			}
		}
		log.trace("        pid="+p.getId()+", jobnr="+job+", acts_weighted size="+acts_weighted.size());
		Activity act_choosen = acts_weighted.get(MatsimRandom.random.nextInt(acts_weighted.size()));
		p.getKnowledge().addActivity(act_choosen, false);
	}
	
	//////////////////////////////////////////////////////////////////////

	private final boolean isEmployed(int ams) {
		if ((11 <= ams) && (ams <= 14)) { return true; }
		else if ((ams == 20) || ((31 <= ams) && (ams <= 35)) || (ams == 4) || (ams == 40)) { return false; }
		else {
			Gbl.errorMsg("wrong value of AMS param!");
			return false;
		}
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void addDemographics(final Person p, final String[] entries, int wkat) {
		Map<String,Object> p_atts = p.getCustomAttributes();

		// adding home knowledge
		Knowledge k = p.getKnowledge();
		if (k == null) { k = p.createKnowledge(""); }
		String desc = "";
		if (wkat == 1) {
			Household hh_w = (Household)p_atts.get(CAtts.HH_W);
			desc = desc+"("+CAtts.HH_W+":"+hh_w.getId()+")";
			k.addActivity(hh_w.getFacility().getActivity(CAtts.ACT_HOME), false);

			Household hh_z = (Household)p_atts.get(CAtts.HH_Z);
			desc = desc+"("+CAtts.HH_Z+":"+hh_z.getId()+")";
			k.addActivity(hh_z.getFacility().getActivity(CAtts.ACT_HOME), false);
		}
		else if (wkat == 3) {
			Household hh_w = (Household)p_atts.get(CAtts.HH_W);
			desc = desc+"("+CAtts.HH_W+":"+hh_w.getId()+")";
			k.addActivity(hh_w.getFacility().getActivity(CAtts.ACT_HOME), false);
		}
		else if (wkat == 4) {
			Household hh_z = (Household)p_atts.get(CAtts.HH_Z);
			desc = desc+"("+CAtts.HH_Z+":"+hh_z.getId()+")";
			k.addActivity(hh_z.getFacility().getActivity(CAtts.ACT_HOME), false);
		}
		else { Gbl.errorMsg("that should not happen!"); }
		k.setDesc(k.getDesc()+desc);

		p.setAge(Integer.parseInt(entries[CAtts.I_ALTJ]));
		if (entries[CAtts.I_GESL].equals("1")) { p.setSex("m"); } else { p.setSex("f"); }
		if (this.isEmployed(Integer.parseInt(entries[CAtts.I_AMS]))) { p.setEmployed("yes"); } else { p.setEmployed("no"); }

		this.chooseWorkFacility(p,entries[CAtts.I_AGDE],entries[CAtts.I_PBER]);
		
		// some consistency checks
		if (p.isEmployed()) {
			if (!p.getKnowledge().getActivityTypes().contains(CAtts.ACT_W2) &&
			    !p.getKnowledge().getActivityTypes().contains(CAtts.ACT_W3)) {
				log.warn("pid="+p.getId()+",employed="+p.isEmployed()+": person does not have work activity!");
			}
			if (p.getAge()<15) {
				log.warn("pid="+p.getId()+",employed="+p.isEmployed()+",age="+p.getAge()+": person is too young for beeing employed!");
			}
		}
		if (p.getAge()<15) {
			if (p.getKnowledge().getActivityTypes().contains(CAtts.ACT_W2) ||
			    p.getKnowledge().getActivityTypes().contains(CAtts.ACT_W3)) {
					log.warn("pid="+p.getId()+",age="+p.getAge()+": person is too young for having work activities!");
			}
		}
		
		this.chooseEducFacility(p,entries);
	}
	
	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) {
		log.info("    running " + this.getClass().getName() + " algorithm...");

		if (!plans.getPersons().isEmpty()) { Gbl.errorMsg("plans DB is not empty!"); }
		plans.setName("created by '" + this.getClass().getName() + "'");

		try {
			FileReader fr = new FileReader(this.infile);
			BufferedReader br = new BufferedReader(fr);
			int line_cnt = 0;
			
			// keep track of the pids which need to be found twice
			HashSet<Id> pids = new HashSet<Id>();

			// Skip header
			String curr_line = br.readLine(); line_cnt++;
			curr_line = br.readLine(); line_cnt++;
			while ((curr_line = br.readLine()) != null) {
				String[] entries = curr_line.split("\t", -1);
				//P_ZGDE  P_GEBAEUDE_ID  P_HHNR  P_PERSON_ID  P_WKAT  P_GEM2  P_PARTNR
				//1       2              3       5            10      11      12
				
				// check for existing household
				Id hhnr = new IdImpl(entries[CAtts.I_HHNR]);
				Household hh = households.getHousehold(hhnr);
				if (hh == null) { Gbl.errorMsg("Line "+line_cnt+": Household id="+hhnr+" does not exist!"); }

				Id pid = new IdImpl(entries[5]);
				int wkat = Integer.parseInt(entries[CAtts.I_WKAT]);
				int gem2 = Integer.parseInt(entries[CAtts.I_GEM2]);
				int partnr = Integer.parseInt(entries[CAtts.I_PARTNR]);
				
				// error heading
				String e_head = "Line "+line_cnt+": case("+pid+","+wkat+","+gem2+","+partnr+") ";

				// allowed combinations:
				// wkat  gem2  partnr  occurence  meaning
				// 1     -9    -9      1/person   person does have only one household (z and w)
				// 3     -7    -7      1/person   person is ONLY part of the 'wirtschaftliche wohnbevoelkerung' (w)
				// 4     -7    -7      1/person   person is ONLY part of the 'zivilrechtliche wohnbevoelkerung' (z)
				// 3     id    id      2/person   person is part of w and z. current line reflects w
				// 4     id    id      2/person   person is part of w and z. current line reflects z
				if ((wkat == 1) && (gem2 == -9) && (partnr == -9)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
					p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 3) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
					p_atts.put(CAtts.HH_Z,null);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 4) && (gem2 == -7) && (partnr == -7)) {
					Person p = plans.getPerson(pid);
					if (p != null) { Gbl.errorMsg(e_head+"person alread exists!"); }
					p = new Person(pid);
					Map<String,Object> p_atts = p.getCustomAttributes();
					p_atts.put(CAtts.HH_W,null);
					p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
					plans.addPerson(p);
					this.addDemographics(p,entries,wkat);
				}
				else if ((wkat == 3) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
						p_atts.put(CAtts.HH_Z,null);
						plans.addPerson(p);
						this.addDemographics(p,entries,wkat);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(CAtts.HH_W) == null) && (p_atts.get(CAtts.HH_Z) != null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(CAtts.HH_W,hh); hh.getPersonsW().put(p.getId(),p);
						this.addDemographics(p,entries,wkat);
					}
				}
				else if ((wkat == 4) && ((1 <= gem2)&&(gem2 <= 7011)) && ((1 <= partnr)&&(partnr <= 999999999))) {
					Person p = plans.getPerson(new IdImpl(partnr));
					if (p == null) {
						if (!pids.add(pid)) { Gbl.errorMsg(e_head+"partner person not found, but pid found in the set!"); }
						p = new Person(pid);
						Map<String,Object> p_atts = p.getCustomAttributes();
						p_atts.put(CAtts.HH_W,null);
						p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
						plans.addPerson(p);
						this.addDemographics(p,entries,wkat);
					}
					else {
						if (!pids.remove(new IdImpl(partnr))) { Gbl.errorMsg(e_head+"partner person found, but not found in the set!"); }
						Map<String,Object> p_atts = p.getCustomAttributes();
						if (!((p_atts.get(CAtts.HH_W) != null) && (p_atts.get(CAtts.HH_Z) == null))) { Gbl.errorMsg(e_head+"something is wrong!"); }
						p_atts.put(CAtts.HH_Z,hh); hh.getPersonsZ().put(p.getId(),p);
						this.addDemographics(p,entries,wkat);
					}
				}
				else { Gbl.errorMsg(e_head+"not allowed!"); }

				// progress report
				if (line_cnt % 100000 == 0) {
					log.info("    Line " + line_cnt + ": # persons = " + plans.getPersons().size() + "; # pids = " + pids.size());
				}
				line_cnt++;
			}
			br.close();
			fr.close();
			
			// some info
			log.info("    "+plans.getPersons().size()+" persons created! (#pids="+pids.size()+")");
			int same_cnt = 0; int diff_cnt = 0; int wonly_cnt = 0; int zonly_cnt = 0; int diff_f_cnt = 0;
			for (Person p : plans.getPersons().values()) {
				Map<String,Object> p_atts = p.getCustomAttributes();
				if ((p_atts.get(CAtts.HH_W)==null)&&(p_atts.get(CAtts.HH_Z)==null)) { Gbl.errorMsg("WAHHH!"); }
				else if ((p_atts.get(CAtts.HH_W)==null)&&(p_atts.get(CAtts.HH_Z)!=null)) { zonly_cnt++; }
				else if ((p_atts.get(CAtts.HH_W)!=null)&&(p_atts.get(CAtts.HH_Z)==null)) { wonly_cnt++; }
				else {
					if (p_atts.get(CAtts.HH_W).equals(p_atts.get(CAtts.HH_Z))) { same_cnt++; }
					else {
						diff_cnt++;
						Facility f1 = ((Household)p_atts.get(CAtts.HH_W)).getFacility();
						Facility f2 = ((Household)p_atts.get(CAtts.HH_Z)).getFacility();
						if (!f1.equals(f2)) { diff_f_cnt++; }
					}
				}
			}
			log.info("    # civil only          = " + zonly_cnt);
			log.info("    # econo only          = " + wonly_cnt);
			log.info("    # one household only  = " + same_cnt);
			log.info("    # two households      = " + diff_cnt);
			log.info("    # two hh facilities   = " + diff_f_cnt);
			log.info("    left over pids:");
			for (Id pid : pids) { log.info("    "+pid); }
		} catch (Exception e) {
			Gbl.errorMsg(e);
		}
		log.info("    done.");
	}
}
