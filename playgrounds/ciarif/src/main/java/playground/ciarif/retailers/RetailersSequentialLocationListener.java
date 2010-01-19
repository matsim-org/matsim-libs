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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;

import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;

public class RetailersSequentialLocationListener implements StartupListener, IterationEndsListener {
	
	private final static Logger log = Logger.getLogger(RetailersSequentialLocationListener.class);
	
	public final static String CONFIG_GROUP = "Retailers";
	public final static String CONFIG_POP_SUM_TABLE = "populationSummaryTable";
	public final static String CONFIG_RET_SUM_TABLE = "retailersSummaryTable";
	public final static String CONFIG_RETAILERS = "retailers";
	public final static String CONFIG_ZONES = "zones";
	public final static String CONFIG_PARTITION = "partition";
	public final static String CONFIG_SAMPLE_SHOPS = "samplingRateShops";
	public final static String CONFIG_SAMPLE_PERSONS = "samplingRatePersons";
	private Retailers retailers;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	private PlansCalcRoute pcrl = null;
	private String facilityIdFile = null;
	private final ArrayList<LinkRetailersImpl> retailersLinks = null;
	private final RetailZones retailZones = new RetailZones();
	private Map<Id,? extends ActivityFacility> controlerFacilities = null;
	ArrayList<ActivityFacility> sampledShops = new ArrayList<ActivityFacility>();

	private Controler controler;
	
	public RetailersSequentialLocationListener() {
	
	}

