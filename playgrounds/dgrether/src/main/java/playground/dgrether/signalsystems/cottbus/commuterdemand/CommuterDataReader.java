/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterDataReader
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
package playground.dgrether.signalsystems.cottbus.commuterdemand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author jbischoff
 * 
 */
public class CommuterDataReader {

	private static final Logger log = Logger.getLogger(CommuterDataReader.class);
	private List<String> filteredMunicipalities;
	private List<CommuterDataElement> CommuterRelations;

	public CommuterDataReader() {
		this.CommuterRelations = new ArrayList<CommuterDataElement>();
		this.filteredMunicipalities = new LinkedList<String>();
	}

	public void addFilterRange(int comm) {
		log.info("Adding municipalities starting with " + comm);

		for (int i = 0; i < 1000; i++) {
			Integer community = comm + i;
			this.filteredMunicipalities.add(community.toString());
		}

	}

	public void addFilter(String comm) {
		this.filteredMunicipalities.add(comm);
	}

	public void printMunicipalities() {
		for (CommuterDataElement cde : this.CommuterRelations) {
			System.out.println(cde);
		}
	}

	public void readFile(String filename) {
		log.info("Reading commuter files from" + filename);
		FileReader fr;
		try {

			fr = new FileReader(new File(filename));
			BufferedReader br = new BufferedReader(fr);
			String line = null;
			String currentFromCode = null;
			String currentFromName = null;
			while ((line = br.readLine()) != null) {
				String[] result = line.split(";");
				try {
					if (!result[0].equals("")) {
						currentFromCode = result[0];
						currentFromName = result[1];
						log.info("Handling From Municipality " + currentFromCode);
						continue;
					}
					else {
						String currentTo = result[2];
						String currentToName = result[3];
						if (currentTo.equals("")) {
							log.error("possible data error, will skip this line: \n " + line);
							continue;
						}
						else if (this.filteredMunicipalities.contains(currentTo)
								&& this.filteredMunicipalities.contains(currentFromCode))

							try {
								int commuters = Integer.parseInt(result[4]);
								CommuterDataElement current = new CommuterDataElement(currentFromCode, currentTo,
										commuters);
								current.setFromName(currentFromName);
								current.setToName(currentToName);
								this.CommuterRelations.add(current);
							} catch (NumberFormatException n) {
								log.error("invalid line format, will skip this line: \n" + line);
							}

					}
				} catch (ArrayIndexOutOfBoundsException ae) {
					log.error("Found possible dataless line , if not check: " + line);
					continue;
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("read " + this.CommuterRelations.size() + " commuter relations");
	}

	public List<CommuterDataElement> getCommuterRelations() {
		return CommuterRelations;
	}

}
