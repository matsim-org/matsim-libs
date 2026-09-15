/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * The service areas of the time-dependent service regimes of one DRT service, read from a GIS file. Every
 * polygon names the service regimes it belongs to, and those names are stamped onto the stops as their
 * {@value AttributeBasedStopFinder#FACILITY_STOP_NETWORKS_ATTRIBUTE} attribute. The stop set of a service
 * regime is therefore derived from the polygons and needs no hand-attributed network or stop file, while
 * everything downstream keeps working with the one stop attribute.
 *
 * @author nkuehnel / MOIA
 */
public final class DrtServiceAreas {

	private record ServiceArea(PreparedGeometry geometry, Set<String> stopNetworks) {
	}

	private final List<ServiceArea> serviceAreas;

	private DrtServiceAreas(List<ServiceArea> serviceAreas) {
		this.serviceAreas = serviceAreas;
	}

	/**
	 * @return the service areas of the service regimes, or an empty {@link java.util.Optional} if the DRT service
	 * has none (either no service regimes at all or no {@code serviceAreaFile})
	 */
	public static Optional<DrtServiceAreas> createIfConfigured(Config config, DrtConfigGroup drtCfg) {
		return drtCfg.getServiceRegimesParams()
				.filter(params -> params.getServiceAreaFile() != null)
				.map(params -> fromFile(
						ConfigGroup.getInputFileURL(config.getContext(), params.getServiceAreaFile()),
						params.getServiceAreaAttribute()));
	}

	/**
	 * @return true if the DRT service has service areas, i.e. if {@link #createIfConfigured} returns them. Unlike
	 * that method, this one does not read the file.
	 */
	public static boolean isConfigured(DrtConfigGroup drtCfg) {
		return drtCfg.getServiceRegimesParams().map(params -> params.getServiceAreaFile() != null).orElse(false);
	}

	/**
	 * The served area as a zone filter, i.e. which zones of the analysis and rebalancing zone systems are inside the
	 * service area. With service areas the union of the polygons is used, so the filter stays time-invariant even
	 * though the individual regimes are not. A service without any area serves everything.
	 */
	public static Predicate<Zone> servedAreaZoneFilter(Config config, DrtConfigGroup drtCfg,
			Optional<DrtServiceAreas> serviceAreas) {
		Verify.verify(serviceAreas.isPresent() == isConfigured(drtCfg),
				"The service areas of mode '%s' are configured but not bound (or the other way round). They are bound"
						+ " in DrtModeRoutingModule, which must be installed alongside.", drtCfg.getMode());
		if (serviceAreas.isPresent()) {
			return zone -> serviceAreas.get().intersects(zone.getPreparedGeometry().getGeometry());
		}
		if (drtCfg.getOperationalScheme() != DrtConfigGroup.OperationalScheme.serviceAreaBased) {
			return zone -> true;
		}
		List<PreparedGeometry> serviceAreaGeoms = ShpGeometryUtils.loadPreparedGeometries(
				ConfigGroup.getInputFileURL(config.getContext(), drtCfg.getDrtServiceAreaShapeFile()));
		return zone -> serviceAreaGeoms.stream()
				.anyMatch(serviceArea -> serviceArea.intersects(zone.getPreparedGeometry().getGeometry()));
	}

	public static DrtServiceAreas fromFile(URL url, String stopNetworksAttribute) {
		return fromFeatures(GeoFileReader.getAllFeatures(url), stopNetworksAttribute);
	}

	public static DrtServiceAreas fromFeatures(Collection<SimpleFeature> features, String stopNetworksAttribute) {
		Verify.verify(!features.isEmpty(), "The service area file does not contain any feature.");
		PreparedGeometryFactory factory = new PreparedGeometryFactory();
		List<ServiceArea> serviceAreas = features.stream()
				.map(feature -> new ServiceArea(factory.create((Geometry)feature.getDefaultGeometry()),
						parseStopNetworks(feature, stopNetworksAttribute)))
				.toList();
		return new DrtServiceAreas(serviceAreas);
	}

	private static Set<String> parseStopNetworks(SimpleFeature feature, String stopNetworksAttribute) {
		Verify.verify(feature.getFeatureType().getDescriptor(stopNetworksAttribute) != null,
				"The service area file has no attribute '%s'. Available attributes: %s. Note that column names of"
						+ " shapefiles are limited to 10 characters.", stopNetworksAttribute,
				feature.getFeatureType()
						.getAttributeDescriptors()
						.stream()
						.map(descriptor -> descriptor.getLocalName())
						.toList());
		Object value = feature.getAttribute(stopNetworksAttribute);
		if (value == null || value.toString().isBlank()) {
			// a polygon without stop networks only contributes to the served area
			return Set.of();
		}
		return Arrays.stream(value.toString().split(","))
				.map(String::trim)
				.filter(stopNetwork -> !stopNetwork.isEmpty())
				.collect(ImmutableSet.toImmutableSet());
	}

	/**
	 * @return the stop networks of all areas containing the given coordinate
	 */
	public Set<String> stopNetworksAt(Coord coord) {
		return stopNetworksOf(areasAt(coord));
	}

	public boolean covers(Coord coord) {
		return !areasAt(coord).isEmpty();
	}

	/**
	 * @return true if the given geometry intersects the served area, i.e. the union of all areas
	 */
	public boolean intersects(Geometry geometry) {
		return serviceAreas.stream().anyMatch(area -> area.geometry().intersects(geometry));
	}

	/**
	 * Builds the stop inventory of a {@code serviceAreaBased} service from the areas: every link whose toNode lies in
	 * one of the areas becomes a stop, tagged with the stop networks of the areas containing it.
	 */
	public DrtStopNetwork createStopNetwork(Network network) {
		ImmutableMap.Builder<Id<DrtStopFacility>, DrtStopFacility> builder = ImmutableMap.builder();
		for (Link link : network.getLinks().values()) {
			List<ServiceArea> areas = areasAt(link.getToNode().getCoord());
			if (areas.isEmpty()) {
				continue;
			}
			DrtStopFacility stop = tag(DrtStopFacilityImpl.createFromLink(link), stopNetworksOf(areas));
			builder.put(stop.getId(), stop);
		}
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> stops = builder.build();
		Verify.verify(!stops.isEmpty(), "None of the network links lies in one of the configured service areas.");
		return () -> stops;
	}

	/**
	 * Stamps the stop networks of the areas onto the stops of an existing inventory (e.g. the transit stops of a
	 * {@code stopbased} service). Stops outside all areas are kept unchanged, so a service regime without a
	 * stopNetwork still serves them.
	 */
	public DrtStopNetwork tag(DrtStopNetwork stopNetwork) {
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> stops = stopNetwork.getDrtStops()
				.values()
				.stream()
				.map(stop -> tag(stop, stopNetworksAt(stop.getCoord())))
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, stop -> stop));
		return () -> stops;
	}

	private List<ServiceArea> areasAt(Coord coord) {
		Point point = MGC.coord2Point(coord);
		return serviceAreas.stream().filter(area -> area.geometry().contains(point)).toList();
	}

	private static Set<String> stopNetworksOf(List<ServiceArea> areas) {
		return areas.stream().flatMap(area -> area.stopNetworks().stream()).collect(ImmutableSet.toImmutableSet());
	}

	private static DrtStopFacility tag(DrtStopFacility stop, Set<String> stopNetworks) {
		if (stopNetworks.isEmpty()) {
			return stop;
		}
		Set<String> merged = new TreeSet<>(AttributeBasedStopFinder.parseStopNetworks(stop));
		merged.addAll(stopNetworks);

		// a stop may share its attributes with the object it was created from, so they must be copied and not modified
		// in place
		Attributes attributes = new AttributesImpl();
		AttributesUtils.copyTo(stop.getAttributes(), attributes);
		attributes.putAttribute(AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE, String.join(",", merged));
		return new DrtStopFacilityImpl(stop.getId(), stop.getLinkId(), stop.getCoord(), attributes);
	}
}
