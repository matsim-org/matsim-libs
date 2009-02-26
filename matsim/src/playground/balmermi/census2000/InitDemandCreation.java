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

import org.matsim.basic.v01.IdImpl;
import org.matsim.config.ConfigWriter;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.MatsimMatricesReader;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.world.MatsimWorldReader;
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

	public static void createInitDemand() {

		System.out.println("MATSim-IIDM: create initial demand based on census2000 data.");

		System.out.println("  reading world xml file... ");
		final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
		worldReader.readFile(Gbl.getConfig().world().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading facilities xml file... ");
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE, null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		System.out.println("  done.");

		System.out.println("  reading matrices xml file... ");
		MatsimMatricesReader reader = new MatsimMatricesReader(Matrices.getSingleton(), Gbl.getWorld());
		reader.readFile(Gbl.getConfig().matrices().getInputFile());
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  parsing additional municipality information... ");
		Municipalities municipalities = new Municipalities("input/gg25_2001_infos.txt");
		municipalities.parse();
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
		Population plans = new PopulationImpl(PopulationImpl.USE_STREAMING);
		PopulationWriter plansWriter = new PopulationWriter(plans);
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		System.out.println("  adding person modules... ");
		plans.addAlgorithm(new PersonLicenseModel(persons));
		plans.addAlgorithm(new PersonDistributeActChains(actchains));
		plans.addAlgorithm(new PersonSetHomeLoc(facilities, persons));
		plans.addAlgorithm(new PersonSetPrimLoc(facilities, Matrices.getSingleton(),persons));
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
		PersonZoneSummary pzs = new PersonZoneSummary((ZoneLayer)Gbl.getWorld().getLayer(new IdImpl("municipality")),persons,"output/output_zones.txt");
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
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();
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
		MatricesWriter mat_writer = new MatricesWriter(Matrices.getSingleton());
		mat_writer.write();
		System.out.println("  done.");

		System.out.println("  writing facilities xml file... ");
		FacilitiesWriter fac_writer = new FacilitiesWriter(facilities);
		fac_writer.write();
		System.out.println("  done.");

		System.out.println("  writing world xml file... ");
		WorldWriter world_writer = new WorldWriter(Gbl.getWorld());
		world_writer.write();
		System.out.println("  done.");

		System.out.println("  writing config xml file... ");
		ConfigWriter config_writer = new ConfigWriter(Gbl.getConfig());
		config_writer.write();
		System.out.println("  done.");

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		Gbl.createConfig(args);

		createInitDemand();

		Gbl.printElapsedTime();
	}
}
