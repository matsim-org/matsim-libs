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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.ZoneLayer;

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

public class InitDemandCreation {

	//////////////////////////////////////////////////////////////////////
	// createInitDemand()
	//////////////////////////////////////////////////////////////////////

	public static void createInitDemand(String[] args) {

		System.out.println("MATSim-IIDM: create initial demand based on census2000 data.");

		ScenarioLoaderImpl sl = new ScenarioLoaderImpl(args[0]);
		ScenarioImpl scenario = sl.getScenario();
		Config config = scenario.getConfig();
		World world = scenario.getWorld();

		System.out.println("  reading world xml file... ");
		new MatsimWorldReader(scenario).readFile(config.world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		sl.loadActivityFacilities();
		ActivityFacilitiesImpl facilities = sl.getScenario().getActivityFacilities();
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		Matrices matrices = new Matrices();
		MatsimMatricesReader reader = new MatsimMatricesReader(matrices, scenario);
		reader.readFile(config.matrices().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/gg25_2001_infos.txt");
		municipalities.parse(world.getLayer(new IdImpl("municipality")));
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
		PopulationImpl plans = (PopulationImpl) scenario.getPopulation();
		plans.setIsStreaming(true);
		PopulationWriter plansWriter = new PopulationWriter(plans, scenario.getNetwork());
		plansWriter.startStreaming(config.plans().getOutputFile());
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new PersonLicenseModel(persons));
		plans.addAlgorithm(new PersonDistributeActChains(actchains));
		plans.addAlgorithm(new PersonSetHomeLoc(facilities, persons));
		plans.addAlgorithm(new PersonSetPrimLoc(facilities, matrices, persons, (ZoneLayer)world.getLayer(new IdImpl("municipality"))));
		plans.addAlgorithm(new PersonSetSecLoc(facilities, persons));
		plans.addAlgorithm(new PersonMobilityToolModel(persons));
		plans.addAlgorithm(new PersonModeChoiceModel(persons));
		//////////////////////////////////////////////////////////////////////
		PersonsSummaryTable pst = new PersonsSummaryTable("output/summaryTable.txt");
		plans.addAlgorithm(pst);
		PersonSummary ps = new PersonSummary();
		plans.addAlgorithm(ps);
		PersonCensusSummaryTables pcst = new PersonCensusSummaryTables("output/output_persons.txt",persons);
		plans.addAlgorithm(pcst);
		PersonMunicipalitySummaryTable pmst = new PersonMunicipalitySummaryTable("output/output_municipalities.txt",persons);
		plans.addAlgorithm(pmst);
		PersonZoneSummary pzs = new PersonZoneSummary((ZoneLayer)world.getLayer(new IdImpl("municipality")),persons,"output/output_zones.txt");
		plans.addAlgorithm(pzs);
		//////////////////////////////////////////////////////////////////////
		PersonRoundTimes prt = new PersonRoundTimes(); // must be last one!!!
		plans.addAlgorithm(prt);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////
		// plans processing
		//////////////////////////////////////////////////////////////////////

		System.out.println("  reading, processing, writing plans...");
		plans.addAlgorithm(plansWriter);
		plansReader.readFile(config.plans().getInputFile());
		plans.printPlansCount();
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
		mat_writer.write(config.matrices().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(config.facilities().getOutputFile());
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(world);
		world_writer.write(config.world().getOutputFile());
		System.out.println("  done.");

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
