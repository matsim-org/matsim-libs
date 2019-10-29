package playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;
import playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector.RouteStopInfo;



public class LinksPtInfoReader {
	List<File> fileList = new ArrayList<File>();
	Map<Id<Link>, LinkExtendImp> LinkExtendImps = new HashedMap();
	PtAccessabilityConfig analysisConfig;
	static Map<String,Map<Double,Double>> district2Time2Score = new HashedMap();
	
	public LinksPtInfoReader(List<String> fileList) {
		for(String string: fileList) {
			File file = new File(string);
			this.fileList.add(file);
		}
	}
	
	public void read () throws ParserConfigurationException, SAXException, IOException {
		for(File file : fileList) {
			System.out.println("readFile :" + file.toString());
			
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(file);
			
			doc.getDocumentElement().normalize();

			System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
					
			NodeList nList = doc.getElementsByTagName("LinkId");
			int a = 0;
			for(int temp=0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
				Element element = (Element) nNode;
				Id<Link> linkId = Id.create(Integer.valueOf(element.getAttribute("id")), Link.class);
				LinkExtendImp linkExtendImp = new LinkExtendImp(linkId);
				a++;
				if(a%10000 == 0) {
					System.out.println(linkId + "is the " + a+"th Link");
				}
				
				List<Map<Map<Id<TransitStopFacility>,RouteStopInfo>, Double>> infoList = new LinkedList<Map<Map<Id<TransitStopFacility>,RouteStopInfo>,Double>>();
				NodeList countList = element.getElementsByTagName("StopInfo");
				
				for(int temp2 = 0; temp2 < countList.getLength(); temp2++) {
					Node countNode = countList.item(temp2);
					
					Element countElement = (Element) countNode;
					Id<TransitStopFacility> trId = Id.create(countElement.getElementsByTagName("StopId").item(0).getTextContent(), TransitStopFacility.class);
					String transportMode = countElement.getElementsByTagName("TransportMode").item(0).getTextContent();
					
						double distance = Double.valueOf(countElement.getElementsByTagName("Distance").item(0).getTextContent());
						
						double departureTime = Double.valueOf(countElement.getElementsByTagName("DepartureTime").item(0).getTextContent());
					
						
						
						RouteStopInfo routeStopInfo = new RouteStopInfo();
						routeStopInfo.setDepatureTime(departureTime);
						routeStopInfo.setTransportMode(transportMode);
						
						linkExtendImp.addPtInfos(trId, routeStopInfo, distance);	
					
				}
				this.LinkExtendImps.put(linkId, linkExtendImp);
			}
		}
	}
	public Map<Id<Link>, LinkExtendImp> getLinkExtendImps() {
		return LinkExtendImps;
	}
	
	private static void scoreCaculate(double beginnTime, double EndTime, Collection<LinkExtendImp> collection) throws Exception {
		
		for(LinkExtendImp linkExtendImp: collection) {
			linkExtendImp.setTime2Score(beginnTime, 0.);
			
			for(Map<Id<TransitStopFacility>, RouteStopInfo> key: linkExtendImp.getPtInfos().keySet()) {
				for(RouteStopInfo routeStopInfo: key.values()) {
					if(routeStopInfo.getDepatureTime()>=beginnTime && routeStopInfo.getDepatureTime()<EndTime) {
						linkExtendImp.setTime2Score(beginnTime, (linkExtendImp.getTime2Score().get(beginnTime)+1));
					}
				}
			}

		}
	}
	public static void main(String[] args) throws Exception {
		
	LinkedList<String> fileList = new LinkedList<String>();
	String file1 ="C:/Users/VW3RCOM/Desktop/ptAnalysisOutputFileDistrict2.0/LinksPtInfo_Mitte_area.xml";
	
	fileList.add(file1);
	LinksPtInfoReader linksPtInfoReader = new LinksPtInfoReader(fileList);

	linksPtInfoReader.read();
	
	scoreCaculate(18900., 19800., linksPtInfoReader.getLinkExtendImps().values());
	double sum = 0;
	for(LinkExtendImp linkExtendImp : linksPtInfoReader.getLinkExtendImps().values()) {
		sum = sum + linkExtendImp.getTime2Score().get(18900.);
		System.out.println(linkExtendImp.getId().toString()+" "+linkExtendImp.getTime2Score().get(18900.)+" sum= "+sum);
		
	}
	
	
	


	System.out.println("finish ");
	}

}
