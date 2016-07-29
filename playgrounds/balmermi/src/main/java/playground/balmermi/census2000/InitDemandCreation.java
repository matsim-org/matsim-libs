/* *********************************************************************** *
 * project: org.matsim.*
 * InitDemandCreation.java
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

package playground.balmermi.census2000;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.population.io.StreamingUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;

import playground.balmermi.census2000.data.ActChains;
import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.Persons;
import playground.balmermi.census2000.modules.PersonCensusSummaryTables;
import playground.balmermi.census2000.modules.PersonDistributeActChains;
import playground.balmermi.census2000.modules.PersonLicenseModel;
import playground.balmermi.census2000.modules.PersonMobilityToolModel;
import playground.balmermi.census2000.modules.PersonModeChoiceModel;
import playground.balmermi.census2000.modules.PersonMunicipalitySummaryTable;
import playground.balmermi.census2000.modules.PersonRoundTimes;
import playground.balmermi.census2000.modules.PersonSetHomeLoc;
import playground.balmermi.census2000.modules.PersonSetPrimLoc;
import playground.balmermi.census2000.modules.PersonSetSecLoc;
import playground.balmermi.census2000.modules.PersonSummary;
import playground.balmermi.census2000.modules.PersonZoneSummary;
import playground.balmermi.census2000.modules.PersonsSummaryTable;
import playground.balmermi.world.Layer;
import playground.balmermi.world.World;
import playground.balmermi.world.ZoneLayer;

public class InitDemandCreation {

	//////////////////////////////////////////////////////////////////////
	// createInitDemand()
	//////////////////////////////////////////////////////////////////////

	public static void createInitDemand(String[] args) {

		System.out.println("MATSim-IIDM: create initial demand based on census2000 data.");

		Config config = ConfigUtils.loadConfig(args[0]);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		World world = new World();
		

		System.out.println("  reading matrices xml file... ");
		Matrices matrices = new Matrices();
		MatsimMatricesReader reader = new MatsimMatricesReader(matrices, scenario);
		reader.readFile(null /*filename not specified*/);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/gg25_2001_infos.txt");
		municipalities.parse(world.getLayer(Id.create("municipality", Layer.class)));
		System.out.println("  done.");

		System.out.println("  parsing household information... ");
		Households households = new Households(municipalities,"input/households2000.txt");
		households.parse();
		System.out.println("  done.");

		System.out.println("  parsing person information... ");
		Persons persons = new Persons(households,"input/persons2000.txt");
		persons.parse();
		System.out.println("  done.");

		System.out.println("  parsing act chain information... ");
		ActChains actchains = new ActChains("input/microcensus2000Tue2ThuWeighted.txt");
		actchains.parse();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		// plans setup
		//////////////////////////////////////////////////////////////////////

		System.out.println("  setting up plans objects...");
//		Population sReader = (Population) scenario.getPopulation();
		StreamingPopulationReader sReader = new StreamingPopulationReader( scenario ) ;
		StreamingUtils.setIsStreaming(sReader, true);
		StreamingPopulationWriter plansWriter = new StreamingPopulationWriter(null, scenario.getNetwork());
		plansWriter.startStreaming(null);//config.plans().getOutputFile());
//		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		sReader.addAlgorithm(new PersonLicenseModel(persons));
		sReader.addAlgorithm(new PersonDistributeActChains(actchains));
		sReader.addAlgorithm(new PersonSetHomeLoc(facilities, persons));
		sReader.addAlgorithm(new PersonSetPrimLoc(facilities, matrices, persons, (ZoneLayer)world.getLayer(Id.create("municipality", Layer.class))));
		sReader.addAlgorithm(new PersonSetSecLoc(facilities, persons));
		sReader.addAlgorithm(new PersonMobilityToolModel(persons));
		sReader.addAlgorithm(new PersonModeChoiceModel(persons));
		//////////////////////////////////////////////////////////////////////
		PersonsSummaryTable pst = new PersonsSummaryTable("output/summaryTable.txt");
		final PersonAlgorithm algo = pst;
		sReader.addAlgorithm(algo);
		PersonSummary ps = new PersonSummary();
		final PersonAlgorithm algo1 = ps;
		sReader.addAlgorithm(algo1);
		PersonCensusSummaryTables pcst = new PersonCensusSummaryTables("output/output_persons.txt",persons);
		final PersonAlgorithm algo2 = pcst;
		sReader.addAlgorithm(algo2);
		PersonMunicipalitySummaryTable pmst = new PersonMunicipalitySummaryTable("output/output_municipalities.txt",persons);
		final PersonAlgorithm algo3 = pmst;
		sReader.addAlgorithm(algo3);
		PersonZoneSummary pzs = new PersonZoneSummary((ZoneLayer)world.getLayer(Id.create("municipality", Layer.class)),persons,"output/output_zones.txt");
		final PersonAlgorithm algo4 = pzs;
		sReader.addAlgorithm(algo4);
		//////////////////////////////////////////////////////////////////////
		PersonRoundTimes prt = new PersonRoundTimes();
		final PersonAlgorithm algo5 = prt; // must be last one!!!
		sReader.addAlgorithm(algo5);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		// plans processing
		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		final PersonAlgorithm algo6 = plansWriter;
		sReader.addAlgorithm(algo6);
//		plansReader.readFile(config.plans().getInputFile());
		sReader.readFile(config.plans().getInputFile());
		PopulationUtils.printPlansCount(sReader) ;
		plansWriter.closeStreaming();
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		// finalize person algorithms
		//////////////////////////////////////////////////////////////////////

		ps.print();
		prt.print();
		pst.close();
		pcst.close();
		pmst.close();
		pzs.close();

		//////////////////////////////////////////////////////////////////////
		// writing down data
		//////////////////////////////////////////////////////////////////////

		System.out.println("  write data to files... ");
		households.writeTable("output/output_households.txt");
		persons.writeTable("output/output_persons.txt");
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  writing matrices xml file... ");
		MatricesWriter mat_writer = new MatricesWriter(matrices);
		mat_writer.write(null /*filename not specified*/);
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(null);//config.facilities().getOutputFile());
		System.out.println("  done.");

//		System.out.println("  writing world xml file... ");
//		WorldWriter world_writer = new WorldWriter(world);
//		world_writer.write("output/output_facilities.xml");
//		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		createInitDemand(args);

		Gbl.printElapsedTime();
	}
}
