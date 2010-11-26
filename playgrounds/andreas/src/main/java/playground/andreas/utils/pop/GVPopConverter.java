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

package playground.andreas.utils.pop;

import java.io.File;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.andreas.bln.pop.SharedNetScenario;

/**
 * Read a given population, expand it, rename the person ids and finally shuffle the coords of every person, except the original one.
 * It is assumed that the original persons has a digit only Id.
 *
 * @author aneumann
 *
 */
public class GVPopConverter {

	public static void main(final String[] args) {
		Gbl.startMeasurement();

		ScenarioImpl sc = new ScenarioImpl();

		String networkFile = "e:/_shared-svn/berlin_prognose_2025/counts_network_merged.xml_cl.xml";
		String inPlansFile = "e:/_shared-svn/berlin_prognose_2025/bb_gv_10pct.xml.gz";
		String secondPlansFile = "e:/_shared-svn/berlin_prognose_2025/baseplan_10x_900s.xml.gz";
		String outFile = "e:/_shared-svn/berlin_prognose_2025/out.xml.gz";
		CoordinateTransformation coordTransform = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);
		String preFix = "gv_";
		
		inPlansFile = inPlansFile.split("\\.")[0];

		int numberOfAdditionalCopies = 1;
		double radiusOfPerimeter = 1000.0;

		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(networkFile);

		Population inPop = sc.getPopulation();
		PopulationReader popReader = new MatsimPopulationReader(sc);
		popReader.readFile(inPlansFile + ".xml.gz");

		DuplicatePlans dp = new DuplicatePlans(net, inPop, "tmp.xml.gz", numberOfAdditionalCopies);
		dp.run(inPop);
		dp.writeEndPlans();

		System.out.println("Dublicating plans finished");
		Gbl.printElapsedTime();

		inPop = new ScenarioImpl().getPopulation();
		popReader = new MatsimPopulationReader(new SharedNetScenario(sc, inPop));
		popReader.readFile("tmp.xml.gz");

		ShuffleCoords shuffleCoords = new ShuffleCoords(net, inPop, "tmp2.xml.gz", radiusOfPerimeter, coordTransform);
		shuffleCoords.setChangeHomeActsOnlyOnceTrue("gvHome");
		shuffleCoords.run(inPop);
		shuffleCoords.writeEndPlans();
		
		System.out.println("Shuffle coords finished");
		Gbl.printElapsedTime();
		
		inPop = new ScenarioImpl().getPopulation();
		popReader = new MatsimPopulationReader(new SharedNetScenario(sc, inPop));
		popReader.readFile("tmp2.xml.gz");
		
		AddPrefixToPersonId ap = new AddPrefixToPersonId(net, inPop, inPlansFile + "_" + (numberOfAdditionalCopies + 1) + "x.xml.gz", preFix);
		ap.run(inPop);
		ap.writeEndPlans();
		
		System.out.println("Renaming person ids finished");
		Gbl.printElapsedTime();

		(new File("tmp.xml.gz")).deleteOnExit();
		(new File("tmp2.xml.gz")).deleteOnExit();
		
		inPop = new ScenarioImpl().getPopulation();
		popReader = new MatsimPopulationReader(new SharedNetScenario(sc, inPop));
		popReader.readFile(inPlansFile + "_" + (numberOfAdditionalCopies + 1) + "x.xml.gz");
		popReader.readFile(secondPlansFile);
		
		PopulationWriter popWriter = new PopulationWriter(inPop, net);
		popWriter.write(outFile);
		

		Gbl.printElapsedTime();
	}
}
