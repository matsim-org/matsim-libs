/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.qiuhan.sa;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;

/**
 * @author Q. SUN
 * 
 */
public class MatrixToPopulation {
	private Set<String> legModes = null;
	Population pop;
	private final Scenario scenario;

	private MatrixToPopulation(Scenario scenario) {
		this.scenario = scenario;
        pop = PopulationUtils.createPopulation(((ScenarioImpl) scenario).getConfig(), ((ScenarioImpl) scenario).getNetwork());
	}

	public MatrixToPopulation(Scenario scenario, Set<String> legModes) {
		this(scenario);
		this.legModes = legModes;
	}

	/**
	 * converts matrices to Pop, where activity happens on coordinates of
	 * "bezugspunkt" in bezirks/zones
	 * 
	 * @param matricesPath
	 * @param zoneIdCoords
	 *            contains information about coordinates of "bezugspunkts"
	 */
	public void readMatrices(String matricesPath,
			Map<String, Coord> zoneIdCoords) {
		// output/matrices/1.mtx
		for (int i = 1; i <= 24; i++) {
			Matrix smallM = new Matrix(Integer.toString(i), "[matrix from\t"
					+ (i - 1) + "\tto\t" + i + "]");
			new VisumMatrixReader(smallM).readFile(matricesPath
					+ Integer.toString(i) + ".mtx");

			// this.smallMs.put(i, smallM);

			Map<Id, Person> persons = new MatrixToPersons(smallM, zoneIdCoords,
					(NetworkImpl) scenario.getNetwork(), legModes)
					.createPersons();
			for (Person per : persons.values()) {
				pop.addPerson(per);
			}
		}
	}

	/**
	 * converts matrices to Pop, where activity happens on random coordinates in
	 * bezirks/zones
	 * 
	 * @param matricesPath
	 * @param zoneFeatures
	 *            contains information about polygon of zones
	 */
	public void readMatrices(String matricesPath, SimpleFeatureSource fts) {
		// output/matrices/1.mtx
		for (int i = 1; i <= 24; i++) {
			Matrix smallM = new Matrix(Integer.toString(i), "[matrix from\t"
					+ (i - 1) + "\tto\t" + i + "]");
			new VisumMatrixReader(smallM).readFile(matricesPath
					+ Integer.toString(i) + ".mtx");

			// this.smallMs.put(i, smallM);

			Map<Id, Person> persons = new MatrixToPersons(smallM, fts,
					(NetworkImpl) scenario.getNetwork(), legModes)
					.createPersons();
			for (Person per : persons.values()) {
				pop.addPerson(per);
			}
		}
	}

	public void writePopulation(String populationFilename, Network network) {
		new PopulationWriter(pop, network).write(populationFilename);
	}

	/**
	 * @param args
	 */
	public static void runClassic(String[] args) {
		// TODO read 24 matrices to population and write a population file
		String matricesPath = "output/matrices2/";
		String zoneFilename = "output/matsimNetwork/Zone2.log";

		// String networkFilename = "output/matsimNetwork/networkBerlin2.xml";
		// String outputPopulationFilename =
		// "output/population/pop2wnplTest.xml.gz";
		//
		// String outputPopulationFilename = "output/population/pop2.xml";

		String networkFilename = "output/matsimNetwork/combi.xml.gz";
		String outputPopulationFilename = "output/population/pop2wnplCombiTest.xml.gz";

		ZoneReader zones = new ZoneReader();
		zones.readFile(zoneFilename);
		Map<String, Coord> zoneIdCoords = zones.getZoneIdCoords();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		MatrixToPopulation mtp = new MatrixToPopulation(scenario, null);
		mtp.readMatrices(matricesPath, zoneIdCoords);
		mtp.writePopulation(outputPopulationFilename, scenario.getNetwork());
	}

	/**
	 * @param args
	 */
	public static void run4carSubNetwork(String[] args) {
		// TODO read 24 matrices to population and write a population file
		String matricesPath = "output/matrices2/";
		String zoneFilename = "output/matsimNetwork/Zone2.log";
		String networkFilename = "output/matsimNetwork/networkBerlin2car.xml";
		String outputPopulationFilename = "output/population/pop4carNetwork.xml";

		ZoneReader zones = new ZoneReader();
		zones.readFile(zoneFilename);
		Map<String, Coord> zoneIdCoords = zones.getZoneIdCoords();

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		Set<String> legModes = new HashSet<String>();
		legModes.add(TransportMode.car);
		legModes.add(TransportMode.pt);
		MatrixToPopulation mtp = new MatrixToPopulation(scenario, legModes);
		mtp.readMatrices(matricesPath, zoneIdCoords);
		mtp.writePopulation(outputPopulationFilename, scenario.getNetwork());
	}

	public static void runClassic2(String[] args) {
		// read 24 matrices to population and write a population file
		String matricesPath = "output/matrices2/";
		String zoneShapeFilename = "input/zoneShp/shape_zone.SHP";

		// String networkFilename = "output/matsimNetwork/networkBerlin2.xml";
		// String outputPopulationFilename =
		// "output/population/pop2wnplTest.xml.gz";
		// String outputPopulationFilename = "output/population/pop2.xml";

		String networkFilename = "output/matsimNetwork/combi.xml.gz";
		String outputPopulationFilename = "output/population/popRandomInShp.xml.gz";

		// reads the shape file in
		SimpleFeatureSource fts = ShapeFileReader.readDataFile(zoneShapeFilename);

		// ZoneReader zones = new ZoneReader();
		// zones.readFile(zoneShapeFilename);
		// Map<String, Coord> zoneIdCoords = zones.getZoneIdCoords();

		// TODO very much things about create persons ...
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		MatrixToPopulation mtp = new MatrixToPopulation(scenario, null);
		mtp.readMatrices(matricesPath, fts);
		mtp.writePopulation(outputPopulationFilename, scenario.getNetwork());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		runClassic2(args);
		// run4carSubNetwork(args);
	}

}
