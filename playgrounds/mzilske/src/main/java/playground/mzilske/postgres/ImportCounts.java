/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mzilske.postgres;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.xml.sax.SAXException;

public class ImportCounts {

	private static final String COUNTS_FILE_PREFIX = "../detailedEval/counts/counts";

	private void parseCounts() throws IOException {

		try {
			Connection connection;
			connection = DriverManager.getConnection("jdbc:postgresql:munich", "postgres", "postgres");

			DatabaseMetaData dbmd = connection.getMetaData();
			System.out.println("Connection to " + dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion() + " successful.\n");
			final Statement statement = connection.createStatement();
			final PreparedStatement statement2 = connection.prepareStatement("select messstellen.messstelle, messstellen.richtung, hour, sum, links.link_id from counts_by_hour, messstellen, links " +
					"where counts_by_hour.messstelle = messstellen.messstelle and counts_by_hour.richtung = messstellen.richtung and messstellen.link_id = links.link_id and counts_by_hour.date = ? ");

			ResultSet rs = statement.executeQuery("select distinct date from counts_by_hour");
			while (rs.next()) {
				Counts counts = new Counts();
				Counts rereadCounts = new Counts();
				CountsWriter countsWriter = new CountsWriter(counts);
				Date date = rs.getDate(1);
				System.out.println(date);
				statement2.setDate(1, date);
				ResultSet rss = statement2.executeQuery();
				while (rss.next()) {
					String messstelle = rss.getString(1);
					String richtung = rss.getString(2);
					int h = rss.getInt(3) + 1;
					int sum = rss.getInt(4);
					Id linkId = new IdImpl(rss.getString(5));
					Count count = counts.getCounts().get(linkId);
					if (count == null) {
						count = counts.createCount(linkId, messstelle+richtung);
					}
					count.createVolume(h, sum);
				}
				counts.setYear(666);
				counts.setName("Uwe");

				String filename = COUNTS_FILE_PREFIX + "-" + date + ".xml";
				countsWriter.write(filename);
				CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(rereadCounts);
				countsReader.parse(filename);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		ImportCounts importCounts = new ImportCounts();
		importCounts.parseCounts();
	}

}