	public void notifyStartup(StartupEvent event) {

		this.controler = event.getControler();
		this.controlerFacilities = controler.getFacilities().getFacilities();
		preprocess.run(controler.getNetwork());
		pcrl = new PlansCalcRoute(controler.getNetwork(),timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		
		// Make a table with all shop activities listed  
		/*String popOutFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_POP_SUM_TABLE);
		if (popOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_POP_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		PlansSummaryTable pst = new PlansSummaryTable (popOutFile);*/
		
		// Make a table where all shops of the given scenario are listed with their attributes
		/*MakeATableFromXMLFacilities txf = new MakeATableFromXMLFacilities("../../output/facilities_table2.txt");
		txf.write(this.controlerFacilities);*/
		
		// Make a table reporting the movements of retailers' shops iteration after iteration
		/*String retailersOutFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RET_SUM_TABLE);
		if (retailersOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RET_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		RetailersSummaryWriter rs = new RetailersSummaryWriter (retailersOutFile);
		*/
		
		// Parameters which define the number of retail zones and the type of partition
		String type_of_partition = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_PARTITION);
		int number_of_zones =0;
		int n = (int)Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP,CONFIG_ZONES));
		if (type_of_partition.equals("symmetric")){
			number_of_zones = (int) Math.pow(n,2);}
		else {}//TODO Define the asymmetric version
		if (number_of_zones == 0) { throw new RuntimeException("In config file, param = "+CONFIG_ZONES+" in module = "+CONFIG_GROUP+" not defined!");}
		
		//Parameters which define the sampling rate for shops and persons. If no sampling rate is given the default is 1 for both of them
		double samplingRateShops = 1;
		if (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_SHOPS) != null) {
			samplingRateShops = Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_SHOPS));
				if (samplingRateShops>1 || samplingRateShops<0) { throw new RuntimeException("In config file, param = "+CONFIG_SAMPLE_SHOPS+" in module = "+CONFIG_GROUP+" must be set to a value between 0 and 1!!!");}
		}
		double samplingRatePersons = 1;
		if (controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_PERSONS) != null) {
			samplingRatePersons = Double.parseDouble(controler.getConfig().findParam(CONFIG_GROUP, CONFIG_SAMPLE_PERSONS));
				if (samplingRatePersons>1 || samplingRatePersons<0) { throw new RuntimeException("In config file, param = "+CONFIG_SAMPLE_PERSONS+" in module = "+CONFIG_GROUP+" must be set to a value between 0 and 1!!!");}
		}
		else {} //TODO put a warning here
		
		//The characteristics of retailers are read
		this.facilityIdFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) {throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_ZONES+" not defined!");}
		else { 
			this.retailers = new FileRetailerReader (this.controlerFacilities, this.facilityIdFile).readRetailers(this.controler);
		}
		
		//Links allowed for relocation are read or generated
		//this.retailersLinks = new LinksRetailerReader (controler, retailers).ReadLinks();
		
		
		//Retail zones are created
		Collection<? extends Person> persons = controler.getPopulation().getPersons().values();
		log.info("Number of retail zones = "+  n);//TODO solve better this problem of n and number of zones, names given here are confusing
		this.createZonesFromScenario(persons, this.findScenarioShops(), n, samplingRatePersons, samplingRateShops);
		
	}
	
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler controler = event.getControler();
		Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
		ArrayList<ActivityFacility> retailersFacilities = new ArrayList<ActivityFacility>();
		// works, but it is not nicely programmed. shouldn't be a global container, should be
		// controlled by the controler (or actually added to the population)
		log.info("There is (are) " + retailers.getRetailers().values().size() + " Retailer(s)");
		//Utils.setFacilityQuadTree(this.createFacilityQuadTree(controler));
		if (controler.getIteration()%5==0 && controler.getIteration()>0){
			// TODO could try to use a double ""for" cycle in order to avoid to use the getIteration method
			// the first is 0...n where n is the number of times the gravity model needs to 
			// be computed, the second is 0...k, where k is the number of iterations needed 
			// in order to obtain a relaxed state, or maybe use a while.
			//int iter = controler.getIteration();
			//if (controler.getIteration()>0 & controler.getIteration()%5==0){
			for (Retailer r:retailers.getRetailers().values()) {
				retailersFacilities.addAll(r.getFacilities().values());
				log.info("The retailer " + r.getId().toString() + " owns " + r.getFacilities().values().size() + " facilities");
			}
				log.info( this.sampledShops.size()+  " shops have been sampled");
				//GravityModelRetailerStrategy gmrs = new GravityModelRetailerStrategy (controler, this.retailZones, this.sampledShops, retailersFacilities, this.retailersLinks); 
				//gmrs.moveFacilities();
			//movedFacilities = gmrs.getMovedFacilities();
			int counter = 0;
			for (Person p : controler.getPopulation().getPersons().values()) {
				
				Plan plan = p.getSelectedPlan(); 
				boolean routeIt = false;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (movedFacilities.containsKey(act.getFacilityId())) { //TODO use here another movedFacilities object, this one very 
							// likely contains too much persons in it!!!!
							act.setLink(((ActivityFacilityImpl) this.controlerFacilities.get(act.getFacilityId())).getLink());
							routeIt = true;
						}
					}
				}
				
				if (routeIt) {
					pcrl.run(plan);
					counter = counter+1;
				}
			}
			log.info("The program re-routed " +  counter + " persons who were shopping in moved facilities");
		}	
	}
	
	private void createZonesFromScenario (Collection<? extends Person> persons, ArrayList<ActivityFacility> shops, int n, double samplingRatePersons, double samplingRateShops) {
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Person p : persons) {
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX() < minx) { minx = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY() < miny) { miny = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX() > maxx) { maxx = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getX(); }
			if (((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY() > maxy) { maxy = ((PlanImpl) p.getSelectedPlan()).getFirstActivity().getCoord().getY(); }
		}
		for (ActivityFacility shop : shops) {
			if (shop.getCoord().getX() < minx) { minx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() < miny) { miny = shop.getCoord().getY(); }
			if (shop.getCoord().getX() > maxx) { maxx = shop.getCoord().getX(); }
			if (shop.getCoord().getY() > maxy) { maxy = shop.getCoord().getY(); }
		}
		minx -= 1.0; miny -= 1.0; maxx += 1.0; maxy += 1.0;
		log.info("Min x = " + minx );
		log.info("Min y = " + miny );
		log.info("Max x = " + maxx );
		log.info("Max y = " + maxy );
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
				for (Person p : persons ) {
					Coord c = this.controlerFacilities.get(((PlanImpl) p.getSelectedPlan()).getFirstActivity().getFacilityId()).getCoord();
					if (c.getX()< x2 && c.getX()>=x1 && c.getY()<y2 && c.getY()>=y1) { 
						rz.addPersonToQuadTree(c,p);
					}		
				} 
				for (ActivityFacility af : shops) {
					Coord c = af.getCoord();
					if (c.getX()< x2 & c.getX()>=x1 & c.getY()<y2 & c.getY()>=y1) {
						rz.addShopToQuadTree(c,af);
					}
				}	
				this.retailZones.addRetailZone(rz);
				log.info("In the zone " + rz.getId() + ", " + rz.getShops().size() + " have been sampled");
				this.sampledShops.addAll(rz.getShops()); //TODO change this!!! The sampling should happen after the model has been estimated
				a=a+1;
				j=j+1;
			}
			i=i+1;
		}
	}
	
	private ArrayList<ActivityFacility> findScenarioShops() {
		ArrayList<ActivityFacility> shops = new ArrayList<ActivityFacility>();
		for (ActivityFacility f : this.controlerFacilities.values()) {
			if (f.getActivityOptions().entrySet().toString().contains("shop")) {
				shops.add(f);
				log.info("The shop " + f.getId() + " has been added to the file 'shops'");
			}
			else {}
		}
		return shops;
	}	
}

