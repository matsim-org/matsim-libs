/* *********************************************************************** *
 * project: org.matsim.*
 * KtiPtTester.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.marcel.kti.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.api.experimental.Scenario;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

import playground.marcel.kti.router.PlansCalcRouteKti;
import playground.marcel.kti.router.SwissHaltestellen;

public class KtiPtTester {

	private Config config;
	private Scenario data;
	private Matrix ptTravelTimes = null;

	public KtiPtTester(final String[] args) {
		
		ScenarioLoader loader = new ScenarioLoader(args[0]);
		loader.loadNetwork();
		this.data = loader.getScenario();
		this.config = this.data.getConfig();
	}

	@SuppressWarnings("deprecation")
	public void readPtTimeMatrix(final String filename) {
		Matrices matrices = new Matrices();
		this.ptTravelTimes = matrices.createMatrix("pt_traveltime", ((ScenarioImpl)this.data).getWorld().getLayer("municipality"), null);
		System.out.println("  reading visum matrix file... ");
		VisumMatrixReader reader = new VisumMatrixReader(this.ptTravelTimes);
		reader.readFile(filename);
		System.out.println("  done.");
	}

	public void run() {
		Gbl.startMeasurement();
//		this.data.getWorld();
		Gbl.printRoundTime();
		readPtTimeMatrix("/Volumes/Data/ETH/cvs/ivt/studies/switzerland/externals/ptNationalModel/2005_OEV_Befoerderungszeit.mtx");
		Gbl.printRoundTime();
		PopulationImpl population = this.data.getPopulation();
		Gbl.printRoundTime();
		SwissHaltestellen haltestellen = new SwissHaltestellen((NetworkLayer) this.data.getNetwork());
		try {
			haltestellen.readFile("/Volumes/Data/ETH/cvs/ivt/studies/switzerland/externals/ptNationalModel/Haltestellen.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Gbl.printRoundTime();
		PreProcessLandmarks commonRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		commonRoutingData.run(this.data.getNetwork());
		FreespeedTravelTimeCost fttc = new FreespeedTravelTimeCost();
		Gbl.printRoundTime();
		Counter counter = new Counter("handle person #");
		PlansCalcRouteKti calcPtLeg = new PlansCalcRouteKti((NetworkLayer) this.data.getNetwork(), commonRoutingData, fttc, fttc, this.ptTravelTimes, haltestellen, ((ScenarioImpl)this.data).getWorld().getLayer("municipality"));
		for (PersonImpl person : population.getPersons().values()) {
			counter.incCounter();
			calcPtLeg.run(person.getSelectedPlan());
		}
		counter.printCounter();
		Gbl.printRoundTime();
		new PopulationWriter(population, this.config.plans().getOutputFile(), this.config.plans().getOutputVersion()).write();
		Gbl.printRoundTime();
	}

	public static void main(final String[] args) {
		new KtiPtTester(args).run();
	}

}
