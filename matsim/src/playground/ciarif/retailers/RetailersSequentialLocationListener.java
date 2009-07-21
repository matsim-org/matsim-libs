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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.data.coverage.grid.file.FSCatalogEntry;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class RetailersSequentialLocationListener implements StartupListener, IterationEndsListener {
	
	private final static Logger log = Logger.getLogger(RetailersSequentialLocationListener.class);
	
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable";
	public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	public final static String CONFIG_RETAILERS = "retailers";
	
	private Retailers retailers;
	private RetailersSummaryWriter rs = null;
	private PlansSummaryTable pst = null;
	private MakeATableFromXMLFacilities txf = null;
	private LinksRetailerReader lrr = null;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRoute pcrl = null;
	private String facilityIdFile = null;
	private ArrayList<LinkRetailersImpl> links = null;
	private RetailZones retailZones = new RetailZones();
	private ArrayList<ActivityFacility> shops = new ArrayList<ActivityFacility>();
	public RetailersSequentialLocationListener() {
	}

	public void notifyStartup(StartupEvent event) {

		Controler controler = event.getControler();
		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRoute(controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		String popOutFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_POP_SUM_TABLE);
		if (popOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_POP_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.pst = new PlansSummaryTable (popOutFile);
		
		this.txf = new MakeATableFromXMLFacilities("output/facilities_table2.txt");
		ActivityFacilitiesImpl facs = (ActivityFacilitiesImpl) controler.getFacilities();
		txf.write(facs);
		String retailersOutFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RET_SUM_TABLE);
		if (retailersOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RET_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.rs = new RetailersSummaryWriter (retailersOutFile);
		this.facilityIdFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		ArrayList<Id> retailersLinks = new ArrayList<Id>();
		if (this.facilityIdFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_GROUP+" not defined!");
		}
		
		else {
			try { //TODO Modify this!!!! In the sequential case other attributes for the retailer are needed, or in any case some of those which are now there are completely useless!!!!!
				this.retailers = new Retailers();
				FileReader fr = new FileReader(this.facilityIdFile);
				BufferedReader br = new BufferedReader(fr);
				// Skip header
				String curr_line = br.readLine();
				int notFoundFacilities = 0;
				while ((curr_line = br.readLine()) != null) {
					String[] entries = curr_line.split("\t", -1);
					// header: r_id  f_id  strategy linkId capacity
					// index:     0     1      2	   3	  4
					Id rId = new IdImpl(entries[0]);
					Id fId = new IdImpl (entries[1]);
					Map<Id,ActivityFacility> controlerFacilities = controler.getFacilities().getFacilities();
					
 					if (controlerFacilities.get(fId) != null) {
						if (this.retailers.getRetailers().containsKey(rId)) { // retailer exists already
							
							ActivityFacility f = controlerFacilities.get(fId);
							log.info("The added facility is = " + f.getId());
							this.retailers.getRetailers().get(rId).addFacility(f);
							log.info("The added facility is on the link number = " + f.getLink().getId());
							retailersLinks.add(f.getLink().getId());

						}	
						else { // retailer does not exists yet
							
							Retailer r = new Retailer(rId, null);
							log.info("The strategy " + entries[2] + " will be added to the retailer = " + rId);
							//r.addSequentialStrategy(controler, entries[2], this.links);
							ActivityFacility f = controlerFacilities.get(fId);
							r.addFacility(f);
							retailersLinks.add(f.getLink().getId());
							log.info("The added facility is on the link number = " + f.getLink().getId());
							this.retailers.addRetailer(r);
						}
					}
					else {
						notFoundFacilities = notFoundFacilities+1;
						log.warn("The facility " + fId + " has not been found" );
					}
				}
				log.warn(notFoundFacilities + " facilities have not been found");
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		this.lrr = new LinksRetailerReader (controler, retailersLinks);
		this.links = lrr.ReadLinks(); 
		Collection<PersonImpl> persons = controler.getPopulation().getPersons().values();
		int n =3; // TODO: get this from the config file  
		log.info("Number of retail zones = "+  n*n);
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		//ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (ActivityFacility f : controler.getFacilities().getFacilities().values()) {
			if (f.getActivityOptions().entrySet().toString().contains("shop")) {
				this.shops.add(f);
				log.info("The shop " + f.getId() + " has been added to the file 'shops'");
			}
			else {}//System.out.println ("Activity options are: " + f.getActivityOptions().values().toString());}
		}
		for (PersonImpl p : persons) {
			if (p.getSelectedPlan().getFirstActivity().getCoord().getX() < minx) { minx = p.getSelectedPlan().getFirstActivity().getCoord().getX(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getY() < miny) { miny = p.getSelectedPlan().getFirstActivity().getCoord().getY(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getX() > maxx) { maxx = p.getSelectedPlan().getFirstActivity().getCoord().getX(); }
			if (p.getSelectedPlan().getFirstActivity().getCoord().getY() > maxy) { maxy = p.getSelectedPlan().getFirstActivity().getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;

		double x_width = (maxx - minx)/n;
		double y_width = (maxy - miny)/n;
		int a = 0;
		int i = 0;
		while (i<n) {
			int j = 0;
			while (j<n) {
				Id id = new IdImpl (a);
				double x1= minx + i*x_width;
				double x2= x1 + x_width;
				double y1= miny + j*y_width;
				double y2= y1 + y_width;
				RetailZone rz = new RetailZone (id, x1, y1, x2, y2);
				rz.addPersons (persons); 
				rz.addFacilities (shops);
				this.retailZones.addRetailZone(rz);
				a=a+1;
				j=j+1;
			}
			i=i+1;
		}
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
		Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
		ArrayList<ActivityFacility> retailersFacilities = new ArrayList<ActivityFacility>();
		// works, but it is not nicely programmed. shouldn't be a global container, should be
		// controlled by the controler (or actually added to the population)
		log.info("There are (is) " + retailers.getRetailers().values().size() + " Retailer(s)");
		//Utils.setFacilityQuadTree(this.createFacilityQuadTree(controler));
		if (controler.getIteration()%2==0 && controler.getIteration()>0){
			for (Retailer r:retailers.getRetailers().values()) {
				retailersFacilities.addAll(r.getFacilities().values());
				log.info("The retailer " + r.getId().toString() + " owns " + r.getFacilities().values().size() + " facilities");
			}
			GravityModelRetailerStrategy gmrs = new GravityModelRetailerStrategy (controler, retailZones, shops, retailersFacilities, links); 
			gmrs.moveFacilities();
			gmrs.getMovedFacilities();
		
			for (PersonImpl p : controler.getPopulation().getPersons().values()) {
				
				PlanImpl plan = p.getSelectedPlan();
				// if I understand what's happening, at least potentially, much more persons than necessary are re-routed 
				boolean routeIt = false;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (movedFacilities.containsKey(act.getFacilityId())) { //TODO use here another movedFacilities object, this one very 
							// likely contains too much persons in it!!!!
							act.setLink(act.getFacility().getLink());
							routeIt = true;
						}
					}
				}
				if (routeIt) {
					pcrl.run(plan);
					log.info("The program is re-routing persons who were shopping in moved facilities");
				}
			}
		}	
	}
}

