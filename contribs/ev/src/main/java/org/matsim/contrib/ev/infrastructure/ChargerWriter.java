/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.contrib.ev.EvUnits;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

public final class ChargerWriter extends MatsimXmlWriter {
	private final Stream<? extends ChargerSpecification> chargerSpecifications;

    private Map<Class<?>, AttributeConverter<?>> attributeConverters = new HashMap<>();
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

	public ChargerWriter(Stream<? extends ChargerSpecification> chargerSpecifications) {
		this.chargerSpecifications = chargerSpecifications;
	}

	public void write(String file) {
		openFile(file);
		writeDoctype("chargers", "http://matsim.org/files/dtd/chargers_v1.dtd");
		writeStartTag("chargers", Collections.emptyList());
		writeChargers();
		writeEndTag("chargers");
		close();
	}

	private void writeChargers() throws UncheckedIOException {
		chargerSpecifications.forEach(c -> {
			List<Tuple<String, String>> atts = Arrays.asList(Tuple.of("id", c.getId().toString()),
					Tuple.of("link", c.getLinkId() + ""), Tuple.of("type", c.getChargerType()),
					Tuple.of("plug_power", EvUnits.W_to_kW(c.getPlugPower()) + ""),
					Tuple.of("plug_count", c.getPlugCount() + ""));
			if (c.getAttributes().size() == 0) {
				writeStartTag("charger", atts, true);
			} else {
				writeStartTag("charger", atts, false);

				try {
					this.writer.write("\n");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}

				attributesWriter.writeAttributes("\t\t", this.writer, c.getAttributes(), false);
				writeEndTag("charger");
			}
		});
	}

	public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
        this.attributeConverters.putAll(converters);
    }

    public void putAttributeConverter(Class<?> key, AttributeConverter<?> converter) {
        this.attributeConverters.put(key, converter);
    }
}
