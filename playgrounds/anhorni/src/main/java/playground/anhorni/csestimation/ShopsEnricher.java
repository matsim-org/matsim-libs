/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;


public class ShopsEnricher {

	private TreeMap<Id, BZShop> bzShops;
	private TreeMap<Id, ShopLocation> shops = new TreeMap<Id, ShopLocation>();
	private final static Logger log = Logger.getLogger(ShopsEnricher.class);
	CH1903LV03toWGS84 trafo = new CH1903LV03toWGS84();
	
	public static void main(String[] args) {
		ShopsEnricher enricher = new ShopsEnricher();
		enricher.enrich(args[0], args[1], args[2]);
	}
	
	public void enrich(String ucsFile, String bzFile, String outShopsFile) {
		UniversalChoiceSetReader ucsReader = new UniversalChoiceSetReader();
		this.shops = ucsReader.readUniversalCS(ucsFile);	
		try {
			this.readBZ(bzFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.assignSize();
		this.assignPrice();
		Writer writer = new Writer();
		writer.writeShops(this.shops, outShopsFile);
		log.info("Enriching finished ------------------------------------------");
	}

	/*
	471901	Warenhäuser
	471101	Verbrauchermärkte (> 2500 m2)
	471102	Grosse Supermärkte (1000-2499 m2)
	471103	Kleine Supermärkte (400-999 m2)
	471104	Grosse Geschäfte (100-399 m2)
	471105	Kleine Geschäfte (< 100 m2)
	*/	
	private void readBZ(String bzFile) throws Exception {
		FileReader fr = new FileReader(bzFile);
		BufferedReader br = new BufferedReader(fr);
		String curr_line = br.readLine();  // Skip header
		
		this.bzShops = new TreeMap<Id, BZShop>();
		
		while ((curr_line = br.readLine()) != null) {
			int [] size = {0, 0, 0, 0, 0, 0};
			String[] entries = curr_line.split(";", -1);
			int x = Integer.parseInt(entries[1].trim());
			int y = Integer.parseInt(entries[2].trim());
			
			int sum = 0;
			for (int i = 3; i < 9; i++) {
				int v = Integer.parseInt(entries[i].trim());				
				size[i - 3] = v;
				sum += v;
			}
			if (sum > 0) {
				Id id = new IdImpl(Integer.parseInt(entries[0].trim()));
				BZShop bzShop = new BZShop(id);
				Coord coord = new CoordImpl(x, y);
				bzShop.setCoord(this.trafo.transform(coord)); // transform Swiss -> WGS84				
				bzShop.setSize(size);
				bzShops.put(id, bzShop);
			}
		}
		log.info("Added " + bzShops.size() + " BZ shops");
	}	
	
	private void assignSize() {
		QuadTree<Location> bzQuadTree = Utils.buildLocationQuadTree(this.bzShops); 
		for (ShopLocation shop:this.shops.values()) {			
			BZShop closestBZShop = (BZShop) bzQuadTree.get(shop.getCoord().getX(), shop.getCoord().getY());
			if (((CoordImpl)closestBZShop.getCoord()).calcDistance(shop.getCoord()) < 150) {
				if (closestBZShop.sizeMultiplyDefined()) {					
					if (shop.getId() == new IdImpl(40051)) {
						shop.setSize(4);
					}
					else if (shop.getId().compareTo(new IdImpl(40011)) == 0) {
						shop.setSize(1);
					}
					else if (shop.getId().compareTo(new IdImpl(40014)) == 0) {
						shop.setSize(3);
					}
					else if (shop.getId().compareTo(new IdImpl(100002)) == 0) {
						shop.setSize(0);
					}
					else if (shop.getId().compareTo(new IdImpl(10002)) == 0) {
						shop.setSize(4);
					}
					else if (shop.getId().compareTo(new IdImpl(100111)) == 0) {
						shop.setSize(4);
					}
					else if (shop.getId().compareTo(new IdImpl(100165)) == 0) {
						shop.setSize(0);
					}
					else {
						log.info("Store " + shop.getId() + " multiply defined " + closestBZShop.getSize()[0] + " " + closestBZShop.getSize()[1] + " "
								+ closestBZShop.getSize()[2] + " " + closestBZShop.getSize()[3] + " " + closestBZShop.getSize()[4] + " " + closestBZShop.getSize()[5]);
					}
				}
				for (int is = closestBZShop.getSize().length-1; is >= 0; is--) {
					if (closestBZShop.getSize()[is] == 1) {
						shop.setSize(is);
					}
				}				
			}
			else {
				log.info(shop.getId() + ": no store close by!");
			}
		}
	}
	
	private void assignPrice() {
		for (ShopLocation shop:this.shops.values()) {
			int lidl_aldi = 1;
			int denner = 2;
			int migros_coop = 3;
			int spar_other = 4;
			int marinello_globus = 5;			
			int idint = Integer.parseInt(shop.getId().toString());

			if (idint < 20000) {
				shop.setPrice(denner);
			}
			else if (idint > 20000 && idint < 30000) {
				shop.setPrice(spar_other);
			}
			else if ((idint > 30000 && idint < 50000)) {
				shop.setPrice(migros_coop);
			}
			else if ((idint > 60000 && idint < 70000) || (idint == 100004) || (idint == 100005) || (idint == 100006)) {
				shop.setPrice(marinello_globus);
			}
			else if ((idint > 70000 && idint < 80000) || (idint == 100074) || (idint == 100147) || (idint == 100148)) {
				shop.setPrice(lidl_aldi);
			}
			else {
				shop.setPrice(spar_other);
			}
		}
	}

	public TreeMap<Id, ShopLocation> getShops() {
		return this.shops;
	}
}
