/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutator.java
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

package org.matsim.locationchoice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.collections.QuadTree;


public abstract class LocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	protected NetworkLayer network = null;
	protected Controler controler = null;
	private final Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
	
	protected TreeMap<Id,Facility> chShopFacilitiesTreeMap = null;
	protected TreeMap<Id,Facility> chLeisureFacilitiesTreeMap = null;
	
	protected ArrayList<Facility> chShopFacilities = null;
	protected ArrayList<Facility> chLeisureFacilities = null;
	
	protected QuadTree<Facility> chShopFacQuadTree = null;
	protected QuadTree<Facility> chLeisFacQuadTree = null;
		
	protected ArrayList<Facility> zhShopFacilities = null;
	protected ArrayList<Facility> zhLeisureFacilities = null;
	
	protected QuadTree<Facility> zhShopFacQuadTree = null;
	protected QuadTree<Facility> zhLeisureFacQuadTree = null;
	
//	private static final Logger log = Logger.getLogger(LocationMutator.class);
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler) {
		this.initialize(network, controler);
	}
	
	public LocationMutator(final NetworkLayer network) {
		this.initialize(network, null);
	}

	private void initialize(final NetworkLayer network, Controler controler) {
	
		this.chShopFacilitiesTreeMap=new TreeMap<Id,Facility>();
		this.chLeisureFacilitiesTreeMap=new TreeMap<Id,Facility>();
		
		this.network = network;
		this.controler = controler;

		this.chShopFacilitiesTreeMap.putAll(this.facilities.getFacilities("shop_retail_gt2500sqm"));
		this.chShopFacilitiesTreeMap.putAll(this.facilities.getFacilities("shop_retail_get1000sqm"));
		this.chShopFacilitiesTreeMap.putAll(this.facilities.getFacilities("shop_retail_get400sqm"));
		this.chShopFacilitiesTreeMap.putAll(this.facilities.getFacilities("shop_retail_get100sqm"));
		this.chShopFacilitiesTreeMap.putAll(this.facilities.getFacilities("shop_retail_lt100sqm"));
		
		// do not use shop_other for the moment
		// this.shop_facilities.putAll(this.facilities.getFacilities("shop_other"));

		this.chLeisureFacilitiesTreeMap.putAll(this.facilities.getFacilities("leisure_gastro"));
		this.chLeisureFacilitiesTreeMap.putAll(this.facilities.getFacilities("leisure_culture"));
		this.chLeisureFacilitiesTreeMap.putAll(this.facilities.getFacilities("leisure_sports"));
		
		this.chShopFacQuadTree=this.builFacQuadTree(this.chShopFacilitiesTreeMap, this.chShopFacilitiesTreeMap);
		this.chLeisFacQuadTree=this.builFacQuadTree(this.chLeisureFacilitiesTreeMap, this.chLeisureFacilitiesTreeMap);
		
		double radius = 30000;
		this.zhShopFacilities = (ArrayList<Facility>)this.chShopFacQuadTree.get(683508.50, 246832.91, radius);
		this.zhLeisureFacilities = (ArrayList<Facility>)this.chLeisFacQuadTree.get(683508.50, 246832.91, radius);
		
		this.chShopFacilities = new ArrayList<Facility>(this.chShopFacilitiesTreeMap.values());
		this.chLeisureFacilities = new ArrayList<Facility>(this.chLeisureFacilitiesTreeMap.values());
		
		
		TreeMap<Id, Facility> treemapShop = new TreeMap<Id, Facility>();
		Iterator<Facility> sfac_it = this.zhShopFacilities.iterator();
		while (sfac_it.hasNext()) {
			Facility f = sfac_it.next();
			treemapShop.put(f.getId(), f);
		}
		
		TreeMap<Id, Facility> treemapLeisure = new TreeMap<Id, Facility>();
		Iterator<Facility> lfac_it = this.zhLeisureFacilities.iterator();
		while (lfac_it.hasNext()) {
			Facility f = lfac_it.next();
			treemapLeisure.put(f.getId(), f);
		}	
		this.zhShopFacQuadTree = this.builFacQuadTree(treemapShop, this.chShopFacilitiesTreeMap);
		this.zhLeisureFacQuadTree = this.builFacQuadTree(treemapLeisure, this.chLeisureFacilitiesTreeMap);
	}

	public void handlePlan(final Plan plan){
	}


	@Override
	public void run(final Person person) {
		final int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			final Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {	
		handlePlan(plan);
	}
	
	private QuadTree<Facility> builFacQuadTree(TreeMap<Id,Facility> facilities_of_type, 
			TreeMap<Id,Facility> swissFacilities) {
		Gbl.startMeasurement();
		System.out.println("      building facility quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		// the boundaries must be taken for the whole of Switzerland!
		for (final Facility f : swissFacilities.values()) {
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

	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
}
