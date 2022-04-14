/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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
package org.matsim.freightDemandGeneration;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection of different methods for the FreightDemandGeneration.
 * 
 * @author Ricardo Ewert
 *
 */
public class FreightDemandGenerationUtils {
	private static final Logger log = Logger.getLogger(FreightDemandGenerationUtils.class);

	/**
	 * Adds the home coordinates to attributes and removes plans
	 * 
	 * @param population
	 * @param sampleSizeInputPopulation
	 * @param sampleTo
	 * @param samlingOption
	 */
	static void preparePopulation(Population population, double sampleSizeInputPopulation, double sampleTo,
			String samlingOption) {
		List<Id<Person>> personsToRemove = new ArrayList<>();
		population.getAttributes().putAttribute("sampleSize", sampleSizeInputPopulation);
		population.getAttributes().putAttribute("samplingTo", sampleTo);
		population.getAttributes().putAttribute("samplingOption", samlingOption);

		for (Person person : population.getPersons().values()) {
			if (person.getAttributes().getAsMap().containsKey("subpopulation")
					&& !person.getAttributes().getAttribute("subpopulation").toString().equals("person")) {
				personsToRemove.add(person.getId());
				continue;
			}
			for (Plan plan : person.getPlans())
				for (PlanElement element : plan.getPlanElements())
					if (element instanceof Activity)
						if (((Activity) element).getType().contains("home")) {
							double x = ((Activity) element).getCoord().getX();
							double y = ((Activity) element).getCoord().getY();
							person.getAttributes().putAttribute("homeX", x);
							person.getAttributes().putAttribute("homeY", y);
							break;
						}
			person.removePlan(person.getSelectedPlan());
		}
		for (Id<Person> id : personsToRemove)
			population.removePerson(id);
	}

	/**
	 * Creates a tsv file with the locations of all created demand elements.
	 * 
	 * @param controler
	 */
	static void createDemandLocationsFile(Controler controler) {

		Network network = controler.getScenario().getNetwork();
		File file = new File(controler.getConfig().controler().getOutputDirectory() + "/outputFacilitiesFile.tsv");
		try (FileWriter writer = new FileWriter(file, true)) {
			writer.write("id	x	y	type	ServiceLocation	pickupLocation	deliveryLocation\n");

			for (Carrier thisCarrier : FreightUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
				for (CarrierService thisService : thisCarrier.getServices().values()) {
					Coord coord = FreightDemandGenerationUtils
							.getCoordOfMiddlePointOfLink(network.getLinks().get(thisService.getLocationLinkId()));
					writer.write(thisCarrier.getId().toString() + thisService.getId().toString() + "	" + coord.getX()
							+ "	" + coord.getY() + "	" + "Service" + "	"
							+ thisService.getLocationLinkId().toString() + "		" + "\n");
				}
				for (CarrierShipment thisShipment : thisCarrier.getShipments().values()) {
					Coord coordFrom = FreightDemandGenerationUtils
							.getCoordOfMiddlePointOfLink(network.getLinks().get(thisShipment.getFrom()));
					Coord coordTo = FreightDemandGenerationUtils
							.getCoordOfMiddlePointOfLink(network.getLinks().get(thisShipment.getTo()));

					writer.write(thisCarrier.getId().toString() + thisShipment.getId().toString() + "	"
							+ coordFrom.getX() + "	" + coordFrom.getY() + "	" + "Pickup" + "		"
							+ thisShipment.getFrom().toString() + "	" + thisShipment.getTo().toString() + "\n");
					writer.write(thisCarrier.getId().toString() + thisShipment.getId().toString() + "	"
							+ coordTo.getX() + "	" + coordTo.getY() + "	" + "Delivery" + "		"
							+ thisShipment.getFrom().toString() + "	" + thisShipment.getTo().toString() + "\n");
				}
			}
			writer.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Wrote job locations file under " + "/outputLocationFile.xml.gz");
	}

	/**
	 * Reduces the population to all persons having their home in the shape
	 * 
	 * @param population
	 * @param index
	 */
	static void reducePopulationToShapeArea(Population population, ShpOptions.Index index) {

		List<Id<Person>> personsToRemove = new ArrayList<>();
		for (Person person : population.getPersons().values()) {

			if (!person.getAttributes().getAsMap().containsKey("homeX")
					|| !person.getAttributes().getAsMap().containsKey("homeY"))
				throw new RuntimeException(
						"The coordinates of the home facility are not part of the attributes a person. Please check!");

			double x = (double) person.getAttributes().getAttribute("homeX");
			double y = (double) person.getAttributes().getAttribute("homeY");

			if (!index.contains(new Coord(x, y)))
				personsToRemove.add(person.getId());
		}

		for (Id<Person> id : personsToRemove)
			population.removePerson(id);
	}

	/**
	 * Checks if a link is one of the possible areas.
	 * 
	 * @param link
	 * @param point
	 * @param polygonsInShape
	 * @param possibleAreas
	 * @param crsTransformationNetworkAndShape
	 * @return
	 */
	static boolean checkPositionInShape(Link link, Point point, Collection<SimpleFeature> polygonsInShape,
			String[] possibleAreas, CoordinateTransformation crsTransformationNetworkAndShape) {

		if (polygonsInShape == null)
			return true;
		boolean isInShape = false;
		Point p = null;
		if (link != null && point == null) {
			if (crsTransformationNetworkAndShape != null)
				p = MGC.coord2Point(crsTransformationNetworkAndShape.transform(getCoordOfMiddlePointOfLink(link)));
			else
				p = MGC.coord2Point(getCoordOfMiddlePointOfLink(link));
		} else if (link == null && point != null)
			p = point;
		for (SimpleFeature singlePolygon : polygonsInShape) {
			if (possibleAreas != null) {
				for (String area : possibleAreas) {
					if (area.equals(singlePolygon.getAttribute("Ortsteil"))
							|| area.equals(singlePolygon.getAttribute("BEZNAME")))
						if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
							isInShape = true;
							return isInShape;
						}
				}
			} else {
				if (((Geometry) singlePolygon.getDefaultGeometry()).contains(p)) {
					isInShape = true;
					return isInShape;
				}
			}
		}
		return isInShape;
	}

	/**
	 * Creates the middle coord of a link.
	 * 
	 * @param link
	 * @return Middle coord of the Link
	 */
	static Coord getCoordOfMiddlePointOfLink(Link link) {

		double x, y, xCoordFrom, xCoordTo, yCoordFrom, yCoordTo;
		xCoordFrom = link.getFromNode().getCoord().getX();
		xCoordTo = link.getToNode().getCoord().getX();
		yCoordFrom = link.getFromNode().getCoord().getY();
		yCoordTo = link.getToNode().getCoord().getY();
		if (xCoordFrom > xCoordTo)
			x = xCoordFrom - ((xCoordFrom - xCoordTo) / 2);
		else
			x = xCoordTo - ((xCoordTo - xCoordFrom) / 2);
		if (yCoordFrom > yCoordTo)
			y = yCoordFrom - ((yCoordFrom - yCoordTo) / 2);
		else
			y = yCoordTo - ((yCoordTo - yCoordFrom) / 2);

		return MGC.point2Coord(MGC.xy2Point(x, y));
	}
}