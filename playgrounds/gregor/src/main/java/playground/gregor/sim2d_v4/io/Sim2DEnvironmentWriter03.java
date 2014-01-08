/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEnvironmentWriter.java
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

package playground.gregor.sim2d_v4.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;

import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.ObjectFactory;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLAbstractFeatureType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLAbstractGeometryType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLBoundingShapeType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLBoxType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLCoordinatesType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLFeatureAssociationType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLLinearRingMemberType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLLinearRingType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLPolygonType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLFeatureCollectionType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLNeighborsType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLOpeningsIdsType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLOpeningsType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLSectionPropertyType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.XMLSim2DEnvironmentSectionType;
import playground.gregor.sim2d_v4.scenario.Section;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class Sim2DEnvironmentWriter03 extends MatsimJaxbXmlWriter{

	private static final Logger log = Logger.getLogger(Sim2DEnvironmentWriter03.class);

	public static final String SCHEMA = "http://svn.vsp.tu-berlin.de/repos/public-svn/xml-schemas/sim2dEnvironment_v0.3.xsd";

	private final Sim2DEnvironment env;

	ObjectFactory gmlFac = new ObjectFactory();
	playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.ObjectFactory sim2dFac = new playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.ObjectFactory();

	public Sim2DEnvironmentWriter03(Sim2DEnvironment env) {
		this.env = env;
	}

	@Override
	public void write(String filename) {
		log.info("writing file:" + filename);
		try {
			JAXBElement<XMLFeatureCollectionType> jxbEnv = getJxbEnv();
			JAXBContext jc = JAXBContext.newInstance(playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.ObjectFactory.class);
			Marshaller m = jc.createMarshaller();
			super.setMarshallerProperties(SCHEMA, m);
			BufferedWriter buffout = IOUtils.getBufferedWriter(filename);
			m.marshal(jxbEnv, buffout);
			buffout.close();
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private JAXBElement<XMLFeatureCollectionType> getJxbEnv() {


		XMLFeatureCollectionType collection = this.sim2dFac.createXMLFeatureCollectionType();
		collection.setFid(this.env.getId().toString());
		
		XMLBoundingShapeType bounds = this.gmlFac.createXMLBoundingShapeType();
		XMLBoxType box = this.gmlFac.createXMLBoxType();
		XMLCoordinatesType koords = this.gmlFac.createXMLCoordinatesType();

		String cs = koords.getCs();
		String ts = koords.getTs();
		StringBuffer buf = new StringBuffer();
		Envelope envelope = this.env.getEnvelope();
		buf.append(envelope.getMinX());
		buf.append(cs);
		buf.append(envelope.getMinY());
		buf.append(ts);
		buf.append(envelope.getMaxX());
		buf.append(cs);
		buf.append(envelope.getMaxY());
		koords.setValue(buf.toString());
		box.setCoordinates(koords);
		bounds.setBox(box );
		box.setSrsName(this.env.getCRS().getIdentifiers().iterator().next().toString());

		collection.setBoundedBy(bounds);

		Map<Id,XMLFeatureAssociationType> mapping = new HashMap<Id, XMLFeatureAssociationType>();
		for (Section sec : this.env.getSections().values()){
			XMLFeatureAssociationType fat = getJXBSection(sec);
			mapping.put(sec.getId(),fat);
			collection.getFeatureMember().add(fat);
		}
		//add neighbors
		for (Section sec : this.env.getSections().values()) {
			if (sec.getNeighbors() != null) {
				XMLFeatureAssociationType fat = mapping.get(sec.getId());

				XMLNeighborsType xmlNeighbors = this.sim2dFac.createXMLNeighborsType();

				XMLSim2DEnvironmentSectionType v = (XMLSim2DEnvironmentSectionType) fat.getFeature().getValue();
				v.getGeometryProperty().setNeighbors(xmlNeighbors);
				List<Object> l = xmlNeighbors.getFidrefs();
				for (Id id : sec.getNeighbors()) {
					XMLFeatureAssociationType obj = mapping.get(id);
					l.add(obj.getFeature().getValue());
				}
			}
		}

		JAXBElement<XMLFeatureCollectionType> jcoll = this.sim2dFac.createSim2DEnvironment(collection);
		return jcoll;
	}

	private XMLFeatureAssociationType getJXBSection(Section sec) {
		Object srsString = this.env.getCRS().getIdentifiers().iterator().next();
		String srsName = srsString.toString();
		StringBuffer buf2 = new StringBuffer();
		Coordinate[] coords = sec.getPolygon().getExteriorRing().getCoordinates();
		XMLCoordinatesType koords = this.gmlFac.createXMLCoordinatesType();
		String cs = koords.getCs();
		String ts = koords.getTs();
		for (int i = 0; i < coords.length; i++) {
			Coordinate c = coords[i];
			buf2.append(c.x);
			buf2.append(cs);
			buf2.append(c.y);
			buf2.append(ts);
		}
		koords.setValue(buf2.toString());

		XMLFeatureAssociationType fat = this.gmlFac.createXMLFeatureAssociationType();
		XMLSectionPropertyType myPolygon = this.sim2dFac.createXMLSectionPropertyType();
		myPolygon.setMatsimType("sim2d_section"); //jaxb seems not to support fixed element values so we set this here 'manually' 

		XMLPolygonType geom = this.gmlFac.createXMLPolygonType();
		geom.setSrsName(srsName);
		XMLLinearRingMemberType ring = this.gmlFac.createXMLLinearRingMemberType();
		XMLLinearRingType lrt = this.gmlFac.createXMLLinearRingType();
		lrt.setSrsName(srsName);
		lrt.setCoordinates(koords);
		JAXBElement<? extends XMLAbstractGeometryType> ringm = this.gmlFac.createLinearRing(lrt );
		ring.setGeometry(ringm);
		geom.setOuterBoundaryIs(ring );
		JAXBElement<? extends XMLPolygonType> geomp = this.gmlFac.createPolygon(geom);

		myPolygon.setGeometry(geomp);
		myPolygon.setLevel(new BigInteger(sec.getLevel()+""));


		if (sec.getOpenings() != null) {
			XMLOpeningsType xmlOpenings = this.sim2dFac.createXMLOpeningsType();
			StringBuffer buf = new StringBuffer();
			int[] openings = sec.getOpenings();
			for (int i : openings) {
				buf.append(i+"");
				buf.append(xmlOpenings.getVs());
			}
			xmlOpenings.setValue(buf.toString());
			JAXBElement<XMLOpeningsType> jopenings = this.sim2dFac.createXMLSectionPropertyTypeOpenings(xmlOpenings);
			myPolygon.setOpenings(jopenings );
		}
		
		if (sec.getOpeningsIds() != null) {
			XMLOpeningsIdsType xmlOpeningsIds = this.sim2dFac.createXMLOpeningsIdsType();
			StringBuffer buf = new StringBuffer();
			Id [] ids = sec.getOpeningsIds();
			for (Id id : ids) {
				buf.append(id.toString());
				buf.append(xmlOpeningsIds.getVs());
			}
			xmlOpeningsIds.setValue(buf.toString());
			JAXBElement<XMLOpeningsIdsType> jopeningsIds = this.sim2dFac.createXMLSectionPropertyTypeOpeningsIds(xmlOpeningsIds);
			myPolygon.setOpeningsIds(jopeningsIds);
		}
		
//		if (sec.getRelatedLinkIds().size() > 0) {
//			XMLRelatedLinksRefIdsType xmlIds = this.sim2dFac.createXMLRelatedLinksRefIdsType();
//			StringBuffer buf = new StringBuffer();
//			for (Id id : sec.getRelatedLinkIds()) {
//				buf.append(id.toString());
//				buf.append(xmlIds.getVs());
//			}
//			xmlIds.setValue(buf.toString());
//			myPolygon.setRelatedLinksRefIds(xmlIds);
//		}

		if (sec.getNeighbors() != null) {
			XMLNeighborsType xmlNeighbors = this.sim2dFac.createXMLNeighborsType();

			//			XMLNeighborsType l = xmlNeighbors;
			List<Object> refs = xmlNeighbors.getFidrefs();
			for (Id id : sec.getNeighbors()) {
				refs.add(id.toString());
			}
			myPolygon.setNeighbors(xmlNeighbors);
		}


		XMLSim2DEnvironmentSectionType testType = this.sim2dFac.createXMLSim2DEnvironmentSectionType();
		testType.setFid(sec.getId().toString());
		testType.setGeometryProperty(myPolygon);
		JAXBElement<? extends XMLAbstractFeatureType> jxb = this.sim2dFac.createSim2DEnvironmentSection(testType);
		fat.setFeature(jxb);



		return fat;
	}


}
