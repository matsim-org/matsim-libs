package playground.gregor.grips.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import net.opengis.gml.v_3_2_1.AbstractCurveType;
import net.opengis.gml.v_3_2_1.CoordinatesType;
import net.opengis.gml.v_3_2_1.CurvePropertyType;
import net.opengis.gml.v_3_2_1.DirectPositionListType;
import net.opengis.gml.v_3_2_1.DirectPositionType;
import net.opengis.gml.v_3_2_1.FeatureArrayPropertyType;
import net.opengis.gml.v_3_2_1.FeatureCollectionType;
import net.opengis.gml.v_3_2_1.LineStringType;
import net.opengis.gml.v_3_2_1.MeasureType;
import net.opengis.gml.v_3_2_1.PointPropertyType;
import net.opengis.gml.v_3_2_1.PointType;
import net.opengis.gml.v_3_2_1.ReferenceType;
import net.opengis.gml.v_3_2_1.SpeedType;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.grips.jaxb.inspire.network.LinkReferenceType;
import playground.gregor.grips.jaxb.inspire.network.NetworkPropertyType.NetworkRef;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.NumberOfLanesType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.ObjectFactory;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadLinkType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadNodeType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.RoadWidthType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.SpeedLimitMinMaxValueType;
import playground.gregor.grips.jaxb.inspire.roadtransportnetwork.SpeedLimitType;

public class NetworkSerializer {



	private final ObjectFactory rnetFac = new ObjectFactory();
	private final net.opengis.gml.v_3_2_1.ObjectFactory gmlFac = new net.opengis.gml.v_3_2_1.ObjectFactory();
	private final playground.gregor.grips.jaxb.inspire.network.ObjectFactory netFac = new playground.gregor.grips.jaxb.inspire.network.ObjectFactory();

	private final Network network;


	public NetworkSerializer(Network network) {
		this.network = network;
	}


	public void serialize(String filename) {
		JAXBElement<FeatureCollectionType> fts = createJAXBObjects();

		Class<ObjectFactory> RT = playground.gregor.grips.jaxb.inspire.roadtransportnetwork.ObjectFactory.class;
		JAXBContext jc = null;
		try {
			jc = JAXBContext.newInstance(RT);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}

		Marshaller m = null;
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
			m.setProperty("jaxb.schemaLocation", "http://inspire-foss.googlecode.com/svn/trunk/schemas/inspire/v3.0.1/" + " " + "http://inspire-foss.googlecode.com/svn/trunk/schemas/inspire/v3.0.1/RoadTransportNetwork.xsd");
		} catch (PropertyException e) {
			throw new RuntimeException(e);
		}

