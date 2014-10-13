/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.pieter.singapore.utils.events;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

import others.sergioo.util.dataBase.DataBaseAdmin;
import playground.pieter.singapore.utils.events.listeners.GetPersonIdsCrossingLinkSelection;
import playground.pieter.singapore.utils.postgresql.PostgresType;
import playground.pieter.singapore.utils.postgresql.PostgresqlCSVWriter;
import playground.pieter.singapore.utils.postgresql.PostgresqlColumnDefinition;

public class PersonsAffectedbyLinkToSQL {

	/**
	 * @param args
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException {
		DataBaseAdmin dba = new DataBaseAdmin(new File("f:\\data\\matsim2postgres.properties"));
		HashSet<String> linkIds = new HashSet<>();
		linkIds.add("25755");
		linkIds.add("58340");
		linkIds.add("61153");
		linkIds.add("61590");
		linkIds.add("61153");
		linkIds.add("61590");
		linkIds.add("34161");
		linkIds.add("26443");
		linkIds.add("73266");
		
		EventsManager events = EventsUtils.createEventsManager();
		// first, read through the events file and get all person ids crossing
		// the link set
		GetPersonIdsCrossingLinkSelection idFinder = new GetPersonIdsCrossingLinkSelection(
				linkIds);
		events.addHandler(idFinder);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse("F:\\data\\sing2.2\\40.events.xml.gz");
		String actTableName = "u_fouriep.idsforlinkanalysis";
		List<PostgresqlColumnDefinition> columns = new ArrayList<>();
		columns.add(new PostgresqlColumnDefinition("person_id",
				PostgresType.TEXT));
		PostgresqlCSVWriter idwriter = new PostgresqlCSVWriter("ACTS",
				actTableName, dba, 100000, columns);
		HashSet<String> personIds = idFinder.getPersonIds();
		for(String id:personIds){
			Object[] idargs = {
					
					id
			};
			
			idwriter.addLine(idargs);
			
		}
		idwriter.finish();
	}

}
