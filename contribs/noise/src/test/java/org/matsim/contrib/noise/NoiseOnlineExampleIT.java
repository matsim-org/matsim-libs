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

/**
 *
 */
package org.matsim.contrib.noise;

import com.google.inject.multibindings.Multibinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.analysis.XYTRecord;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ikaddoura
 *
 */

public class NoiseOnlineExampleIT {
	private static final Logger log = LogManager.getLogger( NoiseOnlineExampleIT.class ) ;

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void test0(){

		String configFile = testUtils.getPackageInputDirectory() + "config.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory( testUtils.getOutputDirectory() );

		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setWriteOutputIteration(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new NoiseModule());
		controler.run();

		String workingDirectory = controler.getConfig().controller().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controller().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controller().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.run();
	}

	@Test
	final void testOnTheFlyAggregationTerms() {
		String configFile = testUtils.getPackageInputDirectory() + "config.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory( testUtils.getOutputDirectory() );

		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setWriteOutputIteration(1);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		//Create NRPs in-memory
		NoiseReceiverPoint nrp1 = new NoiseReceiverPoint(Id.create("nrp1", ReceiverPoint.class), new Coord(0,0));
		NoiseReceiverPoint nrp2 = new NoiseReceiverPoint(Id.create("nrp2", ReceiverPoint.class), new Coord(1000,0));
		NoiseReceiverPoint nrp3 = new NoiseReceiverPoint(Id.create("nrp3", ReceiverPoint.class), new Coord(4000,0));
		NoiseReceiverPoint nrp4 = new NoiseReceiverPoint(Id.create("nrp4", ReceiverPoint.class), new Coord(5000,0));
		NoiseReceiverPoint nrp5 = new NoiseReceiverPoint(Id.create("nrp5", ReceiverPoint.class), new Coord(5000,-5000));
		NoiseReceiverPoint nrp6 = new NoiseReceiverPoint(Id.create("nrp6", ReceiverPoint.class), new Coord(0,-5000));

		NoiseReceiverPoint nrp7 = new NoiseReceiverPoint(Id.create("nrp7", ReceiverPoint.class), new Coord(1000,1000));
		NoiseReceiverPoint nrp8 = new NoiseReceiverPoint(Id.create("nrp8", ReceiverPoint.class), new Coord(2000,1));
		NoiseReceiverPoint nrp9 = new NoiseReceiverPoint(Id.create("nrp9", ReceiverPoint.class), new Coord(3000,1000));
		NoiseReceiverPoint nrp10 = new NoiseReceiverPoint(Id.create("nrp10", ReceiverPoint.class), new Coord(4000,1000));

		//create NRPs scenario element
		NoiseReceiverPoints nrps = new NoiseReceiverPoints();

		nrps.put(nrp1.getId(), nrp1);
		nrps.put(nrp2.getId(), nrp2);
		nrps.put(nrp3.getId(), nrp3);
		nrps.put(nrp4.getId(), nrp4);
		nrps.put(nrp5.getId(), nrp5);
		nrps.put(nrp6.getId(), nrp6);

		nrps.put(nrp7.getId(), nrp7);
		nrps.put(nrp8.getId(), nrp8);
		nrps.put(nrp9.getId(), nrp9);
		nrps.put(nrp10.getId(), nrp10);

		//add scenario element
		scenario.addScenarioElement(NoiseReceiverPoints.NOISE_RECEIVER_POINTS, nrps);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new NoiseModule());
		controler.run();

		//run file based noise processing as reference
		String workingDirectory = controler.getConfig().controller().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controller().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controller().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.run();

		String pathToImmissionsFile = controler.getConfig().controller().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controller().getLastIteration() + "/immissions/immission_processed.csv";

		//store file based noise aggregation terms by receiver
		Map<Id<ReceiverPoint>, Double> ldenByRp = new HashMap<>();
		Map<Id<ReceiverPoint>, Double> l69ByRp = new HashMap<>();
		Map<Id<ReceiverPoint>, Double> l1619ByRp = new HashMap<>();

		Map<String, Integer> idxFromKey = new ConcurrentHashMap<>();

		BufferedReader br = IOUtils.getBufferedReader(pathToImmissionsFile);

		try {

			String line = br.readLine();

			String[] keys = line.split(";");
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}

			int idxReceiverPointId = idxFromKey.get("Receiver Point Id");
			int idxLden = idxFromKey.get("Lden");
			int idxL69 = idxFromKey.get("L_6-9");
			int idxL1619 = idxFromKey.get("L_16-19");

			while((line = br.readLine()) != null){
				keys = line.split(";");
				ldenByRp.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), Double.parseDouble(keys[idxLden]));
				l69ByRp.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), Double.parseDouble(keys[idxL69]));
				l1619ByRp.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), Double.parseDouble(keys[idxL1619]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		//check whether on the fly caluclation equals file based aggregation terms
		for(Map.Entry<Id<ReceiverPoint>, Double> entry: ldenByRp.entrySet()) {
			Assertions.assertEquals(entry.getValue(), nrps.get(entry.getKey()).getLden(), MatsimTestUtils.EPSILON);
		}
		for(Map.Entry<Id<ReceiverPoint>, Double> entry: l69ByRp.entrySet()) {
			Assertions.assertEquals(entry.getValue(), nrps.get(entry.getKey()).getL69(), MatsimTestUtils.EPSILON);
		}
		for(Map.Entry<Id<ReceiverPoint>, Double> entry: l1619ByRp.entrySet()) {
			Assertions.assertEquals(entry.getValue(), nrps.get(entry.getKey()).getL1619(), MatsimTestUtils.EPSILON);
		}
	}

	@Test
	final void testNoiseListener(){

		Config config = ConfigUtils.loadConfig( testUtils.getPackageInputDirectory() + "config.xml", new NoiseConfigGroup() );
		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory( testUtils.getOutputDirectory() );

		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setWriteOutputIteration(1);
		// ---
		Scenario scenario = ScenarioUtils.loadScenario(config);
		// ---
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new NoiseModule());
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				Multibinder<NoiseModule.NoiseListener> binder = Multibinder.newSetBinder( this.binder(), NoiseModule.NoiseListener.class ) ;
				binder.addBinding().toInstance( new NoiseModule.NoiseListener(){
					@Override public void newRecord( XYTRecord record ){
						log.warn( record ) ;
					}
				} );
			}
		} ) ;

		controler.run();

		String workingDirectory = controler.getConfig().controller().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controller().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controller().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(workingDirectory, receiverPointsFile, noiseParameters.getReceiverPointGap());
		processNoiseImmissions.addListener( new NoiseModule.NoiseListener() {
			@Override public void newRecord( XYTRecord record ) { log.warn( record ) ; }
		}) ;
		processNoiseImmissions.run();
	}

}
