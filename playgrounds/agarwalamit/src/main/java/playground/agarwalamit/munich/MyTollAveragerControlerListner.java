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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

/**
 * @author amit
 */

public class MyTollAveragerControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener{

	public MyTollAveragerControlerListner (){

	}

	public static Logger log =Logger.getLogger(MyTollAveragerControlerListner.class);

	private Map<Id<Person>, Double> pId2Tolls= new HashMap<>();
	private Map<Id<Person>, Double> pId2NowTolls= new HashMap<>();
	private ScenarioImpl scenario;
	private BufferedWriter writer;
	private Controler controler;
	private int counter;
	private MoneyEventHandler moneyHandler;
	private int averagingStartIteration;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.controler = event.getControler();
		this.scenario = (ScenarioImpl) controler.getScenario();

		int firstIt = this.scenario.getConfig().controler().getFirstIteration();
		int lastIt = this.scenario.getConfig().controler().getLastIteration();
		double msaStarts = this.scenario.getConfig().vspExperimental().getFractionOfIterationsToStartScoreMSA();
		averagingStartIteration= firstIt + (int) (msaStarts * (lastIt-firstIt));

		for(Id personId:scenario.getPopulation().getPersons().keySet()){
			pId2Tolls.put(personId, 0.0);
		}

		String outputFile = controler.getControlerIO().getOutputPath()+"/analysis/simpleAverageToll.txt";
		this.writer =IOUtils.getBufferedWriter(outputFile);
		try {
			this.writer.write("personId \t  averageToll \n");
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		this.moneyHandler = new MoneyEventHandler();
		event.getControler().getEvents().addHandler(moneyHandler);

	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.pId2NowTolls = this.moneyHandler.getPersonId2amount();

		event.getControler().getEvents().removeHandler(moneyHandler);

		if(event.getIteration() >= averagingStartIteration){
			counter++;
			for(Id personId:pId2NowTolls.keySet()){
				double nowToll = pId2NowTolls.get(personId);
				double tollSoFar = pId2Tolls.get(personId);
				this.pId2Tolls.put(personId, nowToll+tollSoFar);
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			for(Id personId:pId2Tolls.keySet()){
				double toll = this.pId2Tolls.get(personId);
				this.writer.write(personId+"\t"+
						toll/counter+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
		SortedMap<UserGroup, Double> userGrpToToll = new TreeMap<UserGroup, Double>();
		PersonFilter pf = new PersonFilter();
		double totalToll =0;

		for(UserGroup ug : UserGroup.values()){
			userGrpToToll.put(ug, 0.);
		}

		for(UserGroup ug : UserGroup.values()){
			for(Id pId : pId2Tolls.keySet()){
				if(pf.isPersonIdFromUserGroup(pId, ug)){
					double tollSoFar = userGrpToToll.get(ug);
					userGrpToToll.put(ug, tollSoFar+pId2Tolls.get(pId));
					totalToll = totalToll+pId2Tolls.get(pId);
				}
			}
		}

		String outputFile = controler.getControlerIO().getOutputPath()+"/analysis/avgTollData.txt";
		writer = IOUtils.getBufferedWriter(outputFile);

		try {
			writer.write("UserGroup \t toll \n");

			for(UserGroup ug : userGrpToToll.keySet()){
				writer.write(ug+"\t"+userGrpToToll.get(ug)+"\n");
			}
			writer.write("total toll \t"+totalToll+"\n");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}

	}
}
