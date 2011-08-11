package playground.gregor.grips.io;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.AbstractSurfaceType;
import net.opengis.gml.v_3_1_1.PolygonType;
import net.opengis.gml.v_3_1_1.SurfacePropertyType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import playground.gregor.grips.helper.Building;
import playground.gregor.grips.jaxb.EDL.building.BuildingType;


public class BuildingsDeserializer {

	private static final GeometryFactory geoFac = new GeometryFactory();

	public List<Building> deserialize(String file) {
		JAXBElement<FeatureCollectionType> jaxfts = parseFeatureCollection(file);
		FeatureCollectionType fts = jaxfts.getValue();
		List<JAXBElement<? extends AbstractFeatureType>> list = fts.getFeatureMembers().getFeature();
		List<Building> blds = processList(list);
		return null;
	}

	private List<Building> processList(
			List<JAXBElement<? extends AbstractFeatureType>> list) {

		List<Building> blds = new ArrayList<Building>();
		for (JAXBElement<? extends AbstractFeatureType>  el : list) {
			AbstractFeatureType val = el.getValue();
			if (val instanceof BuildingType) {
				blds.add(processBuilding((BuildingType)val));
			} else {
				throw new RuntimeException("Unexpected feature type:" + val + "! Aboarding...");
			}
		}
		return blds;
	}

	private Building processBuilding(BuildingType val) {
		AbstractSurfaceType sf = val.getFloorplan().getSurface().getValue();

		Building ret = new Building();

		Geometry geo = null;
		if (sf instanceof PolygonType) {
			geo = getPolygonGeometry((PolygonType)sf);
		}

		ret.setGeometry(geo);

		int pop = val.getPopulation();
		ret.setNumOfPersons(pop);
		return ret;
	}

	private Polygon getPolygonGeometry(PolygonType sf) {
		JAXBElement<AbstractRingPropertyType> exter = sf.getExterior();
		List<JAXBElement<AbstractRingPropertyType>> inters = sf.getInterior();
		BigInteger dim = sf.getSrsDimension();
		LinearRing shell = getLinearRing(dim,exter);

		LinearRing [] holes = new LinearRing [inters.size()];
		int pos = 0;
		for (JAXBElement<AbstractRingPropertyType> inter : inters) {
			LinearRing lrInt = getLinearRing(dim,inter);
			holes[pos++] = lrInt;
		}
		Polygon p = geoFac.createPolygon(shell, holes);
		return p;
	}

	private LinearRing getLinearRing(BigInteger dim,
			JAXBElement<AbstractRingPropertyType> ring) {
		// TODO Auto-generated method stub
		return null;
	}

	private JAXBElement<FeatureCollectionType> parseFeatureCollection(
			String file) {

		JAXBElement<FeatureCollectionType> ret;
		JAXBContext jc;
		try {
			//			jc = JAXBContext.newInstance(playground.gregor.grips.jaxb.EDL.building.ObjectFactory.class);
			Class [] clazz = {playground.gregor.grips.jaxb.EDL.building.ObjectFactory.class,net.opengis.wfs.v_1_1_0.ObjectFactory.class};
			jc = JAXBContext.newInstance(clazz);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		Unmarshaller u;
		try {
			u = jc.createUnmarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		InputStream stream = null;
		try {
			stream = IOUtils.getInputstream(file);
			//			URL url = new URL("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
			//			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			//			Schema s = schemaFac.newSchema(url);
			//			u.setSchema(s);
			ret =  (JAXBElement<FeatureCollectionType>) u.unmarshal(stream);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (stream != null) { stream.close();	}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return ret;
	}


}
