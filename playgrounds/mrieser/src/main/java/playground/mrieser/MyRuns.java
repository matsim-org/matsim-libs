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

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * @author mrieser
 */
public class MyRuns {

	private final static Logger log = Logger.getLogger(MyRuns.class);

	public static void main(final String[] args) throws IOException {

		log.info("start");


		String networkFile = "/Volumes/Data/coding/eclipse42git/matsimExamples/tutorial/lesson-3/network.xml";
//		String networkFile = "/Volumes/Data/vis/ch25pct_kti/network.c.xml.gz";
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

//		CoordinateTransformation t = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03);
//		
//		BufferedReader rdr = IOUtils.getBufferedReader("/Users/cello/Downloads/tracks.xy");
//		BufferedWriter wrtr = IOUtils.getBufferedWriter ("/Users/cello/Downloads/tracksCH1903.xy");
//		
//		String line = rdr.readLine();
//		wrtr.write(line.replace("lat", "x").replace("lon", "y"));
//		wrtr.write('\n');
//		
//		while ((line = rdr.readLine()) != null) {
//			String[] parts = StringUtils.explode(line, '\t');
//			Coord c = t.transform(new CoordImpl(Double.parseDouble(parts[1]), Double.parseDouble(parts[0])));
//			
//			wrtr.write(Double.toString(c.getX()));
//			wrtr.write('\t');
//			wrtr.write(Double.toString(c.getY()));
//			for (int i = 2, n = parts.length; i < n; i++) {
//				wrtr.write('\t');
//				wrtr.write(parts[i]);
//			}
//			wrtr.write('\n');
//		}
//		wrtr.close();
		
		
//		Coord c = t.transform(new CoordImpl(679976, 248958));
//		System.out.println(c.getX());
//		System.out.println(c.getY());



//		int size = 11;
//		int nOfParts = 3;
//		for (int i = 0; i < nOfParts; i++) {
//			System.out.println(size * (i+1) / nOfParts);
//		}


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
//			new MatsimNetworkReader(s).readFile(networkFile);
//			for (Link l : s.getNetwork().getLinks().values()) {
//				l.setCapacity(l.getCapacity() / 10.0);
//			}
//			((NetworkImpl) s.getNetwork()).setCapacityPeriod(3600);
//			new NetworkWriter(s.getNetwork()).write(networkFile);
//		}
//



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

//		System.out.println(Long.MAX_VALUE);
//		System.out.println(Long.MAX_VALUE / 8);
//		System.out.println(Long.MAX_VALUE / 8 / 1024);
//		System.out.println(Long.MAX_VALUE / 8 / 1024 / 1024);
//		System.out.println(Long.MAX_VALUE / 8 / 1024 / 1024 / 1024);
//		
//		System.out.println();
//		
//		System.out.println(Integer.MAX_VALUE);
//		System.out.println(Integer.MAX_VALUE / 8);
//		System.out.println(Integer.MAX_VALUE / 8 / 1024);
//		System.out.println(Integer.MAX_VALUE / 8 / 1024 / 1024);
//		System.out.println(Integer.MAX_VALUE / 8 / 1024 / 1024 / 1024);

		System.out.println(0x00000000000000FFL);
		System.out.println(0x000000000000FF00L >>> 8);
		System.out.println(0x0000000000FF0000L >>> 16);
		System.out.println(0x00000000FF000000L >>> 24);
		System.out.println(0x000000FF00000000L >>> 32);
		System.out.println(0x0000FF0000000000L >>> 40);
		System.out.println(0x00FF000000000000L >>> 48);
		System.out.println(0xFF00000000000000L >>> 56);

		System.out.println(Long.toBinaryString(1));
		System.out.println(Long.toBinaryString(82346120));
		
		for (int i = 0; i < 64; i++) {
			System.out.println(Long.toBinaryString(~(1L << i)));
		}
		
		System.out.println(Long.toBinaryString(-6884868992138582017L));
		System.out.println(Long.toBinaryString(-6884868992138582017L & 0x00000000000000FFL));
		System.out.println(Long.toBinaryString((-6884868992138582017L & 0x00000000000000FFL) >>> 0));

		long v = 0;
		v |= 1L << (255 - 192);
		System.out.println(Long.toBinaryString(v));
	}

}
