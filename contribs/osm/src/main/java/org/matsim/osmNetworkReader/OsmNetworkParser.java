package org.matsim.osmNetworkReader;

import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;

@Log4j2
class OsmNetworkParser {

	static NodesAndWays parse(Path inputFile, ConcurrentMap<String, LinkProperties> linkPropertiesMap, CoordinateTransformation transformation, BiPredicate<Coord, Integer> linkFilter) {

		log.info("start reading ways");

		var executor = Executors.newWorkStealingPool();
		var waysParser = new WaysPbfParser(executor, linkPropertiesMap);

		try (var fileInputStream = new FileInputStream(inputFile.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			waysParser.parse(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("finished reading ways.");
		log.info("Kept " + NumberFormat.getNumberInstance(Locale.US).format(waysParser.getWays().size()) + "/" +
				NumberFormat.getNumberInstance(Locale.US).format(waysParser.getCounter()) + " ways");
		log.info("Marked " + NumberFormat.getNumberInstance(Locale.US).format(waysParser.getNodes().size()) + " nodes to be kept");
		log.info("starting to read nodes");

		var nodesParser = new NodesPbfParser(executor, linkFilter, waysParser.getNodes(), transformation);

		try (var fileInputStream = new FileInputStream(inputFile.toFile())) {

			var input = new BufferedInputStream(fileInputStream);
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
