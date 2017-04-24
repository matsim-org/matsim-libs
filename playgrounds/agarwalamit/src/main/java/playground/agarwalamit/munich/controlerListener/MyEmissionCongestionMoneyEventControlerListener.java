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
package playground.agarwalamit.munich.controlerListener;

import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.congestion.ExperiencedDelayHandler;
import playground.agarwalamit.analysis.emission.caused.CausedEmissionCostHandler;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;
import playground.vsp.analysis.modules.monetaryTransferPayments.MoneyEventHandler;

/**
 * @author amit
 */

public class MyEmissionCongestionMoneyEventControlerListener implements StartupListener, IterationEndsListener{
	public static final Logger log =Logger.getLogger(MyEmissionCongestionMoneyEventControlerListener.class);

	private Map<Id<Person>, Double> pId2ColdEmissionsCosts = new HashMap<>();
	private Map<Id<Person>, Double> pId2WarmEmissionsCosts= new HashMap<>();
	private Map<Id<Person>, Double> pId2CongestionCosts= new HashMap<>();
	private Map<Id<Person>, Double> pId2Tolls= new HashMap<>();
	private MutableScenario scenario;

	@Inject private EmissionCostModule emissionCostModule;
	@Inject private EmissionModule emissionModule;

	private MoneyEventHandler moneyHandler;
	private ExperiencedDelayHandler congestionCostHandler;

	private CausedEmissionCostHandler emissCostHandler;
	private double vttsCar;
	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.scenario = (MutableScenario) event.getServices().getScenario();
		this.vttsCar = (this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		this.emissCostHandler = new CausedEmissionCostHandler(emissionCostModule);
		this.moneyHandler = new MoneyEventHandler();
		this.congestionCostHandler = new ExperiencedDelayHandler(scenario,1);

		event.getServices().getEvents().addHandler(congestionCostHandler);
		event.getServices().getEvents().addHandler(moneyHandler);

		emissionModule.getEmissionEventsManager().addHandler(emissCostHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("Per person delays costs, cold and warm emissions costs and toll will be written to a file for each iteration.");

		this.pId2Tolls = this.moneyHandler.getPersonId2amount();
		this.pId2CongestionCosts = this.congestionCostHandler.getDelayPerPersonAndTimeInterval().get(event.getServices().getConfig().qsim().getEndTime());
		this.pId2ColdEmissionsCosts = this.emissCostHandler.getPersonId2ColdEmissionCosts();
		this.pId2WarmEmissionsCosts = this.emissCostHandler.getPersonId2WarmEmissionCosts();

		String outputFile = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "person2VariousCosts.txt");
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);

		try {

			writer.write("personId \t delaysCosts \t coldEmissionsCosts \t"
					+ "warmEmissionsCosts \t totalEmissionsCosts \t toll \n");

			for(Id<Person> personId:this.scenario.getPopulation().getPersons().keySet()){
				double delaysCosts ;
				double coldEmissCosts;
				double warmEmissCosts;
				double toll;

				if(!this.pId2CongestionCosts.containsKey(personId)) delaysCosts =0;
				else delaysCosts = 	this.pId2CongestionCosts.get(personId) / 3600 * vttsCar;

				if(!this.pId2ColdEmissionsCosts.containsKey(personId)) coldEmissCosts=0;
				else coldEmissCosts = this.pId2ColdEmissionsCosts.get(personId); // value is positive. Amit Dec 16

				if(!this.pId2WarmEmissionsCosts.containsKey(personId)) warmEmissCosts =0;
				else warmEmissCosts  = this.pId2WarmEmissionsCosts.get(personId); // value is positive. Amit Dec 16

				if(!this.pId2Tolls.containsKey(personId)) toll =0;
				else toll = this.pId2Tolls.get(personId);

				double totalEmissCosts = coldEmissCosts+warmEmissCosts;

				writer.write(personId+"\t"+
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
