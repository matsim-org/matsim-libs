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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierService;
import org.matsim.freight.carriers.CarrierShipment;
import org.matsim.freight.carriers.CarriersUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collection of different methods for the FreightDemandGeneration.
 *
 * @author Ricardo Ewert
 *
 */
public class FreightDemandGenerationUtils {
	private static final Logger log = LogManager.getLogger(FreightDemandGenerationUtils.class);

	/**
	 * Adds the home coordinates to attributes and removes plans
	 *
	 * @param population
	 * @param sampleSizeInputPopulation
	 * @param sampleTo
	 * @param samplingOption
	 */
	static void preparePopulation(Population population, double sampleSizeInputPopulation, double sampleTo,
			String samplingOption) {
		List<Id<Person>> personsToRemove = new ArrayList<>();
		population.getAttributes().putAttribute("sampleSize", sampleSizeInputPopulation);
		population.getAttributes().putAttribute("samplingTo", sampleTo);
		population.getAttributes().putAttribute("samplingOption", samplingOption);

		for (Person person : population.getPersons().values()) {
			if (person.getAttributes().getAsMap().containsKey("subpopulation")
					&& !person.getAttributes().getAttribute("subpopulation").toString().equals("person")) {
				personsToRemove.add(person.getId());
				continue;
			}
			Coord homeCoord = null;
			if (person.getSelectedPlan() != null) {
				if (PopulationUtils.getActivities(person.getSelectedPlan(),
					TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream().anyMatch(
					activity -> activity.getType().contains("home"))) {
					homeCoord = PopulationUtils.getActivities(person.getSelectedPlan(),
						TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream().filter(
						activity -> activity.getType().contains("home")).findFirst().get().getCoord();
				}
			} else if (!person.getPlans().isEmpty())
				for (Plan plan : person.getPlans())
					if (PopulationUtils.getActivities(plan,
						TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream().anyMatch(
						activity -> activity.getType().contains("home"))) {
						homeCoord = PopulationUtils.getActivities(plan,
							TripStructureUtils.StageActivityHandling.ExcludeStageActivities).stream().filter(
							activity -> activity.getType().contains("home")).findFirst().get().getCoord();
					}
			if (homeCoord == null){
				double home_x = (double) person.getAttributes().getAsMap().entrySet().stream().filter(
					entry -> entry.getKey().contains("home") && entry.getKey().contains("X") || entry.getKey().contains("x")).findFirst().get().getValue();
				double home_y = (double) person.getAttributes().getAsMap().entrySet().stream().filter(
					entry -> entry.getKey().contains("home") && (entry.getKey().contains("Y") || entry.getKey().contains("y"))).findFirst().get().getValue();
				homeCoord = new Coord(home_x, home_y);
			}


			if (homeCoord != null) {
				person.getAttributes().putAttribute("homeX", homeCoord.getX());
				person.getAttributes().putAttribute("homeY", homeCoord.getY());
			} else {
				log.warn("No home found for person " + person.getId());
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
		File file = new File(controler.getConfig().controller().getOutputDirectory() + "/outputFacilitiesFile.tsv");
		try (FileWriter writer = new FileWriter(file, true)) {
			writer.write("id	x	y	type	ServiceLocation	pickupLocation	deliveryLocation\n");

			for (Carrier thisCarrier : CarriersUtils.getCarriers(controler.getScenario()).getCarriers().values()) {
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
					writer.write(thisCarrier.getId().toString() + thisShipment.getId() + "	"
							+ coordTo.getX() + "	" + coordTo.getY() + "	" + "Delivery" + "		"
							+ thisShipment.getFrom() + "	" + thisShipment.getTo() + "\n");
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
	 * @param givenCoord
	 * @param indexShape
	 * @param possibleAreas
	 * @param crsTransformationNetworkAndShape
	 * @return
	 */
	static boolean checkPositionInShape(Link link, Coord givenCoord, ShpOptions.Index indexShape,
										String[] possibleAreas, CoordinateTransformation crsTransformationNetworkAndShape) {
		if (indexShape == null)
			return true;
		Coord coordToCheck = null;
		if (link != null && givenCoord == null) {
			if (crsTransformationNetworkAndShape != null)
				coordToCheck = crsTransformationNetworkAndShape.transform(getCoordOfMiddlePointOfLink(link));
			else
				coordToCheck = getCoordOfMiddlePointOfLink(link);
		} else if (link == null && givenCoord != null)
			coordToCheck = givenCoord;
		if (possibleAreas != null) {
			for (String area : possibleAreas) {
				if (Objects.equals(indexShape.query(coordToCheck), area)) {
					return true;
				}
			}
		} else {
			return indexShape.contains(coordToCheck);
		}
		return false;
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
