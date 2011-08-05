package playground.gregor.grips.jaxb;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.gregor.grips.jaxb.EDL001.ObjectFactory;
import playground.gregor.grips.jaxb.EDL001.XMLEDL;
import playground.gregor.grips.jaxb.EDL001.XMLEPSGCodeSRSType;
import playground.gregor.grips.jaxb.EDL001.XMLEvacuationAreaType;
import playground.gregor.grips.jaxb.EDL001.XMLLinksType;
import playground.gregor.grips.jaxb.EDL001.XMLNetworkType;
import playground.gregor.grips.jaxb.EDL001.XMLNodeType;
import playground.gregor.grips.jaxb.EDL001.XMLNodesType;
import playground.gregor.grips.jaxb.EDL001.XMLPopulationType;
import playground.gregor.grips.jaxb.gml.CoordType;
import playground.gregor.grips.jaxb.gml.CoordinatesType;
import playground.gregor.grips.jaxb.gml.LinearRingType;


public class Test {

	public static void main(String [] args) throws JAXBException, IOException {

		ObjectFactory fac = new ObjectFactory();
		XMLEDL xmlEdl = fac.createXMLEDL();
		playground.gregor.grips.jaxb.gml.ObjectFactory gmlFac = new playground.gregor.grips.jaxb.gml.ObjectFactory();

		XMLEvacuationAreaType xmlEvacArea = fac.createXMLEvacuationAreaType();
		XMLEPSGCodeSRSType srs = fac.createXMLEPSGCodeSRSType();
		srs.setEPSGCode("EPSG:666");
		xmlEvacArea.setSRS(srs);

		LinearRingType lrType = gmlFac.createLinearRingType();
		for (double x = 0; x < 2; x++) {
			for (double y = 0; y<2; y++) {
				CoordType coordType = gmlFac.createCoordType();
				coordType.setX(new BigDecimal(x));
				coordType.setY(new BigDecimal(y));
				lrType.getCoord().add(coordType);
			}

		}
		CoordType coordType = gmlFac.createCoordType();
		coordType.setX(new BigDecimal(0.));
		coordType.setY(new BigDecimal(0.));
		lrType.getCoord().add(coordType);
		lrType.setSrsName("EPSG:666");
		xmlEvacArea.setArea(lrType);
		xmlEdl.setEvacuationArea(xmlEvacArea);

		//		XMLNetworkType xmlNet = fac.createXMLNetworkType();
		//
		//		XMLNodesType xmlNodes = fac.createXMLNodesType();
		//		XMLNodeType n1 = fac.createXMLNodeType();
		//		n1.setXCoordinate(0);
		//		n1.setYCoordinate(1);
		//		n1.setId(0);
		//		xmlNodes.getNode().add(n1);
		//
		//		XMLNodeType n2 = fac.createXMLNodeType();
		//		n2.setXCoordinate(1);
		//		n2.setYCoordinate(0);
		//		n2.setId(1);
		//		xmlNodes.getNode().add(n2);
		//
		//		xmlNet.setNodes(xmlNodes);
		//
		//
		//		XMLLinksType xmlLinks = fac.createXMLLinksType();
		//		xmlLinks.setFromNodeId(0);
		//		xmlLinks.setToNodeId(1);
		//		xmlLinks.setToNodeId(0);
		//		xmlNet.setLinks(xmlLinks);
		//		xmlNet.setSRS(srs);
		//		xmlEdl.setNetwork(xmlNet);

		XMLPopulationType xmlPop = fac.createXMLPopulationType();
		xmlPop.setSRS(srs);
		xmlPop.setArea(lrType);
		xmlPop.setNumOfPeople(new BigInteger("1"));
		xmlEdl.setPopulation(xmlPop );

		JAXBContext jc = JAXBContext.newInstance(playground.gregor.grips.jaxb.EDL001.ObjectFactory.class);
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
				Boolean.TRUE);
		m.setProperty("jaxb.schemaLocation", "http://vsp.tu-berlin.de/repos/public-svn/xml-schemas/" + " " + "http://vsp.tu-berlin.de/repos/public-svn/xml-schemas/EDL_v0.0.1.xsd");
		BufferedWriter bufout = IOUtils.getBufferedWriter("/Users/laemmel/tmp/evacuation.xml");
		m.marshal(xmlEdl, bufout);
		bufout.close();
	}

}
