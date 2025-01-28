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

package org.matsim.freight.carriers;

import com.google.inject.Inject;
import java.util.Map;
import org.matsim.utils.objectattributes.AttributeConverter;

/**
 * @author mrieser / Simunto
 */
public class CarrierPlanWriter {

	private final Carriers carriers;
	private Map<Class<?>, AttributeConverter<?>> attributeConverters = null;

	public CarrierPlanWriter(Carriers carriers) {
		this.carriers = carriers;
	}

	@Inject
	public void setAttributeConverters(Map<Class<?>, AttributeConverter<?>> attributeConverters) {
		this.attributeConverters = attributeConverters;
	}

	public void write(String filename) {
		this.writeV2_1(filename);
	}

	/**
	 * Writes out the Carriers file in version 2.1.
	 * Please use the method {@link #write(String)} to always ensure writing out to the newest format.
	 * --> keeping it private
	 *
	 * @param filename Name of the file that should be written.
	 */
	private void writeV2_1(String filename) {
		CarrierPlanXmlWriterV2_1 writer = new CarrierPlanXmlWriterV2_1(this.carriers);
		if (this.attributeConverters != null) {
			writer.putAttributeConverters(this.attributeConverters);
		}
		writer.write(filename);
	}
}
