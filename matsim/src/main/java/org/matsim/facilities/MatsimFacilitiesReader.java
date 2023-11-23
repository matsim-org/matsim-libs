/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFacilitiesReader.java
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

package org.matsim.facilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.xml.sax.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A reader for facilities-files of MATSim. This reader recognizes the format of the facilities-file and uses
 * the correct reader for the specific facilities-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimFacilitiesReader extends MatsimXmlParser {
    /* Why is this suddenly a "Matsim"FacilitiesReader and not just a Facilities reader to be consistent with all other
	 * naming conventions?  kai, jan09
	 * because all other readers in Matsim are also called Matsim*Reader,
	 * e.g. MatsimPopulationReader, MatsimNetworkReader, MatsimWorldReader, ...
	 * marcel, feb09
	 * The logic seems to be:
	 * - there is a "basic" MatsimXmlParser
	 * - there are implementations AbcReaderMatsimVx
	 * - there is a meta-class MatsimReaderAbc, which calls the Vx-Readers depending on the version
	 * - yy there is usually also an interface AbcReader, which is, however, not consistent:
	 *   () sometimes, it is there, and sometimes not
	 *   () sometimes, it is read(), sometimes it is readFile( file), sometimes ...
	 *   () sometimes it throws an i/o exception, sometimes not
	 * Oh well.
	 * At least it seems indeed that the MatsimReader is indeed usually there. kai, jul09
	 */


    private final static String FACILITIES_V1 = "facilities_v1.dtd";
    private final static String FACILITIES_V2 = "facilities_v2.dtd";

    private final static Logger log = LogManager.getLogger(MatsimFacilitiesReader.class);

    private final String externalInputCRS;
    private final String targetCRS;
    private CoordinateTransformation coordinateTransformation;

    private final ActivityFacilities facilities;
    private MatsimXmlParser delegate = null;
    private Map<Class<?>, AttributeConverter<?>> attributeConverters = new HashMap<>();

    /**
     * Creates a new reader for MATSim facilities files.
     * Coordinates are not converted
     *
     * @param scenario The scenario containing the Facilities-object to store the facilities in.
     */
    public MatsimFacilitiesReader(final Scenario scenario) {
        this(null, scenario);
    }

    /**
     * Creates a new reader for MATSim facilities files.
     * Converts the coordinates to the target CRS, given the CRS information in the container attributes, or, if absent,
     * from the config.
     *
     * @param targetCRS the CRS the coordinates should be expressed in
     * @param scenario                 The scenario containing the Facilities-object to store the facilities in.
     */
    public MatsimFacilitiesReader(
            final String targetCRS,
            final Scenario scenario) {
        this(scenario.getConfig().facilities().getInputCRS(), targetCRS, scenario.getActivityFacilities());
    }

    /**
     * Creates a new reader for MATSim facilities files.
     *
     * @param externalInputCRS specifies the CRS the coordinates are expressed in. If the CRS is define in the container
     *                         attributes, this value is ignored
     * @param targetCRS the CRS the coordinates should be expressed in
     * @param facilities                 The ActivityFacilities-object to store the facilities in.
     */
    public MatsimFacilitiesReader(
            final String externalInputCRS,
            final String targetCRS,
            final ActivityFacilities facilities) {
        super(ValidationType.DTD_ONLY);
        this.externalInputCRS = externalInputCRS;
        this.targetCRS = targetCRS;
        this.facilities = facilities;
    }

    public void putAttributeConverter(Class<?> clazz, AttributeConverter<?> converter) {
        this.attributeConverters.put(clazz, converter);
    }

    public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
        this.attributeConverters.putAll(converters);
    }

    @Override
    public void startTag(final String name, final Attributes atts, final Stack<String> context) {
        this.delegate.startTag(name, atts, context);
    }

    @Override
    public void endTag(final String name, final String content, final Stack<String> context) {
        this.delegate.endTag(name, content, context);
    }

    @Override
    protected void setDoctype(final String doctype) {
        super.setDoctype(doctype);
        if (FACILITIES_V1.equals(doctype)) {
            this.delegate = new FacilitiesReaderMatsimV1(this.externalInputCRS, this.targetCRS, this.facilities);
            ((FacilitiesReaderMatsimV1)this.delegate).putAttributeConverters(this.attributeConverters);
            log.info("using facilities_v1-reader.");
        } else if (FACILITIES_V2.equals(doctype)) { // v2 added support for 3D coordinates
            this.delegate = new FacilitiesReaderMatsimV2(this.externalInputCRS, this.targetCRS, this.facilities);
            ((FacilitiesReaderMatsimV2)this.delegate).putAttributeConverters(this.attributeConverters);
            log.info("using facilities_v2-reader.");
        } else {
            throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
        }
    }

}
