/* *********************************************************************** *
 * project: org.matsim.*
 * SetEmployedFlagFromMz.java
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
package playground.thibautd.initialdemandgeneration;
 
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.utils.MoreIOUtils;

/**
 * Simple utility which takes a "population" of activity chains from the mikrozensus
 * and a MZ file linking persons IDs with the employement status, and returns a the
 * activity chains with the employed status set.
 *
 * @author thibautd
 */
public class SetEmployedFlagFromMz {
	private static final Logger log =
		Logger.getLogger(SetEmployedFlagFromMz.class);

	public static void main( final String[] args ) {
		String plansFile = args[ 0 ];
		String mzPersonFile = args[ 1 ];
		String wegeFile = args[ 2 ];
		String outDir = args[ 3 ];

		MoreIOUtils.initOut( outDir );

		StatusLogger status = new StatusLogger();
		status.start( " Beginning employement status setting");
		log.info( " activity chains from file: "+plansFile);
		log.info( " microcensus personal data from: "+mzPersonFile);
		log.info( " id to zpnr file: "+wegeFile);
		log.info( " output directory: "+outDir);

		status.start( " importing population" );
		Scenario scen = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		(new MatsimPopulationReader( scen )).parse( plansFile );
		status.done();

		status.start( " importing data");
		MzEmployementData data = new MzEmployementData( mzPersonFile , wegeFile );
		status.done();

		int count = 0;
		int educCount = 0;
		int emplCount = 0;
		int nullCount = 0;

		status.start( " processing data" );
		for ( Person person : scen.getPopulation().getPersons().values() ) {
			Boolean employed;
			Boolean student;

			try {
				employed = data.getIsEmployed( person.getId() );
				student = data.getIsStudent( person.getId() );
			} catch (MzEmployementData.UnknownIdException e) {
				throw new RuntimeException( "found unknown id after "+count+" activity chains were processed", e );
			}

			((PersonImpl) person).setEmployed( employed );
			if (employed != null && employed) emplCount++;
			if (student != null && student) {
				((PersonImpl) person).createDesires( "education" ).putActivityDuration( "e" , 1234 );
				educCount++;
			}

			if ( employed == null ) nullCount++;
			count++;
		}
		status.done();

		log.info( count+" activity chains were processed");
		log.info( educCount+" activity chains corresponded to student employement status");
		log.info( emplCount+" activity chains corresponded to employed employement status");
		log.info( nullCount+" activity chains corresponded to unknown employement status");

		status.start( " outputing results" );
		String outfileName = outFileName( plansFile );
		(new PopulationWriter( scen.getPopulation() , null )).write( outDir + "/" + outfileName );
		status.done();
		status.done();
	}

	private static String outFileName( final String plansFile ) {
		String[] path = plansFile.split("/");

		String fileName = path[ path.length - 1 ];
		int index = fileName.lastIndexOf( ".xml" );
		fileName = fileName.substring(0, index) + "-withEmployement" + fileName.substring( index );
		return fileName;
	}
}

class StatusLogger {
	private static final Logger log =
		Logger.getLogger(StatusLogger.class);

	private final Stack<String> msgs = new Stack<String>();;

	public void start(final String msg) {
		msgs.push( msg );
		log.info( msg + "..." );
	}

	public void done() {
		log.info( msgs.pop() +"... DONE." );
	}
}

class MzEmployementData {
	// name of fields in MZ2005
	private static final String ID_FIELD = "ZIELPNR";
	private static final String EMPLOYEMENT_FIELD = "F44";
	private static final String WG_ID_FIELD = "ID_PERSON";
	private static final String WG_ZP_FIELD = "ZIELPNR";
	private static final List<String> EMPLOYED_VALUES =
			Arrays.asList( "1","2","3" );
	private static final List<String> UNEMPLOYED_VALUES =
			Arrays.asList( "4","5","6","7","9","10" );
	private static final List<String> EDUCATION_VALUES =
			Arrays.asList( "8" );
	private static final List<String> NO_ANSWER_VALUES =
			Arrays.asList( "11" , "-97" );

