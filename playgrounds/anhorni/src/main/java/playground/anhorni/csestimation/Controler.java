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

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class Controler {

	public static ArrayList<String> modes = new ArrayList<String>(Arrays.asList("car", "pt", "bike", "walk"));
	public static ArrayList<String> frequency = new ArrayList<String>(Arrays.asList("veryOften", "often", "onceAWhile", "seldom", "never"));
	private ArrayList<Person> population = new ArrayList<Person>();
	
	
	public static void main(String[] args) {
		Controler c = new Controler();
		String personFile = args[0];
		String personShopsFile = args[1];
		String addedShopsFile = args[2];
		c.run(personFile, personShopsFile, addedShopsFile);
	}
	
	public void run(String personFile, String personShopsFile, String addedShopsFile) {
		this.readDumpedPersons(personFile);
		this.readDumpedPersonShops(personShopsFile);
		this.readDumpedAddedShops(addedShopsFile);	
	}
	
	
	// fCar	fPt	fBike	fWalk	job
//	W_Street 	
//	W_nbr 	
//	W_PLZ 	
//	W_city 	
//	W_Lat 	
//	W_Lng 	
//	noAddressWork 	
//	mode 	
//	fHome 	
//	fWork 	
//	fInter 	
//	fOther
	
	
	private void readDumpedPersons(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split(";", -1);
				Id id = new IdImpl(entrs[0].trim());
				
				Person person = new Person(id);
				this.population.add(person);
				
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
				person.setNbrShoppingTripsMonth(Integer.parseInt(entrs[6].trim()));
				// 7: H_Street 8: H_nbr 9: H_PLZ
				Location hlocation = new Location();
				person.setHomeLocation(hlocation);
				hlocation.setCity(entrs[10].trim());
				hlocation.setCoord(new CoordImpl(Double.parseDouble(entrs[11].trim()), Double.parseDouble(entrs[12].trim())));
				
//				if (Boolean.parseBoolean(entrs[].trim())) {
//					Location wlocation = new Location();
//					person.setWorkLocation(wlocation);
//				}
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
	}
	
	private void readDumpedPersonShops(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split("\t", -1);

				Id id = new IdImpl(Integer.parseInt(entrs[0].trim()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
	
	private void readDumpedAddedShops(String file) {		
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String curr_line;
			while ((curr_line = br.readLine()) != null) {
				String[] entrs = curr_line.split("\t", -1);

				Id id = new IdImpl(Integer.parseInt(entrs[0].trim()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
}
