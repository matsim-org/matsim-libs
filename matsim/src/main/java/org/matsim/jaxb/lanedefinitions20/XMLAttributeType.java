/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.jaxb.lanedefinitions20;

import jakarta.xml.bind.annotation.*;

/**
 * This type can be used for all attributes of classes in MATSim that implement the Attributable interface.
 * 
 * <p>
 * Java class for attribute type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 *  <xs:complexType name="attributeType">
 * 	<xs:simpleContent>
 * 		<xs:extension base="xs:string">
 * 			<xs:attribute name="name" type="xs:string"/>
 * 			<xs:attribute name="class" type="xs:string"/>
 * 		</xs:extension>
 * 	</xs:simpleContent>
 * </xs:complexType>
 * </pre>
 * 
 * @author tthunig
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "attributeType", propOrder = {
	    "name",
	    "clazz",
	})
public class XMLAttributeType {

	@XmlAttribute(required = true)
    protected String name;
	@XmlAttribute(name = "class", required = true)
    protected String clazz;
	@XmlValue()
    protected String value;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getClazz() {
		return clazz;
	}

	public void setClazz(String clazz) {
		this.clazz = clazz;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
