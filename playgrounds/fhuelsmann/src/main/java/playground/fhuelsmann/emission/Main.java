package playground.fhuelsmann.emission;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;

import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
public class Main {

	public static void main (String[] args) throws Exception{

		// INPUT
		String runDirectory = "../../detailedEval/testRuns/output/1pct/v0-default/run6/";
		String eventsFile = runDirectory + "ITERS/it.10/10.events.txt.gz";
		String netfile = "../../detailedEval/Net/network-86-85-87-84.xml";
		//		String netfile = runDirectory + "output_network.xml.gz";
		
		//		String visumRoadHebefaRoadFile = "../../detailedEval/teststrecke/sim/inputEmissions/road_types.txt";
		String hbefaEmissionFactors = "../../detailedEval/teststrecke/sim/inputEmissions/hbefa_emission_factors_EU3_D.txt";
		//		String Hbefa_Cold_Traffic = "../../detailedEval/teststrecke/sim/inputEmissions/hbefa_coldstart_emission_factors.txt";


		//create an event object
		EventsManager events = new EventsManagerImpl();	

		// instancing the scenario
		ScenarioImpl scenario = new ScenarioImpl();
		Network network = scenario.getNetwork(); 
		new MatsimNetworkReader(scenario).readFile(netfile);

		// ?? was passiert hier ??
		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHabefaTable(hbefaEmissionFactors);

		//hbefaColdTable hbefaColdTable = new hbefaColdTable();
		//hbefaColdTable.makeHbefaColdTable(Hbefa_Cold_Traffic);
		//hbefaColdTable.printHbefaCold();

		//create the handler 
		TravelTimeEventHandler handler = new TravelTimeEventHandler(network,hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());

		LinkAndAgentAccountAnalyseModul linkAndAgentAccount = handler.getLinkAndAgentAccountAnalyseModul();
		//		LinkAndAgentAccountAnalyseModul linkAccount = new LinkAndAgentAccountAnalyseModul();

		//add the handler
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);
		matsimEventReader.readFile(eventsFile);

		Map<Id, double[]> linkId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerLink();
		Map<Id, double[]> personId2emissionsInGrammPerType = linkAndAgentAccount.getTotalEmissionsPerPerson();
		linkAndAgentAccount.printTotalEmissionTable(linkId2emissionsInGrammPerType, runDirectory + "emissionsPerLink.txt");
		linkAndAgentAccount.printTotalEmissionTable(personId2emissionsInGrammPerType, runDirectory + "emissionsPerPerson.txt");

		//for cold start emissions 
		//handler.printTable();
		//handler.printTable2();
		//	ColdEmissionFactor em = 
		//	new ColdEmissionFactor(handler.coldDistance,handler.parkingTime,hbefaColdTable.getHbefaColdTable());

		//em.MapForColdEmissionFactor();
		//em.printColdEmissionFactor();



		//		HbefaTable hbefaTable = new HbefaTable();
		//		hbefaTable.makeHabefaTable(Hbefa_Traffic);

		//		hbefaColdTable hbefaColdTable = new hbefaColdTable();
		//		hbefaColdTable.makeHbefaColdTable(Hbefa_Cold_Traffic);
		//		hbefaColdTable.printHbefaCold();
		/*		HbefaVisum hbefaVisum = new HbefaVisum(handler.getTravelTimes());
		hbefaVisum.createRoadTypes(visumRoadHebefaRoadFile);
		hbefaVisum.printHbefaVisum();
		hbefaVisum.createMapWithHbefaRoadTypeNumber();	

		EmissionFactor emissionFactor = new EmissionFactor(hbefaVisum.map,hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());
		emissionFactor.createEmissionTables();
		emissionFactor.createEmissionFile();
		emissionFactor.printEmissionTable();*/
	}
}	

