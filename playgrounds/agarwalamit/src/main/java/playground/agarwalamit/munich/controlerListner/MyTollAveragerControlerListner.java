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
package playground.agarwalamit.munich.controlerListner;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

/**
 * @author amit
 */

public class MyTollAveragerControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener{

	public MyTollAveragerControlerListner (){

	}

	public static Logger log =Logger.getLogger(MyTollAveragerControlerListner.class);

	private final Map<Id<Person>, Double> pId2Tolls= new HashMap<>();
	private Map<Id<Person>, Double> pId2NowTolls= new HashMap<>();
	private MutableScenario scenario;
	private BufferedWriter writer;
	private MatsimServices controler;
	private int counter;
	private MoneyEventHandler moneyHandler;
	private int averagingStartIteration;

	@Override
	public void notifyStartup(StartupEvent event) {
		this.controler = event.getServices();
		this.scenario = (MutableScenario) controler.getScenario();

		int firstIt = this.scenario.getConfig().controler().getFirstIteration();
		int lastIt = this.scenario.getConfig().controler().getLastIteration();
		double msaStarts = this.scenario.getConfig().planCalcScore().getFractionOfIterationsToStartScoreMSA();
		averagingStartIteration= firstIt + (int) (msaStarts * (lastIt-firstIt));

		for(Id<Person> personId:scenario.getPopulation().getPersons().keySet()){
			pId2Tolls.put(personId, 0.0);
		}

		String outputDir = controler.getControlerIO().getOutputPath()+"/analysis/";
		if ( ! new File(outputDir).exists() ) new File(outputDir).mkdirs();
		
		String outputFile = outputDir+"/simpleAverageToll.txt";
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

		if(event.getIteration() >= averagingStartIteration){
			this.moneyHandler = new MoneyEventHandler();
			event.getServices().getEvents().addHandler(moneyHandler);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		if(event.getIteration() >= averagingStartIteration){
			this.pId2NowTolls = this.moneyHandler.getPersonId2amount();
			counter++;
			for(Id<Person> personId:pId2NowTolls.keySet()){
				double nowToll = pId2NowTolls.get(personId);
				double tollSoFar = pId2Tolls.get(personId);
				this.pId2Tolls.put(personId, nowToll+tollSoFar);
			}
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			for(Id<Person> personId:pId2Tolls.keySet()){
				double toll = this.pId2Tolls.get(personId);
				this.writer.write(personId+"\t"+
						toll/counter+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: "
					+ e);
		}
	}
}
