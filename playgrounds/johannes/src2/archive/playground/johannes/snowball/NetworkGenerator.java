/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGenerator.java
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

/**
 * 
 */
package playground.johannes.snowball;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;

import playground.johannes.socialnets.GraphStatistics;
import playground.johannes.socialnets.PersonGraphMLFileHandler;
import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.io.GraphMLFile;
import edu.uci.ics.jung.random.generators.BarabasiAlbertGenerator;
import edu.uci.ics.jung.random.generators.ErdosRenyiGenerator;
import edu.uci.ics.jung.random.generators.WattsBetaSmallWorldGenerator;

/**
 * @author illenberger
 *
 */
public class NetworkGenerator {
	
	private static Logger logger = Logger.getLogger(NetworkGenerator.class);

	private static final String NETWORK_TYPE_RANDOM = "random";
	
	private static final String NETWORK_TYPE_SMALLWORLD = "smallworld";
	
	private static final String NETWORK_TYPE_BARABSI_ALBERT = "barabsialbert";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(args);
		
		final String MODULE_NAME = "snowballsampling";
		String graphFile = config.getParam(MODULE_NAME, "outputFile");
		String networkType = config.getParam(MODULE_NAME, "networktype");
		int numEdges = Integer.parseInt(config.getParam(MODULE_NAME, "numEdges"));
		int numVertices = Integer.parseInt(config.getParam(MODULE_NAME, "numVertices"));
		double param1 = Double.parseDouble(config.getParam(MODULE_NAME, "param1"));
		int degree = Integer.parseInt(config.getParam(MODULE_NAME, "degree"));
		long randomseed = Gbl.getConfig().global().getRandomSeed();
		
		
		Graph g = null;
		if(NETWORK_TYPE_RANDOM.equals(networkType)) {
			logger.info("Generating random network... n = " + numVertices + ", p = " + param1);
			ErdosRenyiGenerator generator = new ErdosRenyiGenerator(numVertices, param1);
			generator.setSeed(randomseed);
			g = (Graph)generator.generateGraph();
			initUserDatums(g);
		} else if(NETWORK_TYPE_SMALLWORLD.equals(networkType)) {
			logger.info("Generating small world network... n = " + numVertices + ", p = " + param1 + ", k = " + degree);
			WattsBetaSmallWorldGenerator generator = new WattsBetaSmallWorldGenerator(numVertices, param1, degree);
			g = (Graph) generator.generateGraph();
			initUserDatums(g);
		} else if(NETWORK_TYPE_BARABSI_ALBERT.equals(networkType)) {
			logger.info("Generating barabsi alber network... n = " + numVertices + ", m per step = " + numEdges + ", steps = " + (int)param1);
			BarabasiAlbertGenerator generator = new BarabasiAlbertGenerator(numVertices, numEdges, false, false, (int) randomseed);
			generator.evolveGraph((int)param1);
			g = (Graph) generator.generateGraph();
			initUserDatums(g);
		}
		logger.info("Computing graph statistics...");
		int n = g.numVertices();
		int m = g.numEdges();
		float density = m/((float)(n*(n-1))*0.5f);
		float k = (float) GraphStatistics.createDegreeHistogram(g, -1, -1, 0).getMean();
		float C = (float)GraphStatistics.createClusteringCoefficientsHistogram(g, -1, -1, 0).mean();
		float n_iso = GraphStatistics.countIsolates(g);
		logger.info(String.format("n = %1$s, m = %2$s, density = %3$s, k = %4$s, C = %5$s, n_isolates = %6$s.", n, m, density, k, C, n_iso));
		
		logger.info("Saving network...");
		GraphMLFile gmlFile = new GraphMLFile();
		gmlFile.save(g, graphFile);
		logger.info("Done.");
	}

	private static void initUserDatums(Graph g) {
		int counter = 0;
		for(Object v : g.getVertices()) {
			((Vertex)v).addUserDatum(UserDataKeys.ID, String.valueOf(counter), UserDataKeys.COPY_ACT);
			((Vertex)v).addUserDatum(UserDataKeys.X_COORD, Math.random(), UserDataKeys.COPY_ACT);
			((Vertex)v).addUserDatum(UserDataKeys.Y_COORD, Math.random(), UserDataKeys.COPY_ACT);
		}
	}
}
