package playground.gregor.grips.io;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.gml.v_3_2_1.AbstractCurveType;
import net.opengis.gml.v_3_2_1.AbstractFeatureType;
import net.opengis.gml.v_3_2_1.CoordinatesType;
import net.opengis.gml.v_3_2_1.DirectPositionListType;
import net.opengis.gml.v_3_2_1.FeatureArrayPropertyType;
import net.opengis.gml.v_3_2_1.FeatureCollectionType;
import net.opengis.gml.v_3_2_1.LineStringType;
import net.opengis.gml.v_3_2_1.ReferenceType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.xml.sax.SAXException;

import playground.gregor.grips.helper.ProtoLink;
import playground.gregor.grips.helper.ProtoNetwork;
import playground.gregor.grips.helper.ProtoNode;
import playground.gregor.grips.jaxb.inspire.network.NetworkPropertyType.NetworkRef;
import playground.gregor.grips.jaxb.inspire.network.NetworkReferenceType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.NumberOfLanesType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadLinkType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadNodeType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadWidthType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.SpeedLimitType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class NetworkDeserializer {

	private static final Logger log = Logger.getLogger(NetworkDeserializer.class);

	private ProtoNetwork net;

	private static final GeometryFactory geoFac = new GeometryFactory();

	public Network deserialize(String file) {
		NetworkImpl net = NetworkImpl.createNetwork();
		JAXBElement<FeatureCollectionType> fts = parseFeatureCollection(file);
		FeatureCollectionType ftst = fts.getValue();
		FeatureArrayPropertyType members = ftst.getFeatureMembers();
		List<JAXBElement<? extends AbstractFeatureType>> list = members.getAbstractFeature();
		ProtoNetwork protoNet = processList(list);
		createNetwork(net,protoNet);
		return net;
	}

	private void createNetwork(NetworkImpl net, ProtoNetwork protoNet) {

		for (ProtoNode n : protoNet.getProtoNodes().values()) {
			Id id = new IdImpl(n.getId());
			Coord c = MGC.coordinate2Coord(n.getCoord());
			net.createAndAddNode(id, c);
		}

		for (ProtoLink l : protoNet.getProtoLinks().values()) {

			Id id = new IdImpl(l.getId());
			Id fromId = new IdImpl(l.getFromNodeId());
			Id toId = new IdImpl(l.getToNodeId());
			Node fromNode = net.getNodes().get(fromId);
			Node toNode = net.getNodes().get(toId);
			double length = l.getLength();
			double numLanes = l.getNumOfLanes();
			double freespeed = l.getFreespeed();
			double cap = l.getCapacity();
			net.createAndAddLink(id, fromNode, toNode, length, freespeed, cap, numLanes);
		}

	}

	private ProtoNetwork processList(
			List<JAXBElement<? extends AbstractFeatureType>> list) {

		this.net = new ProtoNetwork();
		for (JAXBElement<? extends AbstractFeatureType>  e: list) {
			AbstractFeatureType val = e.getValue();
			if (val instanceof RoadNodeType) {
				processNode((RoadNodeType) val);
			} else if (val instanceof RoadLinkType) {
				processLink((RoadLinkType)val);
			} else if (val instanceof SpeedLimitType) {
				processSpeedLimit((SpeedLimitType)val);
			} else if (val instanceof NumberOfLanesType ) {
				processNumberOfLanes((NumberOfLanesType)val);
			} else if (val instanceof RoadWidthType) {
				processRoadWidth((RoadWidthType)val);
			}else {
				throw new RuntimeException("Unknown type: " + val + "! Abording...");
			}
		}

		return this.net;
	}

	private void processRoadWidth(RoadWidthType val) {
		log.warn("Calculating capacity from RoadWith this is unlikely to make much sense");
		double width = val.getWidth().getValue();
		for (NetworkRef  ref : val.getNetworkRef()) {
			JAXBElement<? extends NetworkReferenceType> nref = ref.getNetworkReference();
			NetworkReferenceType nreft = nref.getValue();
			ReferenceType t = nreft.getElement();
			String id = getIdFromRef(t);
			ProtoLink l = getProtoLink(id);
			double cap = 600 * width/3.5;
			l.setCapacity(cap);
		}
	}

	private void processNumberOfLanes(NumberOfLanesType val) {
		int lanes = val.getNumberOfLanes().intValue();
		for (NetworkRef  ref : val.getNetworkRef()) {
			JAXBElement<? extends NetworkReferenceType> nref = ref.getNetworkReference();
			NetworkReferenceType nreft = nref.getValue();
			ReferenceType t = nreft.getElement();
			String id = getIdFromRef(t);
			ProtoLink l = getProtoLink(id);
			l.setNumOfLanes(lanes);
		}

	}

	private void processSpeedLimit(SpeedLimitType val) {
		double limit = val.getSpeedLimitValue().getValue();
		for (NetworkRef  ref : val.getNetworkRef()) {
			JAXBElement<? extends NetworkReferenceType> nref = ref.getNetworkReference();
			NetworkReferenceType nreft = nref.getValue();
			ReferenceType t = nreft.getElement();
			String id = getIdFromRef(t);
			ProtoLink l = getProtoLink(id);
			l.setFreespeed(limit);
		}
	}

	private void processLink(RoadLinkType val) {
		String id = val.getId();

		ProtoLink l = getProtoLink(id);
		l.setId(id);
		ReferenceType sref = val.getStartNode();
		String fromId = getIdFromRef(sref);
		ReferenceType eref = val.getEndNode();
		String toId = getIdFromRef(eref);
		l.setFromNodeId(fromId);
		l.setToNodeId(toId);

		AbstractCurveType geo = val.getCentrelineGeometry().getAbstractCurve().getValue();

		if (geo instanceof LineStringType) {
			log.warn("Calculating link length from geometry. This makes only sens if the underlying SRS is plane!");
			LineStringType lrt = (LineStringType)geo;

			int dim = lrt.getSrsDimension().intValue();
			//			CoordinatesType ct = lrt.getCoordinates();
			List<Double> pl = lrt.getPosList().getValue();

			List<Coordinate> coords = getCoordinates(pl,dim);

			Coordinate [] coordsArray = new Coordinate[coords.size()];
			for (int i = 0; i < coords.size(); i++) {
				coordsArray[i] = coords.get(i);
			}

			double length = geoFac.createLineString(coordsArray).getLength();
			l.setLength(length);
		}


	}

	private List<Coordinate> getCoordinates(List<Double> pl, int dim) {
		if (dim < 2 || dim > 3) {
			throw new RuntimeException("the SRS Dimension must be 2 or 3! Abording...");
		}
		List<Coordinate> ret = new ArrayList<Coordinate>();
		int pnt = 0;
		while (pnt < pl.size()) {
			Coordinate c = new Coordinate();
			c.x = pl.get(pnt);
			c.y = pl.get(pnt+1);
			if (dim == 3) {
				c.z = pl.get(pnt+2);
			} else {
				c.z = 0;
			}
			ret.add(c);
			pnt += dim;
		}
		return ret;
	}

	private List<Coordinate> getCoordinates(CoordinatesType ct, int dim) {
		if (dim < 2 || dim > 3) {
			throw new RuntimeException("the SRS Dimension must be 2 or 3! Abording...");
		}
		String coords = ct.getValue();
		String[] coordsArray = StringUtils.explode(coords, ' ');
		List<Coordinate> c = parseCoordinate(coordsArray,dim);
		return c;
	}

	private String getIdFromRef(ReferenceType sref) {
		String shref = sref.getHref();
		String[] delimited = StringUtils.explode(shref, '#');
		String refId = delimited[1];
		return refId;
	}

	private ProtoLink getProtoLink(String id) {
		ProtoLink l = this.net.getProtoLinks().get(id);
		if (l == null) {
			l = new ProtoLink();
			this.net.getProtoLinks().put(id, l);
		}
		return l;
	}

	private void processNode(RoadNodeType e) {
		String id = e.getId();
		ProtoNode n = new ProtoNode();

		ProtoNode tmp = this.net.getProtoNodes().put(id, n);
		if (tmp != null) {
			throw new RuntimeException("Node with id: " + id + " already exist! Aborting...");
		}
		n.setId(id);
		CoordinatesType ct = e.getGeometry().getPoint().getCoordinates();
		BigInteger bigDim = e.getGeometry().getPoint().getSrsDimension();
		int dim = bigDim.intValue();
		List<Coordinate> c = getCoordinates(ct, dim);
		if (c.size() != 1) {
			throw new RuntimeException("A node need's have one and only one coordinate! Aborting ...");
		}
		n.setCoord(c.get(0));
	}



	private List<Coordinate> parseCoordinate(String[] coordsArray, int dim) {
		List<Coordinate> ret = new ArrayList<Coordinate>();

		int pnt = 0;
		while (pnt < coordsArray.length) {
			Coordinate c = new Coordinate();
			c.x = Double.parseDouble(coordsArray[pnt]);
			c.y = Double.parseDouble(coordsArray[pnt+1]);
			if (dim == 3) {
				c.z = Double.parseDouble(coordsArray[pnt+2]);
			} else {
				c.z = 0;
			}
			ret.add(c);
			pnt += dim;
		}

		return ret;
	}

	private JAXBElement<FeatureCollectionType> parseFeatureCollection(
			String file) {

		JAXBElement<FeatureCollectionType> ret;
		JAXBContext jc;
		try {
			jc = JAXBContext.newInstance(playground.gregor.grips.jaxb.inspire.roadtransportnetwork.ObjectFactory.class);
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
