/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesReaderMatsimV2.java
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

import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlReaderDelegate;
import org.xml.sax.Attributes;

/**
 * A reader for facilities-files of MATSim according to <code>facilities_v2.dtd</code>.
 *
 * @author mrieser
 * @author balmermi
 */
final class FacilitiesReaderMatsimV2 extends MatsimXmlParser {
    private static final  Logger log = LogManager.getLogger(FacilitiesReaderMatsimV2.class);

    private final static String FACILITIES = "facilities";
    private final static String FACILITY = "facility";
    private final static String ACTIVITY = "activity";
    private final static String CAPACITY = "capacity";
    private final static String OPENTIME = "opentime";
    private static final String ATTRIBUTES = "attributes";
    private static final String ATTRIBUTE = "attribute";

    private final ActivityFacilities facilities;
    private final ActivityFacilitiesFactory factory;
    private final AttributesXmlReaderDelegate attributesReader = new AttributesXmlReaderDelegate();
    private ActivityFacility currfacility = null;
    private ActivityOption curractivity = null;
    private org.matsim.utils.objectattributes.attributable.Attributes currAttributes = null;

    private final String externalInputCRS;
    private final String targetCRS;
    private CoordinateTransformation coordinateTransformation = new IdentityTransformation();

    FacilitiesReaderMatsimV2(
            final String externalInputCRS,
            final String targetCRS,
            final ActivityFacilities facilities) {
        super(ValidationType.DTD_ONLY);
        this.externalInputCRS = externalInputCRS;
        this.targetCRS = targetCRS;
        this.facilities = facilities;
        this.factory = this.facilities.getFactory();
        if (externalInputCRS != null && targetCRS != null) {
            this.coordinateTransformation = TransformationFactory.getCoordinateTransformation(externalInputCRS, targetCRS);
            ProjectionUtils.putCRS(this.facilities, targetCRS);
        }
    }

    public void putAttributeConverter(Class<?> clazz, AttributeConverter<?> converter) {
        this.attributesReader.putAttributeConverter(clazz, converter);
    }

    public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
        this.attributesReader.putAttributeConverters(converters);
    }

    @Override
    public void startTag(final String name, final org.xml.sax.Attributes atts, final Stack<String> context) {
        if (FACILITIES.equals(name)) {
            startFacilities(atts);
        } else if (FACILITY.equals(name)) {
            startFacility(atts);
        } else if (ACTIVITY.equals(name)) {
            startActivity(atts);
        } else if (CAPACITY.equals(name)) {
            startCapacity(atts);
        } else if (OPENTIME.equals(name)) {
            startOpentime(atts);
        } else if (ATTRIBUTE.equals(name)) {
            this.attributesReader.startTag(name, atts, context, this.currAttributes);
        } else if (ATTRIBUTES.equals(name)) {
            currAttributes = context.peek().equals(FACILITIES) ? this.facilities.getAttributes() : this.currfacility.getAttributes();
            attributesReader.startTag(name, atts, context, currAttributes);
        }
    }

    @Override
    public void endTag(final String name, final String content, final Stack<String> context) {
        if (FACILITY.equals(name)) {
            this.facilities.addActivityFacility(this.currfacility);
            this.currfacility = null;
        } else if (ACTIVITY.equals(name)) {
            this.curractivity = null;
        } else if (ATTRIBUTES.equalsIgnoreCase(name)) {
            if (context.peek().equals(FACILITIES)) {
                String inputCRS = (String) currAttributes.getAttribute(ProjectionUtils.INPUT_CRS_ATT);

                if (inputCRS != null && targetCRS != null) {
                    if (externalInputCRS != null) {
                        // warn or crash?
                        log.warn("coordinate transformation defined both in config and in input file: setting from input file will be used");
                    }
                    coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, targetCRS);
                    currAttributes.putAttribute(ProjectionUtils.INPUT_CRS_ATT, targetCRS);
                }
            }
            this.currAttributes = null;
        } else if (ATTRIBUTE.equalsIgnoreCase(name)) {
            this.attributesReader.endTag(name, content, context);
        }
    }

    private void startFacilities(final Attributes atts) {
        this.facilities.setName(atts.getValue("name"));
        this.currAttributes = facilities.getAttributes();
        if (atts.getValue("aggregation_layer") != null) {
            LogManager.getLogger(FacilitiesReaderMatsimV2.class).warn("aggregation_layer is deprecated.");
        }
    }

    private void startFacility(final Attributes atts) {
        if ( atts.getValue("x") !=null && atts.getValue("y") !=null ) {
            if (atts.getValue("linkId") !=null) { //both coord and link present
                this.currfacility =
                        this.factory.createActivityFacility(
                                Id.create(atts.getValue("id"), ActivityFacility.class),
                                coordinateTransformation.transform(coordFromAtts(atts)),
                                Id.create(atts.getValue("linkId"),Link.class));
            } else { // only coord present
                this.currfacility =
                        this.factory.createActivityFacility(
                                Id.create(atts.getValue("id"), ActivityFacility.class),
                                coordinateTransformation.transform(coordFromAtts(atts)));
            }
        } else {
            if (atts.getValue("linkId") !=null) { //only link present
            this.currfacility =
                    this.factory.createActivityFacility(
                            Id.create(atts.getValue("id"), ActivityFacility.class),
                            Id.create(atts.getValue("linkId"),Link.class));
            } else { //neither coord nor link present
                throw new RuntimeException("Neither coordinate nor linkId are available for facility id "+ atts.getValue("id")+". Aborting....");
            }
        }

        ((ActivityFacilityImpl) this.currfacility).setDesc(atts.getValue("desc"));
    }

    private static Coord coordFromAtts(final Attributes atts) {
        if (atts.getValue("z") != null) {
            return new Coord(
                Double.parseDouble(atts.getValue("x")),
                Double.parseDouble(atts.getValue("y")),
                Double.parseDouble(atts.getValue("z")));
        } else {
            return new Coord(
                Double.parseDouble(atts.getValue("x")),
                Double.parseDouble(atts.getValue("y")));
        }
    }

    private void startActivity(final Attributes atts) {
        this.curractivity = this.factory.createActivityOption(atts.getValue("type"));
        this.currfacility.addActivityOption(this.curractivity);
    }

    private void startCapacity(final Attributes atts) {
        double cap = Double.parseDouble(atts.getValue("value"));
        this.curractivity.setCapacity(cap);
    }

    private void startOpentime(final Attributes atts) {
        this.curractivity.addOpeningTime(OpeningTimeImpl.createFromOptionalTimes(
                Time.parseOptionalTime(atts.getValue("start_time")),
                Time.parseOptionalTime(atts.getValue("end_time"))));
    }


}
