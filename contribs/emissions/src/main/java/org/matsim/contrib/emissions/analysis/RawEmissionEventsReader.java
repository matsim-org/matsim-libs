/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Events reader for emissions wich doesn't bother with events manager and all.
 */
class RawEmissionEventsReader extends MatsimXmlParser {

    private static final String EVENT = "event";
    private static final String TYPE = "type";
    private static final String TIME = "time";
    private static final String LINK_ID = "linkId";
    private static final String VEHICLE_ID = "vehicleId";

    private static final Logger logger = LogManager.getLogger(RawEmissionEventsReader.class);
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.UK);

    // create this mapping for parsing the pollutants and create backwards compatibility to older emission events files
    private static final Map<String, Pollutant> mapping = createPollutantMapping();

    // There is no reset. If handler is used multiple times this will keep counting.
    private final AtomicInteger eventsCounter = new AtomicInteger();
    private final HandleEmissionEvent handler;

    RawEmissionEventsReader(HandleEmissionEvent handler) {
			super(ValidationType.NO_VALIDATION);
        this.handler = handler;
        // events don't have dtd. Therefore, validation is not possible
        this.setValidating(false);
    }

    private static Map<String, Pollutant> createPollutantMapping() {

        Map<String, Pollutant> result = new HashMap<>();
        for (Pollutant value : Pollutant.values()) {
            result.put(value.toString(), value);
        }
        // older emission events files contain NOx in all UPPERCASE
        result.put("NOX", Pollutant.NOx);
        return Collections.unmodifiableMap(result);
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {

        if (EVENT.equals(name)) {
            var type = atts.getValue(TYPE);
            if (WarmEmissionEvent.EVENT_TYPE.equals(type) || ColdEmissionEvent.EVENT_TYPE.equals(type)) {
                handleEmissionEvent(atts);
            }
        }
    }

    private void handleEmissionEvent(Attributes atts) {

        // parse metadata
        var time = Double.parseDouble(atts.getValue(TIME));
        var linkId = atts.getValue(LINK_ID);
        var vehicleId = atts.getValue(VEHICLE_ID);

        // parse pollution by pollutant
        for (var i = 0; i < atts.getLength(); i++) {
            var key = atts.getLocalName(i);
            if (mapping.containsKey(key)) {
                var pollutant = mapping.get(key);
                var value = Double.parseDouble(atts.getValue(i));
                handler.accept(time, linkId, vehicleId, pollutant, value);
            }
        }

        // give some feedback about progress
        var currentCount = eventsCounter.incrementAndGet();
        if (currentCount % 500000 == 0) {
            logger.info("Emission Event # {}", numberFormat.format(currentCount));
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
        // don't need to do anything here, since everything is handled in startTag
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        // ignore characters to prevent OutOfMemoryExceptions
        /* the events-file only contains empty tags with attributes,
         * but without the dtd or schema, all whitespace between tags is handled
         * by characters and added up by super.characters, consuming huge
         * amount of memory when large events-files are read in.
         */
    }

    @FunctionalInterface
    public interface HandleEmissionEvent {
        void accept(double time, String linkId, String vehicleId, Pollutant pollutant, double value);
    }
}
