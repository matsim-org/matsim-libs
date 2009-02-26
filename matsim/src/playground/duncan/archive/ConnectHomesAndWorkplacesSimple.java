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

import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;

public class ConnectHomesAndWorkplacesSimple {

	public void run(final String[] args) {

		Facilities facilities = new Facilities() ;
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader( facilities ) ;
		fr.readFile( "lsfd" ) ;

		Population population = new PopulationImpl() ;
		MatsimPopulationReader pr = new MatsimPopulationReader ( population, null ) ;
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
