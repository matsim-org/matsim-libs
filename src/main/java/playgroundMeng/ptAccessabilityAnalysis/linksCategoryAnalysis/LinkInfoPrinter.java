package playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfo;


public class LinkInfoPrinter {
	private static final Logger logger = Logger.getLogger(LinkInfoPrinter.class);
	
	private List<LinkExtendImp> linkExtendImps;
	
	public LinkInfoPrinter(List<LinkExtendImp> linkExtendImps){
		
		this.linkExtendImps =linkExtendImps;
		
	}
	
	public void print(String fileName) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("LinkPtInfo");
		doc.appendChild(rootElement);
		
		int ct = 1;
		for(LinkExtendImp linkExtendImp: this.linkExtendImps) {
			if(ct %1000 == 0) {
				logger.info("beginn to writer the "+ct +"th Link");
			}
			ct ++;
			if(!linkExtendImp.getPtInfos().isEmpty()) {
				Element linkIdElement = doc.createElement("LinkId");
				
				Attr attr = doc.createAttribute("id");
				attr.setValue(linkExtendImp.getId().toString());
				
				linkIdElement.setAttributeNode(attr);
				rootElement.appendChild(linkIdElement);
				
				int a = 1;
				for(Map<Id<TransitStopFacility>, RouteStopInfo> key : linkExtendImp.getPtInfos().keySet()) {
					
					Element stopInfo = doc.createElement("StopInfo");
					Attr count = doc.createAttribute("count");
					count.setValue(Integer.toString(a));
					stopInfo.setAttributeNode(count);
					a++;
					linkIdElement.appendChild(stopInfo);
					for(Id<TransitStopFacility> stopFacilityId : key.keySet()) {
						Element stopId = doc.createElement("StopId");
						stopId.appendChild(doc.createTextNode(stopFacilityId.toString()));
						
						Element distancElement = doc.createElement("Distance");
						distancElement.appendChild(doc.createTextNode(linkExtendImp.getPtInfos().get(key).toString()));
						
						Element departureTimElement = doc.createElement("DepartureTime");
						departureTimElement.appendChild(doc.createTextNode(Double.toString(key.get(stopFacilityId).getDepatureTime())));
						
						Element transportModElement = doc.createElement("TransportMode");
						transportModElement.appendChild(doc.createTextNode(key.get(stopFacilityId).getTransportMode()));
						
						stopInfo.appendChild(stopId);
						stopInfo.appendChild(distancElement);
						stopInfo.appendChild(departureTimElement);
						stopInfo.appendChild(transportModElement);
					}	
				}
			}
		}
			
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer tr = transformerFactory.newTransformer();
		
		tr.setOutputProperty(OutputKeys.INDENT, "yes");
        tr.setOutputProperty(OutputKeys.METHOD, "xml");
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new File(fileName));
		tr.transform(source, result);
		
		logger.info("linkPtFile is printed");

	}
}
