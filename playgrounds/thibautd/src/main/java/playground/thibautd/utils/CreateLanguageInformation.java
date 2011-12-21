/* *********************************************************************** *
 * project: org.matsim.*
 * CreateLanguageInformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;

import playground.balmermi.census2000.data.Households;
import playground.balmermi.census2000.data.Municipalities;
import playground.balmermi.census2000.data.MyPerson;
import playground.balmermi.census2000.data.Persons;
import playground.balmermi.world.Layer;
import playground.balmermi.world.Zone;

/**
 * Uses deprecated classes form playground.balmermi.census2000.data to generate
 * a text file containing agent Id and language information on each line
 * (g for german, f for french, i for italian)
 *
 * @author thibautd
 */
public class CreateLanguageInformation {
	private static final Logger log =
		Logger.getLogger(CreateLanguageInformation.class);

	private static final String GERMAN = "g";
	private static final String FRENCH = "f";
	private static final String ITALIAN = "i";

	public static void main(final String[] args) {
		String personsFile = args[ 0 ];
		String hhFile = args[ 1 ];
		String municipalitiesFile = args[ 2 ];
		String outputDir = args[ 3 ];

		MoreIOUtils.initOut( outputDir );

		log.info( "output dir: "+outputDir );
		log.info( "persons file: "+personsFile );
		log.info( "hh file: "+hhFile );
		log.info( "municipalitiesFile: "+municipalitiesFile );

		Municipalities municipalities = new Municipalities( municipalitiesFile );
		municipalities.parse( new MunicipalityLayer() );
		Households households = new Households( municipalities , hhFile );
		households.parse();
		Persons persons = new Persons( households , personsFile );
		persons.parse();

		BufferedWriter writer = IOUtils.getBufferedWriter( outputDir +"/id2lang.txt" );

		try {
			Counter count = new Counter( "setting language for person # " );
			for ( Map.Entry<Integer , MyPerson> person : persons.getPersons().entrySet() ) {
				count.incCounter();
				writer.write( person.getKey() +"\t"+ lang( person.getValue() ) );
				writer.newLine();
			}
			count.printCounter();

			writer.close();
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private static String lang(final MyPerson person) {
		int canton = person.getHousehold().getMunicipality().getCantonId();

		if ( (( 1 <= canton ) && ( canton <= 9 )) ||
				((11 <= canton) && (canton <= 20)) ) {
			return GERMAN;
		}
		if ( ((22 <= canton) && (canton <= 26)) || (canton == 10)) {
			return FRENCH;
		}
		if ( canton == 21 ) {
			return ITALIAN;
		}

		throw new RuntimeException( "unknown canton id "+canton );
	}
}

class MunicipalityLayer extends Zone implements Layer {
	private final Map<Id, BasicLocation> locations = new HashMap<Id, BasicLocation>();

	private static final Coord c = new CoordImpl( 0,0);

	public MunicipalityLayer() {
		super(new IdImpl( "m") , c , c , c );
		// TODO Auto-generated constructor stub
	}

	@Override
	public BasicLocation getLocation( Id id ) {
		BasicLocation loc = locations.get( id );
		if (loc == null) {
			loc = new Zone(id, c, c, c);
			locations.put(id , loc);
		}
		return loc;
	}

	@Override
	public Map<Id, BasicLocation> getLocations() {
		return locations;
	}

	@Override
	public ArrayList<BasicLocation> getNearestLocations(Coord coord) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ArrayList<BasicLocation> getNearestLocations(Coord coord,
			BasicLocation excludeLocation) {
		throw new UnsupportedOperationException();
	}
}
