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
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.AStarLandmarksFactory;
import org.matsim.router.util.PreProcessLandmarks;


public class RetailersLocationListener implements StartupListener, BeforeMobsimListener {
	
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable";
	public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	public final static String CONFIG_RETAILERS = "retailers";
//	private Retailers retailers = new Retailers();
	private Retailers retailers;
	private RetailersSummaryWriter rs = null;
	private PlansSummaryTable pst = null;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRoute pcrl = null;
	private String facilityIdFile = null;
	//private final String locationStrategy=null;
	private int alternatives;
	
	public RetailersLocationListener() {
	}

	public void notifyStartup(StartupEvent event) {

		Controler controler = event.getControler();
		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRoute(controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		String popOutFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_POP_SUM_TABLE);
		if (popOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_POP_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.pst = new PlansSummaryTable (popOutFile);
		String retailersOutFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_RET_SUM_TABLE);
		if (retailersOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RET_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.rs = new RetailersSummaryWriter (retailersOutFile);
		this.facilityIdFile = Gbl.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) { //Francesco: TODO decide if throw an exception or permit a way to create retailers without an input file
		}
		else {
			try {
				this.retailers = new Retailers();
				FileReader fr = new FileReader(this.facilityIdFile);
				BufferedReader br = new BufferedReader(fr);
				// Skip header
				String curr_line = br.readLine();
				while ((curr_line = br.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);
					// header: f_id
					// index:     0
					Id rId = new IdImpl(entries[0]);
					if (this.retailers.getRetailers().containsKey(rId)) {
						Id fId = new IdImpl (entries[1]);
						Facility f = controler.getFacilities().getFacility(fId);
						this.retailers.getRetailers().get(rId).addFacility(f);
					}
					else {
						Retailer r = new Retailer(rId, null);
						r.addStrategy(controler, entries[2]);
						Id fId = new IdImpl (entries[1]);
						Facility f = controler.getFacilities().getFacility(fId);
						r.addFacility(f);
						this.retailers.addRetailer(r);
						
					}
				}
			} catch (IOException e) {
				Gbl.errorMsg(e);
			}
		} 
	}
	
	public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
		Controler controler = event.getControler();
		Map<Id,Facility> movedFacilities = new TreeMap<Id,Facility>();
		controler.getLinkStats().addData(controler.getVolumes(), controler.getTravelTimeCalculator());
		
		for (Retailer r : this.retailers.getRetailers().values()) {
			Map<Id,Facility> facs =  r.runStrategy();
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
}
