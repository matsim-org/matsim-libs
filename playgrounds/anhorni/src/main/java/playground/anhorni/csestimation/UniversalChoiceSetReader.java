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
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

public class UniversalChoiceSetReader {
	
	private WGS84toCH1903LV03 trafo = new WGS84toCH1903LV03();
	
	public TreeMap<Id<Location>, ShopLocation> readUniversalCS(String file) {
		TreeMap<Id<Location>, ShopLocation> shops = new TreeMap<Id<Location>, ShopLocation>();
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			br.readLine(); // skip header
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split("\t", -1);
				
				Id<Location> id = Id.create(Integer.parseInt(entrs[0].trim()), Location.class);
				ShopLocation shop = new ShopLocation(id);
				// lat -> 1 | lon -> 0
				Coord coord = new Coord(Double.parseDouble(entrs[5]), Double.parseDouble(entrs[4]));
				shop.setCoord(this.trafo.transform(coord));
				shops.put(id, shop);				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return shops;
	}
}
