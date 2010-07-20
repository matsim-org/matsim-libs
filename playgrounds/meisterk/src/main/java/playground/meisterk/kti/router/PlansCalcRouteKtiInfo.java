/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.router;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.meisterk.kti.config.KtiConfigGroup;


public class PlansCalcRouteKtiInfo {
	private Matrix ptTravelTimes = null;
	private SwissHaltestellen haltestellen = null;
	private World localWorld=null;
	private final KtiConfigGroup ktiConfigGroup;

	private static final Logger log = Logger.getLogger(PlansCalcRouteKtiInfo.class);

	public PlansCalcRouteKtiInfo(final KtiConfigGroup ktiConfigGroup) {
		super();
		this.ktiConfigGroup = ktiConfigGroup;
	}

	public void prepare(final Network network) {

		if (!ktiConfigGroup.isUsePlansCalcRouteKti()) {
			log.error("The kti module is missing.");
		}

		// municipality layer from world file
		ScenarioImpl localScenario = new ScenarioImpl();
		this.localWorld = localScenario.getWorld();
		try {
			new MatsimWorldReader(localScenario).parse(ktiConfigGroup.getWorldInputFilename());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Reading traveltime matrix...");
		Matrices matrices = new Matrices();
		this.ptTravelTimes = matrices.createMatrix("pt_traveltime", null);
		VisumMatrixReader reader = new VisumMatrixReader(this.ptTravelTimes);
		reader.readFile(ktiConfigGroup.getPtTraveltimeMatrixFilename());
		log.info("Reading traveltime matrix...done.");

		log.info("Reading haltestellen...");
		this.haltestellen = new SwissHaltestellen(network);
		try {
			haltestellen.readFile(ktiConfigGroup.getPtHaltestellenFilename());
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Reading haltestellen...done.");

	}

	public Matrix getPtTravelTimes() {
		return ptTravelTimes;
	}

	public SwissHaltestellen getHaltestellen() {
		return haltestellen;
	}

	public World getLocalWorld() {
		return localWorld;
	}

	public KtiConfigGroup getKtiConfigGroup() {
		return ktiConfigGroup;
	}

}
