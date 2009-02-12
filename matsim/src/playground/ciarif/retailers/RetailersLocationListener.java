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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;

public class RetailersLocationListener implements IterationStartsListener, BeforeMobsimListener, AfterMobsimListener{

	private Retailers retailers;
	private final RetailersSummaryWriter rs;
	private final PlansSummaryTable pst;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRouteLandmarks pcrl = null;

	public RetailersLocationListener(final String retailerSummaryFileName) {
		rs = new RetailersSummaryWriter(retailerSummaryFileName, this.retailers);
		pst = new PlansSummaryTable ("output/triangle/output_Persons.txt");
	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
		Controler controler = event.getControler();

		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRouteLandmarks(controler.getNetwork(),preprocess,timeCostCalc, timeCostCalc);
		
		Map<Id,Facility> shopFac =  controler.getFacilities().getFacilities("shop");
		for (Facility f:shopFac.values()) {
			System.out.println("facility = " + f.getId());
			System.out.println("facility = " + f.getLink().getId());
		}
				
		ArrayList<Facility> retailerFacilities = this.getFraction(shopFac,0.4);
		this.retailers = this.createRetailers(retailerFacilities);
	}

	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		Map<Id,Facility> movedFacilities = new TreeMap<Id,Facility>();
		for (Retailer r : retailers.getRetailers().values()) {
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

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		Controler controler = event.getControler();
		
	}
	
}
