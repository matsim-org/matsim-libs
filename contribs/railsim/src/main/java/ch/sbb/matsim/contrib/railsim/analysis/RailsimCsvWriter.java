package ch.sbb.matsim.contrib.railsim.analysis;

import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

public class RailsimCsvWriter {

	public static void writeLinkStatesCsv(List<RailsimLinkStateChangeEvent> events, String filename) throws UncheckedIOException {
		String[] header = {"link", "time", "state", "vehicle", "track"};

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(filename), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (RailsimLinkStateChangeEvent event : events) {
				csv.print(event.getLinkId().toString());
				csv.print(event.getTime());
				csv.print(event.getState().toString());
				csv.print(event.getVehicleId() != null ? event.getVehicleId().toString() : "");
				csv.print(event.getTrack());
				csv.println();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	public static void writeTrainStatesCsv(List<RailsimTrainStateEvent> events, Network network, String filename) throws UncheckedIOException {
		String[] header = {"vehicle", "time", "acceleration", "speed", "targetSpeed", "headLink", "headPosition", "headX", "headY", "tailLink", "tailPosition", "tailX", "tailY"};

		try (CSVPrinter csv = new CSVPrinter(IOUtils.getBufferedWriter(filename), CSVFormat.DEFAULT.builder().setHeader(header).build())) {
			for (RailsimTrainStateEvent event : events) {
				csv.print(event.getVehicleId().toString());
				csv.print(event.getTime());
				csv.print(event.getAcceleration());
				csv.print(event.getSpeed());
				csv.print(event.getTargetSpeed());

				csv.print(event.getHeadLink().toString());
				csv.print(event.getHeadPosition());
				if (network != null) {
					Link link = network.getLinks().get(event.getHeadLink());
					if (link != null) {
						double fraction = event.getHeadPosition() / link.getLength();
						Coord from = link.getFromNode().getCoord();
						Coord to = link.getToNode().getCoord();
						csv.print(from.getX() + (to.getX() - from.getX()) * fraction);
						csv.print(from.getY() + (to.getY() - from.getY()) * fraction);
					}
				} else {
					csv.print("");
					csv.print("");
				}

				csv.print(event.getTailLink().toString());
				csv.print(event.getTailPosition());
				if (network != null) {
					Link link = network.getLinks().get(event.getTailLink());
					if (link != null) {
						double fraction = event.getTailPosition() / link.getLength();
						Coord from = link.getFromNode().getCoord();
						Coord to = link.getToNode().getCoord();
						csv.print(from.getX() + (to.getX() - from.getX()) * fraction);
						csv.print(from.getY() + (to.getY() - from.getY()) * fraction);
					}
				} else {
					csv.print("");
					csv.print("");
				}

				csv.println();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

}
