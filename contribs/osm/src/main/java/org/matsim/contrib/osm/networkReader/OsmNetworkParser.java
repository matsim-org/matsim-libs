package org.matsim.contrib.osm.networkReader;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;

class OsmNetworkParser {

	private static Logger log = Logger.getLogger(OsmNetworkParser.class);

	static NodesAndWays parse(Path inputFile, ConcurrentMap<String, LinkProperties> linkPropertiesMap, CoordinateTransformation transformation, BiPredicate<Coord, Integer> linkFilter) {

		log.info("start reading ways");

		ExecutorService executor = Executors.newWorkStealingPool();
		WaysPbfParser waysParser = new WaysPbfParser(executor, linkPropertiesMap);

		try (InputStream fileInputStream = new FileInputStream(inputFile.toFile())) {
			BufferedInputStream input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("finished reading ways.");
		log.info("Kept " + NumberFormat.getNumberInstance(Locale.US).format(waysParser.getWays().size()) + "/" +
				NumberFormat.getNumberInstance(Locale.US).format(waysParser.getCounter()) + " ways");
		log.info("Marked " + NumberFormat.getNumberInstance(Locale.US).format(waysParser.getNodes().size()) + " nodes to be kept");
		log.info("starting to read nodes");

		NodesPbfParser nodesParser = new NodesPbfParser(executor, linkFilter, waysParser.getNodes(), transformation);

		try (InputStream fileInputStream = new FileInputStream(inputFile.toFile())) {

			BufferedInputStream input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("finished reading nodes");
		log.info("Kept " + NumberFormat.getNumberInstance(Locale.US).format(nodesParser.getNodes().size()) + "/"
				+ NumberFormat.getNumberInstance(Locale.US).format(nodesParser.getCount()) + " nodes");

		return new NodesAndWays(nodesParser.getNodes(), waysParser.getWays());
	}
}
