package org.matsim.contrib.shared_mobility.io;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class SharingServiceReader extends MatsimXmlParser {
	private final SharingServiceSpecification service;

	public SharingServiceReader(SharingServiceSpecification service) {
		super(ValidationType.DTD_ONLY);
		this.service = service;
	}

	@Override
	public void startTag(String name, Attributes attributes, Stack<String> context) {
		if (name.equals("vehicle")) {

			service.addVehicle(ImmutableSharingVehicleSpecification.newBuilder() //
					.id(Id.create(attributes.getValue("id"), SharingVehicle.class)) //
					.startLinkId(attributes.getValue("startLink")==null ? null : Id.createLinkId(attributes.getValue("startLink"))) //
					.startStationId(attributes.getValue("startStation")==null ? null : Id.create(attributes.getValue("startStation"), SharingStation.class)) //
					.build());
		} else if (name.equals("station")) {
			service.addStation(ImmutableSharingStationSpecification.newBuilder() //
					.id(Id.create(attributes.getValue("id"), SharingStation.class)) //
					.linkId(Id.createLinkId(attributes.getValue("link"))) //
					.capacity(Integer.parseInt(attributes.getValue("capacity"))) //
					.build());
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}
}
