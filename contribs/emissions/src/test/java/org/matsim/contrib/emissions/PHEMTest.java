package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PHEMTest {

	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	// TODO DEBUG ONLY
	static CSVPrinter csvPrinter = null;
	FileWriter fileWriter = null;

	private static List<DrivingCycleSecond> readCycle(Path path){
		List<DrivingCycleSecond> drivingCycleSeconds = new ArrayList<>();

		var format = CSVFormat.DEFAULT.builder()
			.setDelimiter(";")
			.setSkipHeaderRecord(true)
			.setHeader()
			.build();

		try (var reader = Files.newBufferedReader(path); var parser = CSVParser.parse(reader, format)) {
			for(var record : parser){
				drivingCycleSeconds.add(new DrivingCycleSecond(
					Integer.parseInt(record.get(0)),
					Double.parseDouble(record.get(1)),
					Double.parseDouble(record.get(2))));
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return drivingCycleSeconds;
	}

	/**
	 * Reads a csv (header: time,speed,acceleration) containing a wltp cycle and converts it into a MATSim test-network, with links representing the
	 * phases of the cycle. The first link has its origin at (0,0) and the test-track extends into positive x.
	 * @param network Network to put in the links
	 * @param path path of the wltp.csv
	 */
	private static void createTestLinks(Network network, Path path){
		List<DrivingCycleSecond> drivingCycleSeconds = readCycle(path);
		for (var i : drivingCycleSeconds) {
			System.out.println(i);
		}

		/*
		Partitions the drivingCycleSeconds list into multiple disjunct lists, containing consecutive records.
		A new partition is created at every minimum. The minimum is determined by computing the delta value.
		If the sign changed form negative to positive, the minimum must be somewhere between this point and the last.
		NOTE: We are not using the acceleration as the data does not match numerically with the velocity.
		 */
		List<List<DrivingCycleSecond>> drivingSegments = new ArrayList<>();
		List<DrivingCycleSecond> currentList = new ArrayList<>();
		double lastDelta = 1;
		for (int sec = 1; sec < drivingCycleSeconds.size(); sec++) {
			double currentDelta = drivingCycleSeconds.get(sec).vel - drivingCycleSeconds.get(sec - 1).vel;

			/*
			We want to create a new link at
			1. every minimum
			2. every end of a standing period
			TODO currently, it is impossible to simulate standing times. How do we want to overcome this?
			TODO -> currently, the program creates links with 0 speed and length, which will probably cause problems
			 */
			if ((lastDelta <= 0 && currentDelta > 0)) {
				// We have found a minimum, crate a new sublist
				drivingSegments.add(currentList);
				currentList = new ArrayList<>();

				// TODO DBEUG ONLY
				try{
					csvPrinter.printRecord(drivingCycleSeconds.get(sec).second);
				} catch(Exception e){
					System.out.println(e);
				}

			}

			lastDelta = currentDelta;

			currentList.add(drivingCycleSeconds.get(sec));
		}

		// Add last segment
		drivingSegments.add(currentList);


		// Now create a link for each driving segment
		int i = 1;
		double dist_summed = 0;
		Node from_node = NetworkUtils.createAndAddNode(network, Id.createNodeId("n0"), new Coord(0, 0));
		for (var segment : drivingSegments) {
			// Compute the sum of the segment (which equals the numerical integral, thus the total distance)
			double len = segment.stream().map(s -> s.vel/3.6).reduce(0., Double::sum);
			double freespeed = (len / (segment.getLast().second+1 - segment.getFirst().second));

			Node to_node = NetworkUtils.createAndAddNode(network, Id.createNodeId("n" + i), new Coord(dist_summed + len, 0));
			Link link = NetworkUtils.createLink(Id.createLinkId("l" + i), from_node, to_node, network, len, freespeed, 10000, 1);
			network.addLink(link);
			from_node = to_node;
			dist_summed += len;
			i++;
		}
	}

	@Test
	public void test() throws IOException {
		// TODO DEBUG ONLY
		fileWriter = new FileWriter(utils.getOutputDirectory().toString() + "cuts.csv");
		csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);


		// Prepare test
		Path dir = Paths.get(utils.getClassInputDirectory()).resolve("short_wltp_cycle.csv");
		Network network = NetworkUtils.createNetwork();
		createTestLinks(network, dir);

		System.out.println();

		NetworkUtils.writeNetwork(network, utils.getOutputDirectory() + "net.xml");

		// TODO DEBUG ONLY
		fileWriter.flush();
		fileWriter.close();
	}

	private record DrivingCycleSecond(int second, double vel, double acc){}
}
