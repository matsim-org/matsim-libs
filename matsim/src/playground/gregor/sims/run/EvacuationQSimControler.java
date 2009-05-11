/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationQSimControler.java
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

package playground.gregor.sims.run;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkWriter;
import org.xml.sax.SAXException;

import playground.gregor.sims.evacbase.EvacuationAreaFileReader;
import playground.gregor.sims.evacbase.EvacuationAreaLink;
import playground.gregor.sims.evacbase.EvacuationPlansGeneratorAndNetworkTrimmer;

/**
 * @author glaemmel
 */
public class EvacuationQSimControler extends Controler {

	private final HashMap<Id, EvacuationAreaLink> evacuationAreaLinks = new HashMap<Id, EvacuationAreaLink>();
	final private static Logger log = Logger.getLogger(EvacuationQSimControler.class);

	public EvacuationQSimControler(final String[] args) {
		super(args);
	}

	@Override
	protected void setUp() {

		// first modify network and plans

		try {
			String evacuationAreaLinksFile = this.config.evacuation().getEvacuationAreaFile();
			new EvacuationAreaFileReader(this.evacuationAreaLinks).readFile(evacuationAreaLinksFile);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("generating initial evacuation plans... ");
		new EvacuationPlansGeneratorAndNetworkTrimmer().generatePlans(this.population, this.network, this.evacuationAreaLinks);
		log.info("done");

		log.info("writing network xml file... ");
		new NetworkWriter(this.network, getOutputFilename("evacuation_net.xml")).write();
		log.info("done");

		// then do the regular setup with the modified data

		super.setUp();
	}

	public static void main(final String[] args) {
		final Controler controler = new EvacuationQSimControler(args);
		controler.run();
		System.exit(0);
	}
}
