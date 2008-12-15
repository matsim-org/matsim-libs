/* *********************************************************************** *
 * project: org.matsim.*
 * VolvoAnalysisMain.java
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

package playground.dgrether.analysis;

import java.io.IOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.xml.sax.SAXException;

/**
 * @author dgrether
 */
public class VolvoAnalysisMain {

	public static void main(String[] args) {
		volvoAnalysis(new String[] { "testData/volvoAnalysisConfig.xml" });
	}

	// FROM playground.mrieser.MyRuns
	public static void volvoAnalysis(String[] args) {

		System.out.println("RUN: volvoAnalaysis");
		Gbl.startMeasurement();
		Config config = Gbl.createConfig(args);
		System.out.println("  reading the network...");
		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		Gbl.getWorld().complete();
		System.out.println("  done.");
		System.out.println("  reading tolls...");
		RoadPricingScheme hundekopf;
		RoadPricingScheme gemarkung;
		String roadPricingHundekopf = config.getParam("roadpricing", "inputFileHundekopf");
		String roadPricingGemarkung = config.getParam("roadpricing", "inputFileGemarkung");

		System.out.println("  try...");
		try {
			RoadPricingReaderXMLv1 reader = new RoadPricingReaderXMLv1(network);
			reader.parse(roadPricingHundekopf);
			hundekopf = reader.getScheme();
			reader.parse(roadPricingGemarkung);
			gemarkung = reader.getScheme();
			// todo remove
			// hundekopf = new RoadPricingScheme(network);
			// gemarkung = new RoadPricingScheme(network);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (SAXException e) {
			e.printStackTrace();
			return;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("  done.");
		Events events = new Events();
		VolvoAnalysis analysis = new VolvoAnalysis(network, hundekopf, gemarkung);
		System.out.println("created VolvoAnalysis");

		events.addHandler(analysis);
		new MatsimEventsReader(events).readFile(config.events().getInputFile());
		events.printEventsCount();
		String outfilename = Gbl.getConfig().getParam("roadpricing", "outputFile");
		new VolvoAnalysisWriter(analysis, Locale.GERMAN).write(outfilename);
		Gbl.printElapsedTime();
		System.out.println("RUN: volvoAnalaysis finished.");
	}

}
