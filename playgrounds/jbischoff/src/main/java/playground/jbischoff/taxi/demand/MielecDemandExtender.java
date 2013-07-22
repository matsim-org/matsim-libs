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

package playground.jbischoff.taxi.demand;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.demand.ODDemandGenerator;

/**
 *@author jbischoff
 *
 */

public class MielecDemandExtender {

	private String inputPlansFile = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\20.plans.xml.gz";
	private String inputNetFile = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\network.xml";

	private String inputTaxiDemand = "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\taxiCustomers_05_pc.txt";
	private String outputTaxiDemandDir =  "C:\\local_jb\\Dropbox\\MasterOfDesaster\\jbischoff\\jbmielec\\taxidemand\\";
	
	private TreeSet<Id> customerIds = new TreeSet<Id>();
	private HashSet<Id> agentIds;
	private int MAXIMUMDEMANDINPERCENT = 99;
	/**
	 * @param args
	 */
	
	public static void main(String[] args) {
		
		MielecDemandExtender mde = new MielecDemandExtender();
		mde.readPlans();
		mde.readCustomers();
		mde.fillCustomers();
	}
	
	
	private void readCustomers() {

		  List<String> taxiCustomerIds;
	        try {
	            taxiCustomerIds = ODDemandGenerator.readTaxiCustomerIds(inputTaxiDemand);
	        }
	        catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	        for (String s : taxiCustomerIds){
	        	customerIds.add(new IdImpl(s));
	        }
	}
	
	private void fillCustomers(){
		double d = (double)customerIds.size()/(double)agentIds.size();
		long currentPercentage = Math.round(d*100);
		Random r = new Random();
		currentPercentage++;
		for (;currentPercentage<=MAXIMUMDEMANDINPERCENT;currentPercentage++){
			long amountOfPlans = Math.round((((double)currentPercentage/100.)*agentIds.size()));
			System.out.println(amountOfPlans);
			for (long i = customerIds.size(); i<=amountOfPlans;i++){
				addCustomer(r);
			}
			exportCustomers(currentPercentage);
		}
	}
	
	private void exportCustomers (long percentage){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputTaxiDemandDir+"taxiCustomers_"+percentage+"_.txt")));
			
			
			TreeSet<Integer> ints = new TreeSet<Integer>();
			
			
			for (Id cid : customerIds){
				ints.add(Integer.parseInt(cid.toString()));
			}
			
			for (Integer i : ints){
				bw.append(i.toString());
				bw.newLine();
			}
			System.out.println("Wrote "+percentage+" with "+customerIds.size()+" customers");
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	private void addCustomer(Random r){
		
		do {
		Id cid = new IdImpl(r.nextInt(agentIds.size()));
		if (!customerIds.contains(cid)){
			if(agentIds.contains(cid)){
			customerIds.add(cid);
			break;
		}}
		
		} while (true);
	}
	

	private void readPlans() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc).readFile(inputNetFile);
		new MatsimPopulationReader(sc).readFile(inputPlansFile);
		agentIds = new HashSet<Id>(sc.getPopulation().getPersons().keySet());
		
	}

	

}
