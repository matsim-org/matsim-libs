/* *********************************************************************** *
 * project: org.matsim.*
 * BKickControler
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

package playground.fhuelsmann.emissions;
import org.junit.Test;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.testcases.MatsimTestCase;
public class MainTest extends MatsimTestCase{

	@Test
	public void test1 () throws Exception{

		String eventsFile = this.getInputDirectory() + "events_0807_it.0.txt";
		String visumRoadFile = this.getInputDirectory() + "visumnetzlink.txt";
		String visumRoadHebefaRoadFile = this.getInputDirectory() + "road_types.txt";
		String Hbefa_Traffic = this.getInputDirectory() + "hbefa_emission_factors.txt";

		//create an event object
		EventsManager events = new EventsManagerImpl();	

		//create the handler
		TravelTimeCalculation handler = new TravelTimeCalculation();

		//add the handler
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader matsimEventReader = new MatsimEventsReader(events);

		matsimEventReader.readFile(eventsFile);

		assertEquals( "----ohne Aktivität---,2.0,23953.0" , 
				handler.getTravelTimes().get(new IdImpl("52902456")).get("23933testVehicle").get(0).toString()  ) ;

		assertEquals( "----ohne Aktivität---,2.0,38598.0", 
				handler.getTravelTimes().get(new IdImpl("52902456")).get("38530testVehicle").get(0).toString() ) ;

		VelocityAverageCalculate vac = new VelocityAverageCalculate(handler.getTravelTimes(),visumRoadFile);
		vac.calculateAverageVelocity1();
		//	System.out.println(vac.getNewtravelTimesWithLengthAndAverageSpeed().get("52799758"));
		//System.out.println(vac.getOldtravelTimesWithLengthAndAverageSpeed());


		//vac.printVelocities();
		//	System.out.println(vac.getmapOfSingleEvent().get("592536888").get("10040_1").getFirst().getTravelTime());

		HbefaTable hbefaTable = new HbefaTable();
		hbefaTable.makeHabefaTable(Hbefa_Traffic);
		HbefaVisum hbefaVisum = new HbefaVisum(vac.getmapOfSingleEvent());
		hbefaVisum.creatRoadTypes(visumRoadHebefaRoadFile);
		hbefaVisum.createMapWithHbefaRoadTypeNumber();	

		EmissionFactor emissionFactor = new EmissionFactor(hbefaVisum.map,hbefaTable.getHbefaTableWithSpeedAndEmissionFactor());
		emissionFactor.createEmissionTables();
		String result = emissionFactor.printEmissionTable();
		//	System.out.println(emissionFactor.getmap().get("592536888").get("24072"));

		System.out.println( "result: " + result ) ;

		assertEquals(true, 
				result.contains("68012.0	7.0	51.42857142857143	592536888	68011testVehicle	0.1	8	55	0.0360758842952457	0.036944483925772216") );

	}
}	

