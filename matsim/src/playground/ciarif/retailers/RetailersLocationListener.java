/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesLoadCalculator.java.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.ciarif.retailers;

/**
 *
 *
 * @author ciarif
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.IdImpl;
import org.matsim.controler.Controler;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;


public class RetailersLocationListener implements StartupListener, BeforeMobsimListener {

	private Retailers retailers;
	private final RetailersSummaryWriter rs;
	private final PlansSummaryTable pst;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRouteLandmarks pcrl = null;
	private final String facilityIdFile;

	public RetailersLocationListener(final String facilityIdFile, final String retailerSummaryFileName) {
		this.facilityIdFile = facilityIdFile;
		rs = new RetailersSummaryWriter(retailerSummaryFileName, this.retailers);
		pst = new PlansSummaryTable ("output/triangle/output_Persons.txt");
	}

	public RetailersLocationListener(final String retailerSummaryFileName) {
		this(null,retailerSummaryFileName);
	}

	public void notifyStartup(StartupEvent event) {
		Controler controler = event.getControler();

		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRouteLandmarks(controler.getNetwork(),preprocess,timeCostCalc, timeCostCalc);
		
		// define all given retailers
		ArrayList<Facility> facilities = new ArrayList<Facility>();
		if (this.facilityIdFile == null) {
			Iterator<Facility> fac_it = controler.getFacilities().getFacilities("shop").values().iterator();
			for (Facility f:controler.getFacilities().getFacilities("shop").values()) {
			System.out.println("next = " + f);
			facilities.add(f);
			}
		}
		else {
			try {
				facilities =  new ArrayList<Facility>();
				FileReader fr = new FileReader(this.facilityIdFile);
				BufferedReader br = new BufferedReader(fr);
				// Skip header
				String curr_line = br.readLine();
				while ((curr_line = br.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);
					// header: f_id
					// index:     0
					Id fid = new IdImpl(entries[0]);
					facilities.add(controler.getFacilities().getFacility(fid));
				}
			} catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		this.retailers = this.createRetailers(facilities);
	}
	
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		Map<Id,Facility> movedFacilities = new TreeMap<Id,Facility>();
		for (Retailer r : retailers.getRetailers().values()) {
			// TODO balmermi: replace that with r.runStrategy();
			Map<Id,Facility> facs =  r.moveFacilitiesMaxLink(controler);
			movedFacilities.putAll(facs);
		}
		
		int iter = controler.getIteration();
		this.rs.write(this.retailers);
		
		for (Person p : controler.getPopulation().getPersons().values()) {
			pst.run(p,iter);
			for (Plan plan : p.getPlans()) {
				
				boolean routeIt = false;
				Iterator<?> actIter = plan.getIteratorAct();
				while (actIter.hasNext()) {
					Act act = (Act)actIter.next();
					if (movedFacilities.containsKey(act.getFacilityId())) {
						act.setLink(act.getFacility().getLink());
						routeIt = true;
					}
				}
				if (routeIt) {
					pcrl.run(plan);
				}
			}
		}
	}	

	private final ArrayList<Facility> getFraction(Map<Id,Facility> facs, double fraction) {
		ArrayList<Facility> fs = new ArrayList<Facility>();
		for (Facility f : facs.values()) {
			double rd = MatsimRandom.random.nextDouble();
			if (rd < fraction) { fs.add(f); }
		}
		return fs;
	}
	
	private final Retailers createRetailers(ArrayList<Facility> retailerFacilities) {
		Retailers retailers = new Retailers();
		for (Facility f : retailerFacilities) {
			Retailer r = new Retailer(f.getId());
			if (!r.addFacility(f)) { throw new RuntimeException("Could not add facility id="+f.getId()+" to retailer."); } 
			if (!retailers.addRetailer(r)) { throw new RuntimeException("Could not add retailer id="+r.getId()+" to retailers."); } 
		}
		return retailers;
	}
}
