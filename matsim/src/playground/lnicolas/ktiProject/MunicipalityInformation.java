/* *********************************************************************** *
 * project: org.matsim.*
 * MunicipalityInformation.java
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

package playground.lnicolas.ktiProject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.world.Zone;

/**
 * Class that stores information about a municipality.
 * @author lnicolas
 *
 */
public class MunicipalityInformation {

	IdImpl id = null;

	int personCount = -1;

	int urbanizationIndex = -1;

	double avgGasPrice = -1;

	double avgIncome2000 = -1;

	double avgWorkingIncome2003 = -1;

	double avgRetireeIncome2003 = -1;

	private double populationDensity;

	/**
	 * @param id
	 * @param personCount
	 * @param urbanizationIndex
	 * @param avgGasPrice
	 * @param avgIncome2000
	 * @param avgWorkingIncome2003
	 * @param avgRetireeIncome2003
	 */
	public MunicipalityInformation(IdImpl id, int personCount, int urbanizationIndex,
			double avgGasPrice, double avgIncome2000, double avgWorkingIncome2003,
			double avgRetireeIncome2003) {
		super();
		this.id = id;
		this.personCount = personCount;
		this.urbanizationIndex = urbanizationIndex;
		this.avgGasPrice = avgGasPrice;
		this.avgIncome2000 = avgIncome2000;
		this.avgWorkingIncome2003 = avgWorkingIncome2003;
		this.avgRetireeIncome2003 = avgRetireeIncome2003;
	}

	/**
	 * @return the avgGasPrice
	 */
	public double getAvgGasPrice() {
		return avgGasPrice;
	}

	/**
	 * @return the avgIncome2000
	 */
	public double getAvgIncome2000() {
		return avgIncome2000;
	}

	/**
	 * @return the avgRetireeIncome2003
	 */
	public double getAvgRetireeIncome2003() {
		return avgRetireeIncome2003;
	}

	/**
	 * @return the avgWorkingIncome2003
	 */
	public double getAvgWorkingIncome2003() {
		return avgWorkingIncome2003;
	}

//	/**
//	 * @return the id
//	 */
//	private String getId() {
//		return id;
//	}

	/**
	 * @return the personCount
	 */
	public int getPersonCount() {
		return personCount;
	}

	/**
	 * @return the urbanizationIndex
	 */
	public int getUrbanizationIndex() {
		return urbanizationIndex;
	}

	private void setPopulationDensity(double density) {
		this.populationDensity = density;
	}

	public double getPopulationDensity() {
		return populationDensity;
	}

	public static TreeMap<Id, MunicipalityInformation> readTabbedMunicipalityInfo(
			String filename, ArrayList<Zone> zones) {
		FileReader fileReader;
		TreeMap<Id, MunicipalityInformation> mInfo = new TreeMap<Id, MunicipalityInformation>();
		int lineCount = DatapulsPopulationGenerator.getLineCount(filename);

		System.out.println("reading municipality information...");
		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		try {
			fileReader = new FileReader(filename);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			// Skip header
			String currentLine;
			currentLine = bufferedReader.readLine();
			int lineIndex = 0;
			while ((currentLine = bufferedReader.readLine()) != null) {

				String[] entries = currentLine.split("\t", -1);

				IdImpl id = new IdImpl(entries[0].trim());
				int personCount = Integer.parseInt(entries[1].trim());
				int urbanizationIndex = Integer.parseInt(entries[2].trim());
				double avgGasPrice = Double.parseDouble(entries[3].trim());
				double avgIncome2000 = Double.parseDouble(entries[4].trim());
				double avgWorkingIncome2003 = Double.parseDouble(entries[5].trim());
				double avgRetireeIncome2003 = Double.parseDouble(entries[6].trim());

				mInfo.put(id, new MunicipalityInformation(id, personCount, urbanizationIndex,
						avgGasPrice, avgIncome2000, avgWorkingIncome2003, avgRetireeIncome2003));

				lineIndex++;
				if (lineIndex % (lineCount / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}

			bufferedReader.close();
		} catch (FileNotFoundException e) {
			Gbl.errorMsg(e);
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}

		System.out.println("done.");

		setPopulationDensity(mInfo, zones);

		return mInfo;
	}

	public static void setPopulationDensity(TreeMap<Id, MunicipalityInformation> municipalityInfo, ArrayList<Zone> zones) {
		for (Zone zone : zones) {
			MunicipalityInformation mInfo =
				municipalityInfo.get(zone.getId());
			if (mInfo != null) {
				mInfo.setPopulationDensity(
						mInfo.getPersonCount() / zone.getArea());
			} else {
//				Gbl.errorMsg("No MunicipalityInformation for zone " + zone.getName()
//						+ " (" + zone.getId() + ")");
			}
		}
	}
}