		BufferedWriter bufout = IOUtils.getBufferedWriter(filename);
		try {
			m.marshal(fts, bufout);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		try {
			bufout.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


	}

	private JAXBElement<FeatureCollectionType> createJAXBObjects() {
		FeatureArrayPropertyType array = this.gmlFac.createFeatureArrayPropertyType();

		createRoadNodes(array);
		createRoadLinks(array);

		FeatureCollectionType col = this.gmlFac.createFeatureCollectionType();
		col.setFeatureMembers(array);
		JAXBElement<FeatureCollectionType> ret = this.gmlFac.createFeatureCollection(col);
		return ret;
	}


	private void createRoadLinks(FeatureArrayPropertyType array) {
		for (org.matsim.api.core.v01.network.Link link : this.network.getLinks().values()) {
			RoadLinkType rlt = this.rnetFac.createRoadLinkType();

			ReferenceType refFrom = this.gmlFac.createReferenceType();
			refFrom.setHref("#"+ link.getFromNode().getId().toString());
			ReferenceType refTo = this.gmlFac.createReferenceType();
			refTo.setHref("#"+ link.getToNode().getId().toString());


			CurvePropertyType cpt = this.gmlFac.createCurvePropertyType();
			LineStringType ls = this.gmlFac.createLineStringType();
			ls.setSrsDimension(new BigInteger("2"));

			List<Double> coordList = new ArrayList<Double>();
			coordList.add(link.getFromNode().getCoord().getX());
			coordList.add(link.getFromNode().getCoord().getY());
			coordList.add(link.getToNode().getCoord().getX());
			coordList.add(link.getToNode().getCoord().getY());

			DirectPositionListType dpl = this.gmlFac.createDirectPositionListType();
			dpl.setValue(coordList);

			ls.setPosList(dpl);

			JAXBElement<AbstractCurveType> ac = this.gmlFac.createAbstractCurve(ls);
			cpt.setAbstractCurve(ac);
			rlt.setCentrelineGeometry(cpt);


			rlt.setId(link.getId().toString());
			rlt.setStartNode(refFrom);
			rlt.setEndNode(refTo);

			//			List l = rlt.get

			JAXBElement<RoadLinkType> rl = this.rnetFac.createRoadLink(rlt);
			array.getAbstractFeature().add(rl);


			SpeedLimitType sl = this.rnetFac.createSpeedLimitType();
			SpeedType s = this.gmlFac.createSpeedType();
			s.setValue(link.getFreespeed());
			sl.setSpeedLimitValue(s);
			sl.setSpeedLimitMinMaxType(SpeedLimitMinMaxValueType.MAXIMUM);

			ReferenceType lref = this.gmlFac.createReferenceType();
			lref.setHref("#" + link.getId().toString());
			LinkReferenceType lreft = this.netFac.createLinkReferenceType();
			lreft.setElement(lref);
			JAXBElement<LinkReferenceType> ref = this.netFac.createLinkReference(lreft);

			NetworkRef netRef = this.netFac.createNetworkPropertyTypeNetworkRef();
			netRef.setNetworkReference(ref);
			sl.getNetworkRef().add(netRef);

			JAXBElement<SpeedLimitType> freepseed = this.rnetFac.createSpeedLimit(sl);
			array.getAbstractFeature().add(freepseed);

			NumberOfLanesType nlt = this.rnetFac.createNumberOfLanesType();
			nlt.setNumberOfLanes(new BigInteger((int)link.getNumberOfLanes()+""));
			nlt.getNetworkRef().add(netRef);
			JAXBElement<NumberOfLanesType> nl = this.rnetFac.createNumberOfLanes(nlt);
			array.getAbstractFeature().add(nl);

			RoadWidthType rwt = this.rnetFac.createRoadWidthType();
			MeasureType mt = this.gmlFac.createMeasureType();
			mt.setValue(3.5*link.getNumberOfLanes());
			rwt.setWidth(mt );
			rwt.getNetworkRef().add(netRef);
			JAXBElement<RoadWidthType> rw = this.rnetFac.createRoadWidth(rwt);
			array.getAbstractFeature().add(rw);
		}

	}


	private void createRoadNodes(FeatureArrayPropertyType array) {
		for (Node node : this.network.getNodes().values()) {
			RoadNodeType rnt = this.rnetFac.createRoadNodeType();

			PointPropertyType pp = this.gmlFac.createPointPropertyType();
			PointType p = this.gmlFac.createPointType();
			p.setSrsDimension(new BigInteger("2"));
			CoordinatesType c = this.gmlFac.createCoordinatesType();
			c.setValue(node.getCoord().getX() + " " + node.getCoord().getY());
			//			p.setCoordinates(c);
			DirectPositionType dp = this.gmlFac.createDirectPositionType();
			dp.getValue().add(node.getCoord().getX());
			dp.getValue().add(node.getCoord().getY());
			p.setPos(dp);
			pp.setPoint(p);
			rnt.setGeometry(pp);

			rnt.setId(node.getId().toString());//TODO need this to be unique? In MATSim links and nodes can have same ids but I don't think that this is the case in INSPIRE or GML

			JAXBElement<RoadNodeType> rn = this.rnetFac.createRoadNode(rnt);
			array.getAbstractFeature().add(rn);
		}
	}


	public static void main(String [] args) {
		String conf = "/Users/laemmel/devel/dfg/config2d.xml";
		Config c = ConfigUtils.loadConfig(conf);
		Scenario sc = ScenarioUtils.loadScenario(c);
		new NetworkSerializer(sc.getNetwork()).serialize("/Users/laemmel/tmp/network.xml");



	}

}