	private final Map<Id, Boolean> employed = new HashMap<Id, Boolean>();
	private final Map<Id, Boolean> student = new HashMap<Id, Boolean>();
	private final Map<Id, Id> idToZpnr = new HashMap<Id, Id>();
	private final StatusLogger status = new StatusLogger();

	public MzEmployementData( final String mzPersonFile, final String wegeketteFile ) {
		processMz( mzPersonFile );
		processId( wegeketteFile );
	}

	private void processMz( final String mzPersonFile ) {
		// extract mz data
		status.start( "importing MZ data" );
		BufferedReader reader = IOUtils.getBufferedReader( mzPersonFile );
		String[] line = nextLine( reader );

		int idIndex = -1;
		int emplIndex = -10;
		int length = line.length;

		Counter count = new Counter( "importing line #" );

		for (int i=0; i < line.length; i++) {
			if (line[i].equals( ID_FIELD )) idIndex = i;
			else if (line[i].equals( EMPLOYEMENT_FIELD )) emplIndex = i;
		}

		line = nextLine( reader );
		while (line != null) {
			count.incCounter();
			if (line.length != length) {
				throw new RuntimeException( "found a line of "+line.length+"fields. "
						+length+" fields were expected.");
			}
			
			Id  id = new IdImpl( line[ idIndex ] );
			employed.put(
					id,
					employement( line[ emplIndex ].trim() ));
			student.put(
					id,
					education( line[ emplIndex ].trim() ));

			line = nextLine( reader );
		}

		status.done();
	}

	private void processId( final String file ) {
		status.start( "importing ID data" );

		BufferedReader reader = IOUtils.getBufferedReader( file );
		String[] line = nextLine( reader );

		int idIndex = -1;
		int zpIndex = -10;
		int length = line.length;

		Counter count = new Counter( "importing line #" );

		for (int i=0; i < line.length; i++) {
			if (line[i].equals( WG_ID_FIELD )) idIndex = i;
			else if (line[i].equals( WG_ZP_FIELD )) zpIndex = i;
		}

		line = nextLine( reader );
		while (line != null) {
			count.incCounter();
			if (line.length != length) {
				throw new RuntimeException( "found a line of "+line.length+"fields. "
						+length+" fields were expected.");
			}
			
			idToZpnr.put(
					new IdImpl( line[ idIndex ] ),
					new IdImpl( line[ zpIndex ] ));

			line = nextLine( reader );
		}

		status.done();
	}

	private Boolean employement( final String emplValue ) {
		if (EMPLOYED_VALUES.contains( emplValue )) {
			return true;
		}
		else if (
				UNEMPLOYED_VALUES.contains( emplValue ) ||
				EDUCATION_VALUES.contains( emplValue ) ) {
			return false;
		}
		else if (NO_ANSWER_VALUES.contains( emplValue )) {
			return null;
		}
		
		throw new IllegalArgumentException( "unknown employement status "+emplValue );
	}

	private Boolean education( final String emplValue ) {
		if (EDUCATION_VALUES.contains( emplValue )) {
			return true;
		}
		else if (
				UNEMPLOYED_VALUES.contains( emplValue ) ||
				EMPLOYED_VALUES.contains( emplValue ) ) {
			return false;
		}
		else if (NO_ANSWER_VALUES.contains( emplValue )) {
			return null;
		}

		throw new IllegalArgumentException( "unknown employement status "+emplValue );
	}

	private String[] nextLine(final BufferedReader reader) {
		try {
			String line = reader.readLine();
			return line == null ? null : line.split("\t");
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private Boolean getValue( final Id id , final Map<Id, Boolean> values ) throws UnknownIdException {
		Id zp = idToZpnr.get( id );
		if ((zp == null) || !values.containsKey( zp )) throw new UnknownIdException( "no id "+id+" in MZ" );
		return values.get( zp );
	}

	public Boolean getIsEmployed( final Id id ) throws UnknownIdException {
		return getValue( id, employed );
	}

	public Boolean getIsStudent( final Id id ) throws UnknownIdException {
		return getValue( id, student );
	}

	public static class UnknownIdException extends Exception {
		private static final long serialVersionUID = 1L;

		private UnknownIdException( final String msg ) {
			super( msg );
		}
	}
}

