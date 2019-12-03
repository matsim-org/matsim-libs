package org.matsim.osmNetworkReader;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

@Log4j2
class OsmNetworkParser {

	static NodesAndWays parse(Path inputFile, ConcurrentMap<String, LinkProperties> linkProperties) {

		log.info("start reading ways");

		var executor = Executors.newWorkStealingPool();
		var waysParser = new ParallelWaysPbfParser(executor, linkProperties);

		try (var fileInputStream = new FileInputStream(inputFile.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
            waysParser.parse(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("finished reading ways.");
        log.info("Kept " + waysParser.getWays().size() + "/" + waysParser.getCounter() + " ways");
        log.info("Marked " + waysParser.getNodes().size() + " nodes to be kept");
        log.info("starting to read nodes");

        throw new RuntimeException("not implemented");
/*
		var nodesParser = new NodesPbfParser(null);//waysParser.getNodes());


		try (var fileInputStream = new FileInputStream(inputFile.toFile())) {
			var input = new BufferedInputStream(fileInputStream);
			nodesParser.parse(input);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("finished reading nodes");
		log.info("Kept " + nodesParser.getNodes().size() + "/" + nodesParser.getCounter() + " nodes");
		return new NodesAndWays(nodesParser.getNodes(), waysParser.getWays());
		*/

    }
}
