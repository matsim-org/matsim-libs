/* *********************************************************************** *
 * project: org.matsim.*
 * MyRuns.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mrieser;

import org.apache.log4j.Logger;

/**
 * @author mrieser
 */
public class MyRuns {

	private final static Logger log = Logger.getLogger(MyRuns.class);

	public static void main(final String[] args) {

		log.info("start");


		String networkFile = "/Volumes/Data/vis/ch25pct_kti/network.c.xml.gz";
//		String networkFile = "/Volumes/Data/talks/20120322_UsrMtg_Via/data_basic/network.xml.gz";
		String inPlansFile = "/Volumes/Data/talks/20120322_UsrMtg_Via/data_basic/0.plans.xml.gz";
		String outPlansFile = "/Volumes/Data/talks/20120322_UsrMtg_Via/data_basic/0.plans.selected.xml.gz";

//		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
//
//		log.info("Reading network from " + networkFile);
//		new MatsimNetworkReader(sc).readFile(networkFile);
//
//		System.gc();
//		System.gc();
//		System.gc();
//		System.gc();
//		System.gc();
//
//		while (true) {
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}


		int size = 11;
		int nOfParts = 3;
		for (int i = 0; i < nOfParts; i++) {
			System.out.println(size * (i+1) / nOfParts);
		}


//
//		final PopulationImpl plans = (PopulationImpl) sc.getPopulation();
//		plans.setIsStreaming(true);
//		plans.addAlgorithm(new PersonFilterSelectedPlan());
//
//		final PopulationWriter plansWriter = new PopulationWriter(plans, sc.getNetwork());
//		plansWriter.startStreaming(outPlansFile);
//		plans.addAlgorithm(plansWriter);
//		PopulationReader plansReader = new MatsimPopulationReader(sc);
//
//		log.info("Reading plans file from " + inPlansFile);
//		plansReader.readFile(inPlansFile);
//		plans.printPlansCount();
//		plansWriter.closeStreaming();



//		{
//			Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//			new MatsimNetworkReader(s).readFile("/Volumes/Data/vis/ch25pct_kti/network.c.xml.gz");
//			FreespeedTravelTimeAndDisutility ttc = new FreespeedTravelTimeAndDisutility(s.getConfig().planCalcScore());
//			PreProcessLandmarks preProcessData = new PreProcessLandmarks(ttc);
//			log.info("start preprocess");
//			preProcessData.run(s.getNetwork());
//			log.info("stop preprocess");
//		}




//		AStarLandmarks router = new AStarLandmarks(s.getNetwork(), preProcessData, ttc, ttc);

//		EventsManager em = EventsUtils.createEventsManager();
//		EventWriterXML writer = new EventWriterXML("/Volumes/Data/projects/zhaw/wu/events.run374.it240.xml.gz");
//		em.addHandler(writer);
//		new MatsimEventsReader(em).readFile("/Volumes/Data/projects/zhaw/wu/events.run374.it240.txt.gz");
//		writer.closeFile();

//		new MatsimNetworkReader(s).readFile("/Users/cello/sweden2.xml.gz");
//		List<Node> nodes = new ArrayList<Node>(s.getNetwork().getNodes().values());
//		for (Node node : nodes) {
//			if (node.getCoord().getY() > 6460000 || node.getCoord().getY() < 6340000 || node.getCoord().getX() > 400000) {
//				s.getNetwork().removeNode(node.getId());
//			}
//		}
//		new NetworkCleaner().run(s.getNetwork());
//		new NetworkWriter(s.getNetwork()).write("/Users/cello/goteborg.xml.gz");

//		log.info("done.");

	}

}
