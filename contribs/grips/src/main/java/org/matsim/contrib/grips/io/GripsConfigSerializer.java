/* *********************************************************************** *
 * project: org.matsim.*
 * GripsConfigSerializer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.grips.io;

import java.io.BufferedWriter;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.apache.log4j.Logger;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.FileType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.GripsConfigType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.MainTrafficTypeType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.ObjectFactory;
import org.matsim.contrib.grips.model.config.GripsConfigModule;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

public class GripsConfigSerializer extends MatsimJaxbXmlWriter {
	
private static final Logger log = Logger.getLogger(GripsConfigSerializer.class);
	
	public static final String SCHEMA = "http://matsim.org/files/dtd/grips_config_v0.1.xsd";
//	public static final String SCHEMA = "http://svn.vsp.tu-berlin.de/repos/public-svn/xml-schemas/grips_config_v0.1.xsd";
	private final GripsConfigModule gcm;

	public GripsConfigSerializer(GripsConfigModule gcm) {
		this.gcm = gcm;
	}
	
	
	@Override
	public void write(String filename) {
		JAXBElement<GripsConfigType> jgct = getGripsConfigType();
		log.info("writing file:" + filename);
		try {
			JAXBContext jc = JAXBContext.newInstance(org.matsim.contrib.grips.io.jaxb.gripsconfig.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(SCHEMA, m);
			BufferedWriter buffout = IOUtils.getBufferedWriter(filename);
			m.marshal(jgct, buffout);
			buffout.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private JAXBElement<GripsConfigType> getGripsConfigType() {
		ObjectFactory fac = new ObjectFactory();
		
		GripsConfigType gct = fac.createGripsConfigType();
		
		FileType net = fac.createFileType();
		net.setInputFile(this.gcm.getNetworkFileName());
		FileType evacArea = fac.createFileType();
		evacArea.setInputFile(this.gcm.getEvacuationAreaFileName());
		FileType pop = fac.createFileType();
		pop.setInputFile(this.gcm.getPopulationFileName());
		FileType outDir = fac.createFileType();
		outDir.setInputFile(this.gcm.getOutputDir());
		DepartureTimeDistributionType depTimeDistr = this.gcm.getDepartureTimeDistribution();
		
		MainTrafficTypeType mtt = MainTrafficTypeType.fromValue(this.gcm.getMainTrafficType());
		
		gct.setMainTrafficType(mtt);
		gct.setEvacuationAreaFile(evacArea);
		gct.setNetworkFile(net);
		gct.setOutputDir(outDir);
		gct.setPopulationFile(pop);
		gct.setDepartureTimeDistribution(depTimeDistr);
		
		gct.setSampleSize(this.gcm.getSampleSize());
		
		
		
		return fac.createGripsConfig(gct);
	}


	public void serialize(String fileName) {
		
		JAXBElement<GripsConfigType> gcmType = createGCMType();
		
		JAXBContext jc;
		
		try {
			jc = JAXBContext.newInstance(GripsConfigType.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		
		Marshaller m;
		try {
			m = jc.createMarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		try {
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,Boolean.TRUE);
		} catch (PropertyException e) {
			throw new RuntimeException(e);
		}
		try {
			m.setProperty("jaxb.schemaLocation", "http://matsim.org/xsd" + " " + SCHEMA);
		} catch (PropertyException e) {
			throw new RuntimeException(e);
		}
		BufferedWriter bufout = IOUtils.getBufferedWriter(fileName);
		try {
			m.marshal(gcmType, bufout);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		try {
			bufout.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private JAXBElement<GripsConfigType> createGCMType() {
		ObjectFactory fac = new ObjectFactory();
		
		GripsConfigType gct = fac.createGripsConfigType();
		
		FileType net = fac.createFileType();
		net.setInputFile(this.gcm.getNetworkFileName());
		FileType evacArea = fac.createFileType();
		evacArea.setInputFile(this.gcm.getEvacuationAreaFileName());
		FileType pop = fac.createFileType();
		pop.setInputFile(this.gcm.getPopulationFileName());
		FileType outDir = fac.createFileType();
		outDir.setInputFile(this.gcm.getOutputDir());

		gct.setEvacuationAreaFile(evacArea);
		gct.setNetworkFile(net);
		gct.setOutputDir(outDir);
		gct.setPopulationFile(pop);
		gct.setDepartureTimeDistribution(this.gcm.getDepartureTimeDistribution());
		gct.setSampleSize(this.gcm.getSampleSize());
		gct.setMainTrafficType(MainTrafficTypeType.fromValue(this.gcm.getMainTrafficType()));
		
		
		
		return fac.createGripsConfig(gct);
	}



}
