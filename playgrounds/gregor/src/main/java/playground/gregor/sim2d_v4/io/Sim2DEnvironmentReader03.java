/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEnvironmentReader02.java
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

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimJaxbXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.xml.sax.SAXException;

import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLAbstractFeatureType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLBoundingShapeType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLCoordType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLCoordinatesType;
import playground.gregor.sim2d_v4.io.jaxb.gmlfeature.XMLFeatureAssociationType;
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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class Sim2DEnvironmentReader03  extends MatsimJaxbXmlParser{

	private static final Logger log = Logger.getLogger(Sim2DEnvironmentReader03.class);

	public static final String SCHEMA = "http://svn.vsp.tu-berlin.de/repos/public-svn/xml-schemas/sim2dEnvironment_v0.2.xsd";
//	public static final String SCHEMA = "http://matsim.org/files/dtd/sim2dEnvironment_v0.2.xsd";

	private final Sim2DEnvironment env;


	private CoordinateReferenceSystem targetSRS = null;


	private boolean isValidating = false;

	private final GeometryFactory geofac = new GeometryFactory();
	
	public Sim2DEnvironmentReader03(Sim2DEnvironment env, String schemaLocation, boolean isValidating) {
		super(schemaLocation);
		this.env = env;
		this.isValidating = isValidating;
	}

	public Sim2DEnvironmentReader03(Sim2DEnvironment env, boolean isValidating) {
		this(env,SCHEMA, isValidating);
	}



	@Override
	public void readFile(String filename) throws UncheckedIOException {
		JAXBContext jc;
		JAXBElement<XMLFeatureCollectionType> xmlcoll = null;
		InputStream stream = null;

		try {
			jc = JAXBContext.newInstance(playground.gregor.sim2d_v4.io.jaxb.sim2denvironment03.ObjectFactory.class);
			Unmarshaller u = jc.createUnmarshaller();

			if (this.isValidating){
				validate(filename,u);
			}
			log.info("starting unmarshalling " + filename);
			stream = IOUtils.getInputStream(filename);
			xmlcoll = (JAXBElement<XMLFeatureCollectionType>) u.unmarshal(stream);

		} catch (JAXBException e) {
			throw new UncheckedIOException(e);
		}finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				log.warn("Could not close stream.", e);
			}
		}

		//convert from Jaxb types to sim2d conform types

		this.env.setId(Id.create(xmlcoll.getValue().getFid(), Sim2DEnvironment.class));
		
		//envelope
		XMLBoundingShapeType bounds = xmlcoll.getValue().getBoundedBy();
		String boxSRS = bounds.getBox().getSrsName();
		MathTransform boxTransform = getTransform(boxSRS);


		XMLCoordinatesType boxCoordinates = bounds.getBox().getCoordinates();
		List<XMLCoordType> boxCoord = bounds.getBox().getCoord();
		List<Coordinate> jtsCoords = null;
		if (boxCoord.size() > 0) {
			jtsCoords = getJtsCoords(boxCoord, boxTransform);
		} else {
			jtsCoords = getJtsCoords(boxCoordinates, boxTransform);
		}
		Envelope e = getEnvelope(jtsCoords);
		this.env.setEnvelope(e);

		if (this.targetSRS != null) {
			this.env.setCRS(this.targetSRS);
		} else {
			try {
				CoordinateReferenceSystem crs = CRS.decode(boxSRS, true);
//				System.out.println(crs.getName());
				this.env.setCRS(crs);
			} catch (NoSuchAuthorityCodeException e1) {
				throw new IllegalArgumentException(e1);
			} catch (FactoryException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
		
		//sections
		for (XMLFeatureAssociationType m : xmlcoll.getValue().getFeatureMember()) {
			JAXBElement<? extends XMLAbstractFeatureType> f = m.getFeature();
			XMLSectionPropertyType ff = ((XMLSim2DEnvironmentSectionType) f.getValue()).getGeometryProperty();
			Id<Section> id = Id.create(f.getValue().getFid(), Section.class);
			Polygon p = getPolygon(ff);
			int [] openings = getOpenings(ff);
			Id<Section>[] neighbors = getNeighbors(ff);
			BigInteger LEVEL = ff.getLevel();
			Id<Node>[] openingsMATSimIds = getOpeningsMATSimIds(ff);
			Section s = this.env.createAndAddSection(id,p,openings,neighbors, LEVEL.intValue(),openingsMATSimIds);
			
		}

	}




	private Id<Section>[] getNeighbors(XMLSectionPropertyType ff) {
		XMLNeighborsType xmlNeighbors = ff.getNeighbors();
		if (xmlNeighbors == null) {
			return new Id[0];
		}
		Id<Section> [] ret = new Id[xmlNeighbors.getFidrefs().size()];
		int idx = 0;
		for (Object  o : xmlNeighbors.getFidrefs()) {
			Id<Section> id = Id.create(((XMLSim2DEnvironmentSectionType)o).getFid(), Section.class);
			ret[idx++] = id;
		}
		return ret;
	}
	
	private Id<Node>[] getOpeningsMATSimIds(XMLSectionPropertyType ff) {
		JAXBElement<XMLOpeningsIdsType> oooo = ff.getOpeningsIds();
		if (oooo == null) {
			return null;
		}
		XMLOpeningsIdsType o = oooo.getValue();
		String vs = o.getVs();
		String val = o.getValue();
		if (vs.length() > 1) {
			throw new RuntimeException("Can not tokenize String:" + val);
		}
		String[] toks = StringUtils.explode(val, vs.charAt(0));
		Id<Node> [] ret = new Id [toks.length];
		int i = 0;
		for (String tok : toks) {
			ret[i++] = Id.create(tok, Node.class);
		}
		return ret;
	}
	
	private int [] getOpenings(XMLSectionPropertyType ff) {
		JAXBElement<XMLOpeningsType> oooo = ff.getOpenings();
		if (oooo == null || oooo.getValue().getValue().length() == 0) {
			return new int[0];
		}
		XMLOpeningsType xmlOpening = oooo.getValue();
		String vs = xmlOpening.getVs();
		String val = xmlOpening.getValue();
		
		if (vs.length() > 1) {
			throw new RuntimeException("Can not tokenize String:" + val);
		}
		
		String[] toks = StringUtils.explode(val, vs.charAt(0));
		int [] ret = new int[toks.length];
		for (int i = 0; i < toks.length; i++) {
			ret[i] = Integer.parseInt(toks[i]);
		}
		
		return ret;
	}

	private Polygon getPolygon(XMLSectionPropertyType ff) {
		
		XMLPolygonType xmlPolygon = (XMLPolygonType) ff.getGeometry().getValue();
		String polygonSRS = xmlPolygon.getSrsName();
		MathTransform polygonTransform = getTransform(polygonSRS);
		XMLLinearRingType xmlLr = ((XMLLinearRingType)xmlPolygon.getOuterBoundaryIs().getGeometry().getValue());
		XMLCoordinatesType polygonCoordinates = xmlLr.getCoordinates();
		List<XMLCoordType> polygonCoord = xmlLr.getCoord();
		List<Coordinate> jtsPCoords = null;
		if (polygonCoord.size() > 0) {
			jtsPCoords = getJtsCoords(polygonCoord, polygonTransform);
		} else {
			jtsPCoords = getJtsCoords(polygonCoordinates, polygonTransform);
		}
		Coordinate [] coords = jtsPCoords.toArray(new Coordinate[0]);
		LinearRing lr = this.geofac.createLinearRing(coords);
		Polygon p = this.geofac.createPolygon(lr, null);
		return p;
	}

	private void validate(String filename, Unmarshaller u) {
		//validate xml file
		log.info("starting to validate " + filename);
		try {
			super.validateFile(filename, u);
		} catch (SAXException e) {
			throw new UncheckedIOException(e);
		} catch (ParserConfigurationException e) {
			throw new UncheckedIOException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	private MathTransform getTransform(String srs) {
		if (srs != null && this.targetSRS != null) {
			CoordinateReferenceSystem crs;
			try {
				crs = CRS.decode(srs, true);
				return CRS.findMathTransform(crs, this.targetSRS);
			} catch (NoSuchAuthorityCodeException e1) {
				throw new IllegalArgumentException(e1);
			} catch (FactoryException e1) {
				throw new IllegalArgumentException(e1);
			}
		}
		return null;
	}

	private Envelope getEnvelope(List<Coordinate> jtsCoords) {
		Iterator<Coordinate> it = jtsCoords.iterator();
		Coordinate c = it.next();
		Envelope e = new Envelope(c);
		while (it.hasNext()) {
			c = it.next();
			e.expandToInclude(c);
		}
		return e;
	}

	private List<Coordinate> getJtsCoords(XMLCoordinatesType boxCoordinates, MathTransform transform) {

		List<Coordinate> ret = new ArrayList<Coordinate>();

		String val = boxCoordinates.getValue();
		String cs = boxCoordinates.getCs();
		String ts = boxCoordinates.getTs();

		if (ts.length() > 1 || cs.length() > 1) {
			throw new RuntimeException("Can not tokenize String:" + val);
		}
		String[] tuples = StringUtils.explode(val, ts.charAt(0) );
		for (String tuple : tuples) {
			String[] coordStringArray = StringUtils.explode(tuple, cs.charAt(0));
			double x = Double.parseDouble(coordStringArray[0]);
			double y = Double.NaN;
			double z = Double.NaN;
			if (coordStringArray.length > 1) {
				y = Double.parseDouble(coordStringArray[1]);
			}
			if (coordStringArray.length > 2) {
				z = Double.parseDouble(coordStringArray[2]);
			}
			Coordinate c = new Coordinate(x,y,z);
			ret.add(c);
		}


		if (transform != null) {
			transform(ret,transform);
		}
		return ret;
	}

	private void transform(List<Coordinate> ret, MathTransform transform) {
		for (Coordinate c : ret) {
			try {
				JTS.transform(c, c, transform);
			} catch (TransformException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private List<Coordinate> getJtsCoords(List<XMLCoordType> boxCoord, MathTransform transform) {
		log.warn("The input file uses the deprecated and discouraged gml CoordType!");
		List<Coordinate> ret = new ArrayList<Coordinate>();
		for (XMLCoordType t : boxCoord) {
			BigDecimal X = t.getX();
			BigDecimal Y = t.getY();
			BigDecimal Z = t.getZ();

			double x = X.doubleValue();
			double y = Double.NaN;
			if (Y != null) {
				y = Y.doubleValue();
			}

			double  z = Double.NaN;
			if (Z != null) {
				z = Z.doubleValue();
			}
			Coordinate c = new Coordinate(x,y,z);
			ret.add(c);

		}
		if (transform != null) {
			transform(ret,transform);
		}
		return ret;
	}


	public void setTargetSRS(String targetSRS) {
		try {
			this.targetSRS = CRS.decode(targetSRS, true);
		} catch (NoSuchAuthorityCodeException e) {
			throw new IllegalArgumentException(e);
		} catch (FactoryException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
