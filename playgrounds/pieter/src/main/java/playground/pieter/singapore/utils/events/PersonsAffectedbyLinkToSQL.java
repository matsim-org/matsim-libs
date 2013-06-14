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
import java.util.Arrays;
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
		HashSet<String> linkIds = new HashSet<String>();
		linkIds.add("106026");
		linkIds.add("106025");
		
		EventsManager events = EventsUtils.createEventsManager();
		// first, read through the events file and get all person ids crossing
		// the link set
		GetPersonIdsCrossingLinkSelection idFinder = new GetPersonIdsCrossingLinkSelection(
				linkIds);
		events.addHandler(idFinder);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse("x:\\fourie\\data\\zoutput\\quicktest_toll_hotstart\\ITERS\\it.200\\0.1_0_100Q1P24.200.events.xml.gz");
		String actTableName = "u_fouriep.affected_after_hotstart";
		List<PostgresqlColumnDefinition> columns = new ArrayList<PostgresqlColumnDefinition>();
		columns.add(new PostgresqlColumnDefinition("personId",
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
