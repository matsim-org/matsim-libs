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

package playground.duncan;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import java.io.PrintWriter;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.controler.Controler;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.locationchoice.LocationChoice;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.RandomLocationMutator;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

public class ConnectHomesAndWorkplacesSimple {
	
	public void run(final String[] args) {

		Facilities facilities = new Facilities() ;
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader( facilities ) ;
		fr.readFile( "lsfd" ) ;
		
		Population population = new Population() ;
		MatsimPopulationReader pr = new MatsimPopulationReader ( population ) ;
		pr.readFile( "lsdkjf" ) ;

		// program locachoice here
		
		PopulationWriter popWriter = new PopulationWriter(population,"newfilename","v4",1 ) ;
		popWriter.write();

	}

	public static void main(final String[] args) {
		ConnectHomesAndWorkplacesSimple app = new ConnectHomesAndWorkplacesSimple();
		app.run(args);
	}

}
