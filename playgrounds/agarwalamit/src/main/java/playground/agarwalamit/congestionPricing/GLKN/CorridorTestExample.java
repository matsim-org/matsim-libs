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

package playground.agarwalamit.congestionPricing.GLKN;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.congestionPricing.testExamples.handlers.CorridorNetworkAndPlans;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;

/**
 * @author amit
 */

class CorridorTestExample {

	public static void main(String[] args) {
		new CorridorTestExample().run();
	}
	
	private BufferedWriter writer;
	
	private void run(){
		int noOfPersonsInPlan = 10;
		writer = IOUtils.getBufferedWriter("./output/delay_analysis.txt");
		writeString("TotalDelay \t totalInternalizedDelay \n");
		CorridorNetworkAndPlans corridor = new CorridorNetworkAndPlans();
		corridor.createNetwork();
		corridor.createPopulation(noOfPersonsInPlan);
		Scenario sc = corridor.getDesiredScenario();
		getCongestionEvents(sc);
		
		try {
			writer.close();	
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}
	
	private void getCongestionEvents (Scenario sc) {
		
		sc.getConfig().qsim().setStuckTime(3600);

		EventsManager events = EventsUtils.createEventsManager();

		CongestionHandlerImplV3 v3 = new CongestionHandlerImplV3(events, (MutableScenario)sc);
		CongestionHandlerImplV4 v4 = new CongestionHandlerImplV4(events, sc);
//		CongestionHandlerImplV6 v6 = new CongestionHandlerImplV6(events, sc);
//		CongestionHandlerImplV7 v7_gl = new CongestionHandlerImplV7(events, sc, CongestionImpls.GL);
//		CongestionHandlerImplV7 v7_kn = new CongestionHandlerImplV7(events, sc, CongestionImpls.KN);
		
		events.addHandler(v3);
		events.addHandler(v4);
//		events.addHandler(v6);
//		events.addHandler(v7_gl);
//		events.addHandler(v7_kn);

		QSim sim = QSimUtils.createDefaultQSim(sc, events);
		sim.run();

		writeString("v3 \t"+v3.getTotalDelay()+"\t"+v3.getTotalInternalizedDelay()+"\n");
		writeString("v4 \t"+v4.getTotalDelay()+"\t"+v4.getTotalInternalizedDelay()+"\n");
//		writeString("v6 \t"+v6.getTotalDelay()+"\t"+v6.getTotalDelay()+"\n");
//		writeString("v7_gl \t"+v7_gl.getTotalDelay()+"\t"+v7_gl.getTotalInternalizedDelay()+"\n");
//		writeString("v7_kn \t"+v7_kn.getTotalDelay()+"\t"+v7_kn.getTotalInternalizedDelay()+"\n");
	}
	
	private void writeString(String str){
		try {
			writer.write(str);
		} catch (IOException e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}
}


	