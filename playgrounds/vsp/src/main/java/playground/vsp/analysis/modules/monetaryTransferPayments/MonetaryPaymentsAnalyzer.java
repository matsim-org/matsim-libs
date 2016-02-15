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

package playground.vsp.analysis.modules.monetaryTransferPayments;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.analysis.modules.AbstractAnalysisModule;

/**
 * This module calculates the transfer payments for each agent and the sum of all transfer payments.
 * A transfer payment can be a toll, a fare or any other {@link PersonMoneyEvent}.
 * The sum of all transfer payments normally can be interpreted as the operator revenue.
 * 
 * @author ikaddoura
 *
 */
public class MonetaryPaymentsAnalyzer extends AbstractAnalysisModule{
	private final static Logger log = Logger.getLogger(MonetaryPaymentsAnalyzer.class);
	private MutableScenario scenario;

	private MoneyEventHandler moneyEventHandler;
	private Map<Id<Person>, Double> personId2amount;
	private double allUsersAmount;
	
	public MonetaryPaymentsAnalyzer() {
		super(MonetaryPaymentsAnalyzer.class.getSimpleName());
	}
	
	public void init(MutableScenario scenario) {
		this.scenario = scenario;
		
		this.moneyEventHandler = new MoneyEventHandler();
		this.personId2amount = new TreeMap<Id<Person>, Double>();
		this.allUsersAmount = 0.;
	}
	
	@Override
	public List<EventHandler> getEventHandler() {
		List<EventHandler> handler = new LinkedList<EventHandler>();
		handler.add(this.moneyEventHandler);		
		return handler;
	}

	@Override
	public void preProcessData() {
		// nothing to do
	}

	@Override
	public void postProcessData() {
		this.personId2amount = moneyEventHandler.getPersonId2amount();
		
		for (Double amount : personId2amount.values()){
			this.allUsersAmount += amount;
		}
	}

	@Override
	public void writeResults(String outputFolder) {
		String fileName = outputFolder + "transferPayments.txt";
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("Note: From users' perspective a positive amount means a gain, a negative amount a payment!");
			bw.newLine();
			bw.newLine();
			bw.write("total transfer payments (from users' perspective): " + this.allUsersAmount);
			bw.newLine();
			bw.newLine();
			bw.write("userID \t transfer payment from user's perspective");
			bw.newLine();
			
			for (Id id : this.personId2amount.keySet()){
				String row = id + "\t" + this.personId2amount.get(id);
				bw.write(row);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public Map<Id<Person>, Double> getPersonId2amount() {
		return personId2amount;
	}

	public double getAllUsersAmount() {
		return allUsersAmount;
	}
	
}
