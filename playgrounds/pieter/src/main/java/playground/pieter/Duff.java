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

package playground.pieter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class Duff {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException, SQLException, NoConnectionException {
		// TODO Auto-generated method stub
		DataBaseAdmin dba = new DataBaseAdmin(new File("f:/data/matsim2postgres.properties"));
		// need to update the transit stop ids so they are consistent with LTA
		// list
		String update = "		UPDATE " + "u_sergioo.matsim_trips_realsched_m2plans"
				+ " SET boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE "
				+ "u_sergioo.matsim_trips_realsched_m2plans"
				+ " SET alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE alighting_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE " + "u_sergioo.matsim_journeys_realsched_m2plans"
				+ " SET first_boarding_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE first_boarding_stop = matsim_stop ";
		dba.executeUpdate(update);
		update = "		UPDATE "
				+ "u_sergioo.matsim_journeys_realsched_m2plans"
				+ " SET last_alighting_stop = matsim_to_transitstops_lookup.stop_id "
				+ " FROM m_calibration.matsim_to_transitstops_lookup "
				+ " WHERE last_alighting_stop = matsim_stop ";
		dba.executeUpdate(update);

		HashMap<String, String[]> idxNames = new HashMap<>();
		String[] idx1 = { "person_id", "facility_id", "type" };
		idxNames.put("u_sergioo.matsim_activities_realsched_m2plans", idx1);
		String[] idx2 = { "person_id", "from_act", "to_act", "main_mode" };
		idxNames.put("u_sergioo.matsim_journeys_realsched_m2plans", idx2);
		String[] idx3 = { "journey_id", "mode", "line", "route",
				"boarding_stop", "alighting_stop" };
		idxNames.put("u_sergioo.matsim_trips_realsched_m2plans", idx3);
		String[] idx4 = { "journey_id", "from_trip", "to_trip" };
		idxNames.put("u_sergioo.matsim_transfers_realsched_m2plans", idx4);
		for (Entry<String, String[]> entry : idxNames.entrySet()) {
			String tableName = entry.getKey();
			String[] columnNames = entry.getValue();
			for (String columnName : columnNames) {
				String indexName = tableName.split("\\.")[1] + "_" + columnName;
				String fullIndexName = tableName.split("\\.")[0] + "."
						+ indexName;
				String indexStatement;
				try {
					indexStatement = "DROP INDEX IF EXISTS " + fullIndexName + " ;\n ";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

				try {
					indexStatement = "CREATE INDEX " + indexName + " ON "
							+ tableName + "(" + columnName + ");\n";
					dba.executeStatement(indexStatement);
					System.out.println(indexStatement);
				} catch (SQLException e) {
					e.printStackTrace();
				}

			}
		}
	}

}
