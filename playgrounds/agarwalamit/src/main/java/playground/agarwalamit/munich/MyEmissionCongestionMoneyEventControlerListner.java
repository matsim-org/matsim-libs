/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.internalization.EmissionCostModule;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

/**
 * @author amit
 */

public class MyEmissionCongestionMoneyEventControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener{

	public MyEmissionCongestionMoneyEventControlerListner(ScenarioImpl sc, EmissionCostModule emissionCostModule) {
		this.scenario =sc;
		this.emissionCostModule = emissionCostModule;
	}

	public static Logger log =Logger.getLogger(MyEmissionCongestionMoneyEventControlerListner.class);

	private Map<Id<Person>, Double> pId2ColdEmissionsCosts = new HashMap<>();
	private Map<Id<Person>, Double> pId2WarmEmissionsCosts= new HashMap<>();
	private Map<Id<Person>, Double> pId2CongestionCosts= new HashMap<>();
	private Map<Id, Double> pId2Tolls= new HashMap<>();
	private ScenarioImpl scenario;
	private EmissionCostModule emissionCostModule;
	private BufferedWriter writer;
	private Controler controler;

	private MoneyEventHandler moneyHandler;
	private CongestionCostCollector congestionCostHandler;
	private EmissionCostsCollector emissCostHandler;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.controler = event.getControler();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		this.emissCostHandler = new EmissionCostsCollector(emissionCostModule);
		event.getControler().getEvents().addHandler(emissCostHandler);

		this.moneyHandler = new MoneyEventHandler();
		event.getControler().getEvents().addHandler(moneyHandler);

		this.congestionCostHandler = new CongestionCostCollector(scenario);
		event.getControler().getEvents().addHandler(congestionCostHandler);

		String outputFile = controler.getControlerIO().getIterationFilename(event.getIteration(), "person2VariousCosts.txt");
		this.writer =IOUtils.getBufferedWriter(outputFile);
		try {
			this.writer.write("personId \t delaysCosts \t coldEmissionsCosts \t"
					+ "warmEmissionsCosts \t totalEmissionsCosts \t toll \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.pId2Tolls = this.moneyHandler.getPersonId2amount();
		this.pId2CongestionCosts = this.congestionCostHandler.getCausingPerson2Cost();
		this.pId2ColdEmissionsCosts = this.emissCostHandler.getPersonId2ColdEmissCosts();
		this.pId2WarmEmissionsCosts = this.emissCostHandler.getPersonId2WarmEmissCosts();

		try {
			for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
				double delaysCosts = this.pId2CongestionCosts.get(personId);
				double coldEmissCosts = this.pId2ColdEmissionsCosts.get(personId);
				double warmEmissCosts = this.pId2WarmEmissionsCosts.get(personId);
				double totalEmissCosts = coldEmissCosts+warmEmissCosts;
				double toll = this.pId2Tolls.get(personId);
				this.writer.write(personId+"\t"+
						delaysCosts+"\t"+
						coldEmissCosts+"\t"+
						warmEmissCosts+"\t"+
						totalEmissCosts+"\t"+
						toll+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
