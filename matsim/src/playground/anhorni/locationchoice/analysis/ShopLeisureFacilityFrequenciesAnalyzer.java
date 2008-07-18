/* *********************************************************************** *
 * project: org.matsim.*
 * FacilityFrequenciesAnalyzer.java
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

package playground.anhorni.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Counter;


/**
 * Creates a table of the visitor frequencies for visitors of a certain shop or leisure facility.
 * @author anhorni
 */

public class ShopLeisureFacilityFrequenciesAnalyzer {

	private Plans plans=null;
	private NetworkLayer network=null;
	private Facilities  facilities =null;
	private TreeMap<Id,Facility> shop_facilities=new TreeMap<Id,Facility>();
	private TreeMap<Id,Facility> leisure_facilities=new TreeMap<Id,Facility>();
	private QuadTree<Facility> shopFacQuadTree = null;
	private QuadTree<Facility> leisFacQuadTree = null;

	private final static Logger log = Logger.getLogger(ShopLeisureFacilityFrequenciesAnalyzer.class);


	/**
	 * @param
	 *  - path of the plans file
	 */
	public static void main(String[] args) {

		if (args.length < 2 || args.length > 2 ) {
			System.out.println("Too few or too many arguments. Exit");
			System.exit(1);
		}
		String plansfilePath = args[0];
		String reducedArea = args[1];
		String type[] = {"s", "l"};
		String networkfilePath="./input/network.xml";
		String facilitiesfilePath="./input/facilities.xml.gz";

		log.info(plansfilePath);

		ShopLeisureFacilityFrequenciesAnalyzer analyzer = new ShopLeisureFacilityFrequenciesAnalyzer();
		analyzer.init(plansfilePath, networkfilePath, facilitiesfilePath);
		analyzer.checkShopANDLeisure();
		
		for (int i=0; i<2; i++) {		
			analyzer.collectAgents(type[i]);
			analyzer.writeFacilityFrequencies(type[i], reducedArea);
		}	
	}

	private void init(final String plansfilePath, final String networkfilePath,
			final String facilitiesfilePath) {


		this.network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(this.network).readFile(networkfilePath);
		log.info("network reading done");

		//this.facilities=new Facilities();
		this.facilities=(Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilities).readFile(facilitiesfilePath);
		log.info("facilities reading done");
		
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.shop_facilities.putAll(this.facilities.getFacilities("shop_retail_lt100sqm"));
		//this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_culture"));
		this.leisure_facilities.putAll(this.facilities.getFacilities("leisure_sports"));

		this.shopFacQuadTree=this.builFacQuadTree(this.shop_facilities);
		this.leisFacQuadTree=this.builFacQuadTree(this.leisure_facilities);
		
		log.info("Total number of ch shop facilities:" + this.shop_facilities.size());
		log.info("Total number of ch leisure facilities:" + this.leisure_facilities.size());

		this.plans=new Plans(false);
		final PlansReaderI plansReader = new MatsimPlansReader(this.plans);
		plansReader.readFile(plansfilePath);
		log.info("plans reading done");
	}

	
	
	
	private void collectAgents(String type) {
		Iterator<Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
			counter.incCounter();

			Plan selectedPlan = person.getSelectedPlan();

			final ArrayList<?> actslegs = selectedPlan.getActsLegs();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final Act act = (Act)actslegs.get(j);

				if (act.getType().startsWith(type)) {
					act.getFacility().addVisitorsPerDay(1);
				}
			}
		}
	}

	private void writeFacilityFrequencies(String type, String reducedArea) {

		log.info("writting " + type + " facilities");
		
		try {

			final String header="Facility_id\tx\ty\tNumberOfVisitors\tDailyCapacity\tAttrFactor";

			final BufferedWriter out = IOUtils.getBufferedWriter("./output/facFrequencies_"+type+".txt");
			out.write(header);
			out.newLine();

			Iterator<? extends Facility> iter = null;
			if (reducedArea.equals("0")) {
				log.info("complete area");
				iter = this.facilities.iterator();
			}
			else {
				if (type.equals("s")) {
					log.info("reduced area: shop facilities");
					//iter = this.shopFacQuadTree.get(683508.50, 246832.91, 30000).iterator();
					iter = this.shopFacQuadTree.get(683508.50, 246832.91, 60000).iterator();
				}
				else {
					log.info("reduced area: leisure facilities");
					//iter = this.leisFacQuadTree.get(683508.50, 246832.91, 30000).iterator();
					iter = this.leisFacQuadTree.get(683508.50, 246832.91, 60000).iterator();
				}
			}
			while (iter.hasNext()){
				Facility facility = iter.next();
				facility.finish();

				//if (facility.getNumberOfVisitorsPerDay() > 0) {
					
				boolean type_ok = false;
				Iterator<Activity> act_it=facility.getActivities().values().iterator();
				while (act_it.hasNext()){
					Activity activity = act_it.next();
					if (activity.getType().startsWith(type)) {
						type_ok = true;
					}
				}
				if (type_ok) {
					out.write(facility.getId().toString()+"\t"+
						String.valueOf(facility.getCenter().getX())+"\t"+
						String.valueOf(facility.getCenter().getY())+"\t"+
						String.valueOf(facility.getNumberOfVisitorsPerDay())+"\t"+
						String.valueOf(facility.getDailyCapacity())+"\t"+
						String.valueOf(facility.getAttrFactor()));
					out.newLine();
				}
				//}
				out.flush();
			}
			out.close();
		}
		catch (final IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void checkShopANDLeisure() {
		
		log.info("checking if a facility has shop AND leisure activities");
				
		Iterator<? extends Facility> iter = this.facilities.iterator();
		while (iter.hasNext()){
			Facility facility = iter.next();
			
			boolean shop = false;
			boolean leisure = false;
			
			Iterator<Activity> act_it=facility.getActivities().values().iterator();
			while (act_it.hasNext()){
				Activity activity = act_it.next();
				if (activity.getType().startsWith("s")) {
					shop = true;
				}
			}
			act_it=facility.getActivities().values().iterator();
			while (act_it.hasNext()){
				Activity activity = act_it.next();
				if (activity.getType().startsWith("l")) {
					leisure = true;
				}
			}
			if (shop && leisure) {
				log.info(facility.getId() + " has shop AND leisure activities");
			}
		}
	}

	private QuadTree<Facility> builFacQuadTree(TreeMap<Id,Facility> facilities_of_type) {
		Gbl.startMeasurement();
		System.out.println("      building facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (final Facility f : facilities_of_type.values()) {
			if (f.getCenter().getX() < minx) { minx = f.getCenter().getX(); }
			if (f.getCenter().getY() < miny) { miny = f.getCenter().getY(); }
			if (f.getCenter().getX() > maxx) { maxx = f.getCenter().getX(); }
			if (f.getCenter().getY() > maxy) { maxy = f.getCenter().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Facility> quadtree = new QuadTree<Facility>(minx, miny, maxx, maxy);
		for (final Facility f : facilities_of_type.values()) {
			quadtree.put(f.getCenter().getX(),f.getCenter().getY(),f);
		}
		System.out.println("      done.");
		Gbl.printRoundTime();
		return quadtree;
	}
}

