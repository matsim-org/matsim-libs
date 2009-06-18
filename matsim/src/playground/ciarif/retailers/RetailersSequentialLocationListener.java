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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class RetailersSequentialLocationListener implements StartupListener, IterationEndsListener {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
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
	private Object[] links = null;
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
		this.lrr = new LinksRetailerReader (controler);
		this.links = lrr.ReadLinks(); 
		this.txf = new MakeATableFromXMLFacilities("output/facilities_table2.txt");
		ActivityFacilitiesImpl facs = (ActivityFacilitiesImpl) controler.getFacilities();
		txf.write(facs);
		String retailersOutFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RET_SUM_TABLE);
		if (retailersOutFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RET_SUM_TABLE+" in module = "+CONFIG_GROUP+" not defined!"); }
		this.rs = new RetailersSummaryWriter (retailersOutFile);
		this.facilityIdFile = controler.getConfig().findParam(CONFIG_GROUP,CONFIG_RETAILERS);
		if (this.facilityIdFile == null) { throw new RuntimeException("In config file, param = "+CONFIG_RETAILERS+" in module = "+CONFIG_GROUP+" not defined!");
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
					// header: r_id  f_id  strategy
					// index:     0     1      2
					Id rId = new IdImpl(entries[0]);
					if (this.retailers.getRetailers().containsKey(rId)) { // retailer exists already
						Id fId = new IdImpl (entries[1]);
						ActivityFacility f = controler.getFacilities().getFacilities().get(fId);
						this.retailers.getRetailers().get(rId).addFacility(f);
					}
					else { // retailer does not exists yet
						Retailer r = new Retailer(rId, null);
						System.out.println("The strategy " + entries[2] + " will be added to the retailer = " + rId);
						r.addStrategy(controler, entries[2], this.links);
						Id fId = new IdImpl (entries[1]);
						ActivityFacility f = controler.getFacilities().getFacilities().get(fId);
						r.addFacility(f);
						this.retailers.addRetailer(r);
					}
				}
			} 
			catch (IOException e) {
				Gbl.errorMsg(e);
			}
		}
		Collection<Person> persons = controler.getPopulation().getPersons().values();
		 // TODO check if it works at runtime, otherwise try to initialize the object differently
		int n =2; // TODO: get this from the config file  
		System.out.println("Number of retail zones = "+  n*n);
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		//ArrayList<ActivityOption> acts = new ArrayList<ActivityOption>();
		for (ActivityFacility f : controler.getFacilities().getFacilities().values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
			if (f.getActivityOptions().entrySet().toString().contains("shop")) {
				this.shops.add(f);
				System.out.println("The shop " + f.getId() + "has been added to the file 'shops'");
			}
			else {System.out.println ("Activity options are: " + f.getActivityOptions().values().toString());}
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
		// TODO Auto-generated method stub
		Controler controler = event.getControler();
		// TODO use a double ""for" cycle in order to avoid to use the getIteration method
		// the first is 0...n where n is the number of times the gravity model needs to 
		// be computed, the second is 0...k, where k is the number of iterations needed 
		// in order to obtain a relaxed state, or maybe use a while.
		//int iter = controler.getIteration();
		//if (controler.getIteration()>0 & controler.getIteration()%5==0){
		if (controler.getIteration()%5==0){
			log.info("matrix dimensions, columns = " + this.retailZones.getRetailZones().values().size());
			log.info("matrix dimensions, rows = " + shops.size());
			DenseDoubleMatrix2D am2d = new DenseDoubleMatrix2D (shops.size() , this.retailZones.getRetailZones().values().size());
			for (ActivityFacility f:shops) {//for (RetailZone rz : this.retailZones.getRetailZones().values()) 
				//rz.getPersonsQuadTree().
			}				
//			for (Person p : controler.getPopulation().getPersons().values()) {				
//				for (ActivityFacility f:controler.getFacilities().getFacilities().values()) {
//					for (PlanElement pe2 : p.getSelectedPlan().getPlanElements()) {
//						if (pe2 instanceof Activity) {
//							Activity act = (Activity) pe2;
//							if (act.getType().equals("shop") && act.getFacility().getId().equals(f.getId())) {
//								// TODO here characteristics of persons are checked (in which shop the shop activity happened, distance from home, 
//								//dimension, etc., the information is then saved in a special data structure having the facility ID as ID field 
//							}
//						}
//					}
//				}
//			}
		}
	}
	
	
}

