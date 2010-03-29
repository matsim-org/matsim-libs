/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkDistance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yalcin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

public class NetworkDistance {

	private final static String bikeCoordsFilename = "../mystudies/yalcin/BikeCoordinates.txt";
	private final static String networkFilename = "../mystudies/yalcin/berlin_wipnet.xml";
	private final static String bikeDistancesFilename = "../mystudies/yalcin/bikeDistances.txt";
	private final static String wgs84NetworkFilename = "../mystudies/yalcin/berlin_wipnet_wgs84b.xml";
	private final static String networkKmzFilename = "../mystudies/yalcin/berlin_wipnet.kmz";

	public static void exportNetwork() {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);

		ObjectFactory kmlObjectFactory = new ObjectFactory();

		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainDoc.setId("berlin_wipnet");
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		// create a folder
		FolderType mainFolder = kmlObjectFactory.createFolderType();
		mainFolder.setId("2dnetworklinksfolder");
		mainFolder.setName("Matsim Data");
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mainFolder));
		// the writer
		KMZWriter writer = new KMZWriter(networkKmzFilename);
		try {
			// add the matsim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(logo));
			KmlNetworkWriter netWriter = new KmlNetworkWriter(network, ct, writer, mainDoc);
			FolderType networkFolder = netWriter.getNetworkFolder();
			mainFolder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(networkFolder));
		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo because of: " + e.getMessage());
			e.printStackTrace();
		}
		writer.writeMainKml(mainKml);
		writer.close();
		System.out.println("done");
	}

	public static void convertNetwork() {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFilename);

		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.DHDN_GK4, TransformationFactory.WGS84);
		new NetworkTransform(ct).run(network);
		new NetworkWriter(network).writeFile(wgs84NetworkFilename);
	}

	public static void findDistances() {
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(wgs84NetworkFilename);

		// set the config to only look at distance costs, not travel time costs
		Config config = Gbl.createConfig(null);
		config.charyparNagelScoring().setTraveling(0.0);
		config.charyparNagelScoring().setMarginalUtlOfDistanceCar(-0.001); // -1 per kilometer == -0.001 per meter

		// create the router algorithm
		TravelTime travelTime = new FreespeedTravelTimeCost(config.charyparNagelScoring());
		TravelCost linkCosts = new TravelTimeDistanceCostCalculator(travelTime, config.charyparNagelScoring());
		Dijkstra router = new Dijkstra(network, linkCosts, travelTime);

		// we need to transform the coordinate from wgs84 to gk4 for calculating distances
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.DHDN_GK4);

		// process the file line after line
		try {
			BufferedReader reader = IOUtils.getBufferedReader(bikeCoordsFilename);
			BufferedWriter writer = IOUtils.getBufferedWriter(bikeDistancesFilename);
			String header = reader.readLine();
			writer.write("Index\tKundenindex\tcrowflyDist\tnetworkDist\tfromNode\ttoNode\tlinks...\n");

			Counter counter = new Counter("line ");
			String line = reader.readLine();
			while (line != null) {
				counter.incCounter();
				String[] parts = StringUtils.explode(line, '\t');
				/* parts[0]: Index
				 * parts[1]: Kundenindex
				 * parts[2]: Fromstr1
				 * parts[3]: Fromstr2
				 * parts[4]: FromX
				 * parts[5]: FromY
				 * parts[6]: Tostr1
				 * parts[7]: Tostr2
				 * parts[8]: ToX
				 * parts[9]: ToY
				 */
				Coord fromCoord = new CoordImpl(parts[4], parts[5]);
				Coord toCoord = new CoordImpl(parts[8], parts[9]);
				Node fromNode = network.getNearestNode(fromCoord);
				Node toNode = network.getNearestNode(toCoord);

				Path path = router.calcLeastCostPath(fromNode, toNode, 0);

				double crowflyDistance = CoordUtils.calcDistance(ct.transform(fromCoord), ct.transform(toCoord));

				writer.write(parts[0] + "\t" + parts[1] + "\t" + crowflyDistance + "\t" + getPathDistance(path));

				writer.write("\t" + fromNode.getId().toString());
				writer.write("\t" + toNode.getId().toString());
				for (Link link : path.links) {
					writer.write("\t" + link.getId().toString());
				}

				writer.write("\n");
				// ----
				line = reader.readLine();
			}
			counter.printCounter();

			reader.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

	}

	private static double getPathDistance(final Path path) {
		double dist = 0;
		for (Link link : path.links) {
			dist += link.getLength();
		}
		return dist;
	}

	public static void main(String[] args) {
//		exportNetwork(); // exports the network as GoogleEarth KMZ. NOTE: This can be quite big for GoogleEarth!
		convertNetwork();
		findDistances();
	}

}
