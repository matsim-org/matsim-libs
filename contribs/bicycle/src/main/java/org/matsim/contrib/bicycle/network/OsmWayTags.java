/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OSM way tags, read straight from an {@code .osm} file and keyed by way id.
 *
 * <p>This is the tag source for bicycle post-processing on a SUMO-converted network.
 * A MATSim link there carries the SUMO edge id, and the edge knows the OSM way id(s)
 * it came from ({@code origId}) — so the classifier can be fed the original way tags
 * instead of what survived the conversion.
 *
 * <p><b>Why not read the tags netconvert exported?</b> netconvert merges edges
 * without looking at OSM tags, and on a merged edge a tag present on only one
 * constituent way wins silently over the full length. Going back to the ways is the
 * only way to see that; the package README ("Why the tags come from the OSM file")
 * has the worked example.
 *
 * <p>Only the keys handed to {@link #read(Path, Set)} are kept, so memory stays at
 * O(ways × whitelist) rather than O(all tags). Ways without a single matching tag are
 * not stored at all; {@link #get(long)} returns an empty map for them.
 *
 * <p>Reads uncompressed and compressed XML ({@code .osm}, {@code .osm.gz}, ...) via
 * {@link IOUtils}. There is no PBF support — the SUMO pipeline feeds netconvert an
 * {@code .osm} file anyway, and using the very same file guarantees the way ids match.
 */
public final class OsmWayTags {

	private static final Logger log = LogManager.getLogger(OsmWayTags.class);

	private final Map<Long, Map<String, String>> tagsByWay;

	private OsmWayTags(Map<Long, Map<String, String>> tagsByWay) {
		this.tagsByWay = tagsByWay;
	}

	/**
	 * Reads the tag keys of {@link BicycleOsmTags#classificationKeys()}.
	 */
	public static OsmWayTags read(Path osmFile) {
		return read(osmFile, BicycleOsmTags.classificationKeys());
	}

	/**
	 * @param osmFile an OSM XML file, optionally compressed
	 * @param keys    the tag keys to keep; everything else is discarded while parsing
	 */
	public static OsmWayTags read(Path osmFile, Set<String> keys) {

		Map<Long, Map<String, String>> result = new HashMap<>();
		// OSM tag values repeat heavily ("yes", "asphalt", ...); one shared instance each
		// keeps this from being the dominant memory cost on a country-sized extract.
		Map<String, String> valuePool = new HashMap<>();

		long ways = 0;
		long unparseableIds = 0;

		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

		try (InputStream in = IOUtils.getInputStream(IOUtils.resolveFileOrResource(osmFile.toString()))) {

			XMLStreamReader reader = factory.createXMLStreamReader(in);

			// Non-null exactly while we are inside a <way>, which is what keeps the <tag>
			// children of nodes and relations out.
			Map<String, String> current = null;
			long wayId = 0;

			while (reader.hasNext()) {
				int event = reader.next();

				if (event == XMLStreamConstants.START_ELEMENT) {
					switch (reader.getLocalName()) {
						case "way" -> {
							String id = reader.getAttributeValue(null, "id");
							try {
								wayId = Long.parseLong(id);
								current = new HashMap<>();
							} catch (NumberFormatException | NullPointerException e) {
								unparseableIds++;
								current = null;
							}
						}
						case "tag" -> {
							if (current != null) {
								String key = reader.getAttributeValue(null, "k");
								String value = reader.getAttributeValue(null, "v");
								if (value != null && keys.contains(key)) {
									current.put(key, valuePool.computeIfAbsent(value, v -> v));
								}
							}
						}
						case "node", "relation" -> current = null;
						default -> {
							// nd, member, bounds, ... carry nothing we want
						}
					}
				} else if (event == XMLStreamConstants.END_ELEMENT && "way".equals(reader.getLocalName())) {
					if (current != null && !current.isEmpty()) {
						// copyOf gives a compact map; the mutable one was only a builder
						result.put(wayId, Map.copyOf(current));
						ways++;
					}
					current = null;
				}
			}

			reader.close();

		} catch (IOException | XMLStreamException e) {
			throw new UncheckedIOException(new IOException("Could not read OSM ways from " + osmFile, e));
		}

		if (unparseableIds > 0) {
			log.warn("Skipped {} way(s) with a missing or unparseable id.", unparseableIds);
		}
		log.info("Read {} way(s) carrying at least one of {} whitelisted tag keys from {}.", ways, keys.size(), osmFile);

		return new OsmWayTags(result);
	}

	/**
	 * The whitelisted tags of that way, or an empty map when the way is unknown or
	 * carried none of them. Never null.
	 */
	public Map<String, String> get(long wayId) {
		return tagsByWay.getOrDefault(wayId, Map.of());
	}

	/** Whether that way contributed at least one whitelisted tag. */
	public boolean contains(long wayId) {
		return tagsByWay.containsKey(wayId);
	}

	/** Number of ways held. */
	public int size() {
		return tagsByWay.size();
	}
}
