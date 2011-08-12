package playground.gregor.grips.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.gml.v_3_1_1.AbstractFeatureType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.AbstractRingType;
import net.opengis.gml.v_3_1_1.AbstractSurfaceType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonType;
import net.opengis.wfs.v_1_1_0.FeatureCollectionType;

import org.matsim.core.utils.io.IOUtils;

import playground.gregor.grips.helper.Building;
import playground.gregor.grips.jaxb.EDL.building.BuildingType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class BuildingsDeserializer {

	private static final GeometryFactory geoFac = new GeometryFactory();

	public List<Building> deserialize(String uri) {
		JAXBElement<FeatureCollectionType> jaxfts = parseFeatureCollection(uri);
		FeatureCollectionType fts = jaxfts.getValue();
		List<JAXBElement<? extends AbstractFeatureType>> list = fts.getFeatureMembers().getFeature();
		List<Building> blds = processList(list);
		return blds;
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
		int dim = sf.getSrsDimension().intValue();
		if (dim < 2 || dim > 3) {
			throw new RuntimeException("the SRS Dimension must be 2 or 3! Abording...");
		}
		LinearRing shell = getLinearRing(dim,exter.getValue());

		LinearRing [] holes = new LinearRing [inters.size()];
		int pos = 0;
		for (JAXBElement<AbstractRingPropertyType> inter : inters) {
			LinearRing lrInt = getLinearRing(dim,inter.getValue());
			holes[pos++] = lrInt;
		}
		Polygon p = geoFac.createPolygon(shell, holes);
		return p;
	}

	private LinearRing getLinearRing(int dim,
			AbstractRingPropertyType ring) {
		AbstractRingType aRing = ring.getRing().getValue();
		LinearRingType lrt = null;
		if (aRing instanceof LinearRingType) {
			lrt = (LinearRingType)aRing;
		} else {
			throw new RuntimeException("Expected LinearRingType got: " + aRing.getClass());
		}
		List<Double> lst = lrt.getPosList().getValue();
		Coordinate[] coords = new Coordinate[lst.size()/dim];
		for (int i = 0; i < lst.size(); i += dim) {
			Coordinate c = new Coordinate();
			c.x = lst.get(i);
			c.y = lst.get(i+1);
			if (dim == 3) {
				c.z = lst.get(i+2);
			} else {
				c.z = 0;
			}
			coords[i/dim] = c;
		}

		LinearRing lr = geoFac.createLinearRing(coords);
		return lr;
	}

	private JAXBElement<FeatureCollectionType> parseFeatureCollection(
			String uri) {

		JAXBElement<FeatureCollectionType> ret =null;
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

			URI ur = new URI(uri);
			String schema = ur.getScheme();
			if (schema != null && schema.equals("http")) {
				stream = new URL(uri).openStream();
			} else {
				stream = IOUtils.getInputstream(uri);
			}


			//			URL url = new URL("http://schemas.opengis.net/gml/3.2.1/gml.xsd");
			//			XMLSchemaFactory schemaFac = new XMLSchemaFactory();
			//			Schema s = schemaFac.newSchema(url);
			//			u.setSchema(s);
			ret =  (JAXBElement<FeatureCollectionType>) u.unmarshal(stream);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
