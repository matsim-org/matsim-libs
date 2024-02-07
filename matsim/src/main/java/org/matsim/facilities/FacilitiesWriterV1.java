/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author mrieser
 */
class FacilitiesWriterV1 extends MatsimXmlWriter implements MatsimWriter {

    private static final String DTD = "http://www.matsim.org/files/dtd/facilities_v1.dtd";

    private final ActivityFacilities facilities;

    private final CoordinateTransformation coordinateTransformation;

    private AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

    FacilitiesWriterV1(
        final CoordinateTransformation coordinateTransformation,
        final ActivityFacilities facilities) {
        this.coordinateTransformation = coordinateTransformation;
        this.facilities = facilities;
    }

    @Override
    public void write(String filename) {
        openFile(filename);
        this.writeInit();
        for (ActivityFacility f : FacilitiesUtils.getSortedFacilities(this.facilities).values()) {
            this.writeFacility((ActivityFacilityImpl) f);
        }
        this.writeFinish();
    }

    public void write(OutputStream stream) {
        openOutputStream(stream);
        this.writeInit();
        for (ActivityFacility f : FacilitiesUtils.getSortedFacilities(this.facilities).values()) {
            this.writeFacility((ActivityFacilityImpl) f);
        }
        this.writeFinish();
    }

    private void writeInit() {
        this.writeXmlHead();
        this.writeDoctype("facilities", DTD);
        this.startFacilities(this.facilities, this.writer);
    }

    private void writeFacility(final ActivityFacilityImpl f) {
        try {
            this.startFacility(f);
            for (ActivityOption a : f.getActivityOptions().values()) {
                this.startActivity((ActivityOptionImpl) a);
                this.writeCapacity((ActivityOptionImpl) a, this.writer);
                SortedSet<OpeningTime> o_set = a.getOpeningTimes();
                for (OpeningTime o : o_set) {
                    this.writeOpentime(o, this.writer);
                }
                this.endActivity();
            }
            if (!f.getAttributes().isEmpty()) {
                this.writer.write(NL);
            }
            this.attributesWriter.writeAttributes("\t\t", this.writer, f.getAttributes(), false);
            this.endFacility();
            this.writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeFinish() {
        try {
            this.endFacilities();
            this.writer.flush();
            this.writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //////////////////////////////////////////////////////////////////////
    // <facilities ... > ... </facilities>
    //////////////////////////////////////////////////////////////////////

    private void startFacilities(final ActivityFacilities facilities, final BufferedWriter out) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        if (facilities.getName() != null) {
            attributes.add(new Tuple<>("name", facilities.getName()));
        }
        writeStartTag("facilities", attributes);
        if (!facilities.getAttributes().isEmpty()) {
            try {
                this.writer.write(NL);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        this.attributesWriter.writeAttributes("\t", out, facilities.getAttributes());
    }


    private void endFacilities() {
        writeEndTag("facilities");
    }

    //////////////////////////////////////////////////////////////////////
    // <facility ... > ... </facility>
    //////////////////////////////////////////////////////////////////////

    private void startFacility(final ActivityFacilityImpl facility) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(new Tuple<>("id", facility.getId().toString()));
        if (facility.getLinkId() != null) {
            attributes.add(new Tuple<>("linkId", facility.getLinkId().toString()));
        }
        if (facility.getCoord()!=null) {
            final Coord coord = this.coordinateTransformation.transform(facility.getCoord());
            attributes.add(new Tuple<>("x", Double.toString(coord.getX())));
            attributes.add(new Tuple<>("y", Double.toString(coord.getY())));
        }
        if (facility.getDesc() != null) {
            attributes.add(new Tuple<>("desc", facility.getDesc()));
        }
        writeStartTag("facility", attributes, false);
    }

    private void endFacility() {
        writeEndTag("facility");
    }

    //////////////////////////////////////////////////////////////////////
    // <activity ... > ... </activity>
    //////////////////////////////////////////////////////////////////////

    public void startActivity(final ActivityOptionImpl activity) {
        List<Tuple<String, String>> attributes = new ArrayList<>();
        attributes.add(new Tuple<>("type", activity.getType()));
        writeStartTag("activity", attributes);
    }

    public void endActivity() {
        writeEndTag("activity");
    }

    //////////////////////////////////////////////////////////////////////
    // <capacity ... />
    //////////////////////////////////////////////////////////////////////

    private void writeCapacity(final ActivityOptionImpl activity, final BufferedWriter out) throws IOException {
        if (activity.getCapacity() != Integer.MAX_VALUE) {
            out.write("\t\t\t<capacity");
            out.write(" value=\"" + activity.getCapacity() + "\"");
            out.write(" />\n");
        }
    }

    //////////////////////////////////////////////////////////////////////
    // <opentime ... />
    //////////////////////////////////////////////////////////////////////

    private void writeOpentime(final OpeningTime opentime, final BufferedWriter out) throws IOException {
        out.write("\t\t\t<opentime");
        out.write(" day=\"wkday\"");
        out.write(" start_time=\"" + Time.writeTime(opentime.getStartTime()) + "\"");
        out.write(" end_time=\"" + Time.writeTime(opentime.getEndTime()) + "\"");
        out.write(" />\n");
    }

    public void putAttributeConverters(Map<Class<?>, AttributeConverter<?>> converters) {
        this.attributesWriter.putAttributeConverters(converters);
    }
}
