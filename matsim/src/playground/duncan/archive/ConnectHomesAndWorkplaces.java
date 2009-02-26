/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.duncan.archive;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import java.io.PrintWriter;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.RandomLocationMutator;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

public class ConnectHomesAndWorkplaces {

	Config config ;

	public void run(final String[] args) {

		// create/read the config:
		if ( args.length==0 ) {
			this.config = Gbl.createConfig(new String[] {"./src/playground/duncan/h2w-config.xml"});
		} else {
			this.config = Gbl.createConfig(args) ;
		}
		ConfigWriter configwriter = new ConfigWriter(this.config, new PrintWriter(System.out));
		configwriter.write();

		// create the control(l)er:
		final Controler controler = new Controler(Gbl.getConfig());
//		controler.loadData() ;
		// (I think that the control(l)er is only needed to make the locationchoice module happy;
		// there is no logical reason why it is necessary. Kai)

		// create/read the network:
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		// create/read the world (probably empty input file)
		final World world = Gbl.getWorld();
		if (this.config.world().getInputFile() != null) {
			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
			worldReader.readFile(this.config.world().getInputFile());
		}
		world.setNetworkLayer(network);
		world.complete();

		Facilities facilities = new Facilities() ;
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader( facilities ) ;
		fr.readFile( this.config.facilities().getInputFile() ) ;

		// create the locachoice object:
		LocationMutator locachoice = new RandomLocationMutator( controler.getNetwork(),controler) ;

		final Population plans = new PopulationImpl(PopulationImpl.USE_STREAMING);
		final PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		final PopulationWriter plansWriter = new PopulationWriter(plans);
		plans.addAlgorithm(locachoice);
		plans.addAlgorithm(plansWriter); // planswriter must be the last algorithm added

		// I don't know why this works:
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		ConnectHomesAndWorkplaces app = new ConnectHomesAndWorkplaces();
		app.run(args);
	}

}
