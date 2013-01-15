/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class Controler {

	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk", ""));
	public static ArrayList<String> frequency = new ArrayList<String>(Arrays.asList("VeryOften", "Often", "OnceAWhile", "Seldom", "Never", "NULL", ""));
	private TreeMap<Id, Person> population = new TreeMap<Id, Person>();
	
	private final static Logger log = Logger.getLogger(Controler.class);
	
	
	public static void main(String[] args) {
		Controler c = new Controler();
		String personFile = args[0];
		String personShopsFile = args[1];
		String addedShopsFile = args[2];
		c.run(personFile, personShopsFile, addedShopsFile);
	}
	
	public void run(String personFile, String personShopsFile, String addedShopsFile) {
		this.readDumpedPersons(personFile);
		log.info(this.population.size() + " persons created");
		this.readDumpedPersonShops(personShopsFile);		
		log.info("finished .......................................");
	}
	
	private void readUniversalCS(String file) {
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
// 0: username,		1: age,		2: sex,		3: income,	4: hhsize,	5: shoppingPerson,	6: purchasesPerMonth,	
// 7: H_Street,		8: H_nbr,	9: H_PLZ,	10: H_city, 11: H_Lat, 12: H_Lng,
// 13: fCar, 		14: fPt, 	15: fBike,	16: fWalk,	17: job, 
// 18: W_Street,	19: W_nbr,	20: W_PLZ,	21: W_city, 22: W_Lat, 23: W_Lng,			24: noAddressWork, 
// 25: mode,		26: fHome,	27: fWork,	28: fInter,	29: fOther

	private void readDumpedPersons(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				Id userId = new IdImpl(entrs[0].trim().substring(4, 8));
				
				Person person = new Person(userId);
				this.population.put(userId, person);
				
				person.setAge(Integer.parseInt(entrs[1].trim()));
				person.setSex(entrs[2].trim());
				// TODO: hh income -99 = AHV
				if (!entrs[3].trim().equals("")) {
					person.setHhIncome(Integer.parseInt(entrs[3].trim()));
				}
				else {
					person.setHhIncome(-1);
				}
				person.setHhSize(Integer.parseInt(entrs[4].trim()));
				person.setNbrPersonShoppingTripsMonth(Integer.parseInt(entrs[5].trim()));
				person.setNbrShoppingTripsMonth(entrs[6].trim());

				Location hlocation = new Location(new IdImpl(-1));
				person.setHomeLocation(hlocation);
				hlocation.setCity(entrs[10].trim());
				hlocation.setCoord(new CoordImpl(Double.parseDouble(entrs[11].trim()), Double.parseDouble(entrs[12].trim()))); // TODO: prüfen bei NULL
				
				person.setModesForShopping(Controler.modes.indexOf("car"), Controler.frequency.indexOf(entrs[13].trim().replaceAll("car", "")));
				person.setModesForShopping(Controler.modes.indexOf("pt"), Controler.frequency.indexOf(entrs[14].trim().replaceAll("pt", "")));
				person.setModesForShopping(Controler.modes.indexOf("bike"), Controler.frequency.indexOf(entrs[15].trim().substring(1)));
				person.setModesForShopping(Controler.modes.indexOf("walk"), Controler.frequency.indexOf(entrs[16].trim().substring(1)));
				
				if (entrs[17].trim().equals("yes")) {
					Location wlocation = new Location(new IdImpl(-2));
					person.setWorkLocation(wlocation);
					wlocation.setCity(entrs[21].trim());
					wlocation.setCoord(new CoordImpl(Double.parseDouble(entrs[22].trim()), Double.parseDouble(entrs[23].trim())));
				
					if (entrs[25].trim().contains("car")) person.setModeForWorking(Controler.modes.indexOf("car"), true);
					if (entrs[25].trim().contains("pt")) person.setModeForWorking(Controler.modes.indexOf("pt"), true);
					if (entrs[25].trim().contains("bike")) person.setModeForWorking(Controler.modes.indexOf("bike"), true);
					if (entrs[25].trim().contains("walk")) person.setModeForWorking(Controler.modes.indexOf("walk"), true);		
					
					person.setAreaToShop(0, Controler.frequency.indexOf(entrs[26].trim().replaceAll("home", "")));
					person.setAreaToShop(1, Controler.frequency.indexOf(entrs[27].trim().replaceAll("work", "")));
					person.setAreaToShop(2, Controler.frequency.indexOf(entrs[28].trim().replaceAll("inter", "")));
					person.setAreaToShop(3, Controler.frequency.indexOf(entrs[29].trim().replaceAll("other", "")));
				}				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}

// 0:	id, 			1: shop_id,		2: uname,		3: visitedByUser,	4: addedByUser,	5: awareness,	6: frequency,
// 7: iProducts,		8: iPtChange,	9: iDistance,	10: iPrize,			11: iParking,	12: iAtmo,		13: iOpentimes,	14: furtherReasons,
// 15: disadvantages,	16: disad_text,	
// 17: startLoc,	18: endLoc,			19: saved,		20: inCS	

	private void readDumpedPersonShops(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			Id prevId = new IdImpl(-99);
			int countAware = 0;
			int countVisited = 0;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				Id shopId = new IdImpl(entrs[1].trim());
				Id userId = new IdImpl(entrs[2].trim().substring(4, 8));
				
				Person person = this.population.get(userId);
				ShopLocation store = new ShopLocation(shopId);				
				int aware = 0;
				int visited = 0;
				
				if (entrs[5].trim().equals("yes")) {
					aware = 1;
					countAware++;
				}
				else if (entrs[5].trim().equals("no")) {
					aware = -1;
					store.setVisitFrequency("never");
				}
				if (entrs[3].trim().equals("yes")) {
					visited = 1;
					store.setVisitFrequency(entrs[6].trim());
					countVisited++;
				}
				else if (entrs[3].trim().equals("no")) {
					visited = -1;
					store.setVisitFrequency(entrs[6].trim());
				}
				if (person == null) {
					log.error("person null for user " + userId);
					System.exit(1);
				}
				else {
					person.addStore(store, aware, visited);	
				}
				if (prevId.compareTo(userId) != 0 && prevId.compareTo(new IdImpl(-99)) != 0) {
					log.info("parsed user: " + prevId + " " + countAware + " aware stores " + countVisited + " visited stores");
					countAware = 0;
					countVisited = 0;
				}
				prevId = new IdImpl(entrs[2].trim().substring(4, 8));
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
}
