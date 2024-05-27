package org.matsim.contrib.shared_mobility.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.shared_mobility.io.DefaultSharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingServiceReader;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingStationSpecification;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.GeoFileWriter;

public class WriteStationShapefile {
	static public void main(String[] args) throws ConfigurationException {

		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("service-path", "network-path", "output-path", "crs") //
				.build();

		SharingServiceSpecification service = new DefaultSharingServiceSpecification();
		new SharingServiceReader(service).readFile(cmd.getOptionStrict("service-path"));

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));

		PointFeatureFactory featureFactory = new PointFeatureFactory.Builder() //
				.setCrs(crs) //
				.setName("stations") //
				.addAttribute("station_id", String.class) //
				.addAttribute("capacity", Integer.class) //
				.create();

		Collection<SimpleFeature> features = new ArrayList<>(service.getStations().size());

		for (SharingStationSpecification station : service.getStations()) {
			Link link = network.getLinks().get(station.getLinkId());

			Coordinate coordinate = new Coordinate( //
					link.getCoord().getX(), link.getCoord().getY());

			features.add(featureFactory.createPoint( //
					coordinate, //
					new Object[] { //
							station.getId().toString(), station.getCapacity() },
					null));
		}

		GeoFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}
