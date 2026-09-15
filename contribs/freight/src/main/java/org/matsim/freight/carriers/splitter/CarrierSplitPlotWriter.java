/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C)  by the members listed in the COPYING,            *
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

package org.matsim.freight.carriers.splitter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.core.config.ConfigUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CarrierSplitPlotWriter {

	private static final Logger log = LogManager.getLogger(CarrierSplitPlotWriter.class);
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;
	private static final int MARGIN = 60;

	private CarrierSplitPlotWriter() {
	}

	static void writeCarrierSplitPlot(Scenario scenario,
			CarrierSplitter.ClusteringStrategy clusteringStrategy,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		List<PlotPoint> points = collectPoints(scenario, shipmentClusteringLocation);
		if (points.isEmpty()) {
			log.info("No carrier jobs available. Skipping carrier split plot.");
			return;
		}

		Path outputFile = getOutputFile(scenario);
		try {
			Files.createDirectories(outputFile.getParent());
			BufferedImage image = createImage(points, clusteringStrategy, shipmentClusteringLocation);
			ImageIO.write(image, "png", outputFile.toFile());
			log.info("Carrier split plot written to: {}", outputFile);
		} catch (IOException e) {
			throw new RuntimeException("Could not write carrier split plot to " + outputFile, e);
		}
	}

	private static Path getOutputFile(Scenario scenario) {
		String runId = scenario.getConfig().controller().getRunId();
		String filename = runId == null ? "carrier_split.png" : runId + ".carrier_split.png";
		FreightCarriersConfigGroup freightConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), FreightCarriersConfigGroup.class);
		if (freightConfig.getCarriersFile() != null && !freightConfig.getCarriersFile().isBlank()) {
			Path carriersFile = Path.of(freightConfig.getCarriersFile());
			Path carrierDirectory = carriersFile.getParent();
			if (carrierDirectory != null) {
				return carrierDirectory.resolve(filename);
			}
		}
		return Path.of(scenario.getConfig().controller().getOutputDirectory()).resolve(filename);
	}

	private static List<PlotPoint> collectPoints(Scenario scenario,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		List<PlotPoint> points = new ArrayList<>();
		List<Carrier> carriers = CarriersUtils.getCarriers(scenario).getCarriers().values().stream()
				.sorted(Comparator.comparing(carrier -> carrier.getId().toString()))
				.toList();

		for (Carrier carrier : carriers) {
			String carrierId = carrier.getId().toString();
			carrier.getShipments().values().stream()
					.sorted(Comparator.comparing(shipment -> shipment.getId().toString()))
					.map(shipment -> new PlotPoint(getShipmentCoord(scenario, shipment, shipmentClusteringLocation),
							carrierId, PointType.SHIPMENT))
					.forEach(points::add);
			carrier.getServices().values().stream()
					.sorted(Comparator.comparing(service -> service.getId().toString()))
					.map(service -> new PlotPoint(getLinkCoord(scenario, service.getServiceLinkId().toString()),
							carrierId, PointType.SERVICE))
					.forEach(points::add);
			carrier.getCarrierCapabilities().getCarrierVehicles().values().stream()
					.sorted(Comparator.comparing(vehicle -> vehicle.getId().toString()))
					.map(vehicle -> new PlotPoint(getLinkCoord(scenario, vehicle.getLinkId().toString()),
							carrierId, PointType.DEPOT))
					.forEach(points::add);
		}
		return points;
	}

	private static Coord getShipmentCoord(Scenario scenario, CarrierShipment shipment,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		if (shipmentClusteringLocation == null) {
			throw new IllegalArgumentException("shipmentClusteringLocation must be specified explicitly when shipments are present.");
		}
		Coord pickupCoord = getLinkCoord(scenario, shipment.getPickupLinkId().toString());
		Coord deliveryCoord = getLinkCoord(scenario, shipment.getDeliveryLinkId().toString());
		return switch (shipmentClusteringLocation) {
			case PICKUP -> pickupCoord;
			case DELIVERY -> deliveryCoord;
			case MIDPOINT -> new Coord((pickupCoord.getX() + deliveryCoord.getX()) / 2,
					(pickupCoord.getY() + deliveryCoord.getY()) / 2);
		};
	}

	private static Coord getLinkCoord(Scenario scenario, String linkId) {
		Link link = scenario.getNetwork().getLinks().get(Id.createLinkId(linkId));
		if (link == null) {
			throw new IllegalArgumentException("Link " + linkId + " is missing from the network.");
		}
		return link.getCoord();
	}

	private static BufferedImage createImage(List<PlotPoint> points,
			CarrierSplitter.ClusteringStrategy clusteringStrategy,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, WIDTH, HEIGHT);

		Bounds bounds = Bounds.of(points);
		Map<String, Color> colors = carrierColors(points);
		drawTitle(graphics, points, clusteringStrategy, shipmentClusteringLocation);
		for (PlotPoint point : points) {
			drawPoint(graphics, point, bounds, colors.get(point.carrierId()));
		}
		drawLegend(graphics, points, shipmentClusteringLocation);
		graphics.dispose();
		return image;
	}

	private static void drawTitle(Graphics2D graphics, List<PlotPoint> points,
			CarrierSplitter.ClusteringStrategy clusteringStrategy,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		graphics.setColor(Color.DARK_GRAY);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
		long jobCount = points.stream().filter(point -> point.type() != PointType.DEPOT).count();
		long carrierCount = points.stream().map(PlotPoint::carrierId).distinct().count();
		String splitLocation = shipmentClusteringLocation == null ? "SERVICE" : shipmentClusteringLocation.name();
		graphics.drawString("Carrier split: " + jobCount + " jobs, " + carrierCount + " carriers, algorithm "
				+ clusteringStrategy.name() + ", split location " + splitLocation, MARGIN, 36);
	}

	private static void drawLegend(Graphics2D graphics, List<PlotPoint> points,
			CarrierSplitter.ShipmentClusteringLocation shipmentClusteringLocation) {
		int rowHeight = 26;
		int legendWidth = 260;
		int x = WIDTH - legendWidth - 60;
		int y = 55;
		boolean hasShipments = points.stream().anyMatch(point -> point.type() == PointType.SHIPMENT);
		String jobLocationLabel = hasShipments ? switch (shipmentClusteringLocation) {
			case PICKUP -> "pickups";
			case DELIVERY -> "deliveries";
			case MIDPOINT -> "midpoints";
		} : "services";
		int legendHeight = 52 + 2 * rowHeight;

		graphics.setColor(new Color(255, 255, 255, 235));
		graphics.fillRect(x, y, legendWidth, legendHeight);
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.drawRect(x, y, legendWidth, legendHeight);

		graphics.setColor(Color.DARK_GRAY);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
		graphics.drawString("Legend", x + 14, y + 24);

		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
		drawDepotLegendEntry(graphics, x + 18, x + 42, y + 54);
		drawJobLegendEntry(graphics, hasShipments, x + 18, x + 42, y + 54 + rowHeight, jobLocationLabel);
	}

	private static void drawJobLegendEntry(Graphics2D graphics, boolean hasShipments, int symbolX, int textX, int currentY, String label) {
		graphics.setColor(Color.GRAY);
		if (hasShipments)
			graphics.fillOval(symbolX - 5, currentY - 10, 10, 10);
		else
			graphics.fillRect(symbolX - 5, currentY - 10, 10, 10);
		graphics.setColor(Color.DARK_GRAY);
		graphics.drawString(label + " (color = carrier)", textX, currentY);
	}

	private static void drawDepotLegendEntry(Graphics2D graphics, int symbolX, int textX, int currentY) {
		graphics.setColor(Color.BLACK);
		int[] depotX = {symbolX, symbolX - 8, symbolX + 8};
		int[] depotY = {currentY - 13, currentY + 4, currentY + 4};
		graphics.fillPolygon(depotX, depotY, 3);
		graphics.setColor(Color.DARK_GRAY);
		graphics.drawString("depot", textX, currentY);
	}

	private static void drawPoint(Graphics2D graphics, PlotPoint point, Bounds bounds, Color color) {
		int x = scale(point.coord().getX(), bounds.minX(), bounds.maxX(), WIDTH - MARGIN);
		int y = HEIGHT - scale(point.coord().getY(), bounds.minY(), bounds.maxY(), HEIGHT - MARGIN);
		if (point.type() == PointType.DEPOT) {
			graphics.setColor(Color.BLACK);
			int[] xs = {x, x - 8, x + 8};
			int[] ys = {y - 10, y + 7, y + 7};
			graphics.fillPolygon(xs, ys, 3);
			graphics.setColor(Color.WHITE);
			graphics.setStroke(new BasicStroke(2f));
			graphics.drawPolygon(xs, ys, 3);
			return;
		}
		graphics.setColor(color);
		if (point.type() == PointType.SERVICE)
			graphics.fillRect(x - 5, y - 5, 10, 10);
		else
			graphics.fillOval(x - 5, y - 5, 10, 10);
	}

	private static int scale(double value, double min, double max, int upper) {
		if (max == min) {
			return (CarrierSplitPlotWriter.MARGIN + upper) / 2;
		}
		return (int) Math.round(CarrierSplitPlotWriter.MARGIN + (value - min) / (max - min) * (upper - CarrierSplitPlotWriter.MARGIN));
	}

	private static Map<String, Color> carrierColors(List<PlotPoint> points) {
		Map<String, Color> colors = new HashMap<>();
		List<String> carrierIds = points.stream().map(PlotPoint::carrierId).distinct().sorted().toList();
		for (int i = 0; i < carrierIds.size(); i++) {
			float hue = (float) i / carrierIds.size();
			colors.put(carrierIds.get(i), Color.getHSBColor(hue, 0.7f, 0.8f));
		}
		return colors;
	}

	private enum PointType {
		SHIPMENT, SERVICE, DEPOT
	}

	private record PlotPoint(Coord coord, String carrierId, PointType type) {
	}

	private record Bounds(double minX, double maxX, double minY, double maxY) {
		static Bounds of(List<PlotPoint> points) {
			double minX = points.stream().mapToDouble(point -> point.coord().getX()).min().orElse(0);
			double maxX = points.stream().mapToDouble(point -> point.coord().getX()).max().orElse(1);
			double minY = points.stream().mapToDouble(point -> point.coord().getY()).min().orElse(0);
			double maxY = points.stream().mapToDouble(point -> point.coord().getY()).max().orElse(1);
			return new Bounds(minX, maxX, minY, maxY);
		}
	}
}
