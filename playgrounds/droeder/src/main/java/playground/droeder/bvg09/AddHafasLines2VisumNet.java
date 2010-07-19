package playground.droeder.bvg09;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitScheduleReader;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.xml.sax.SAXException;

import playground.droeder.DaPaths;

public class AddHafasLines2VisumNet {
	
	private final String PATH = DaPaths.OUTPUT + "bvg09/";
	
	private final String NETFILE = PATH + "intermediateNetwork.xml";
	private final String HAFASTRANSITFILE = PATH + "transitSchedule-HAFAS-Coord.xml";
	private final String VISUMTRANSITFILE = PATH + "intermediateTransitSchedule.xml";
	private final String FINALTRANSITFILE = PATH + "finalTransit.xml";
	
	
	private ScenarioImpl visumSc;
	private ScenarioImpl hafasSc;
	private ScenarioImpl newSc;
	
	private NetworkImpl visumNet;
	private TransitSchedule visumTransit;
	
	private TransitSchedule hafasTransit;
	
	private TransitSchedule finalTransitSchedule;
	private TransitScheduleFactory newTransitFactory;
	
	
	private TreeMap<Id, Id> vis2HafLines;
	
	private Map<Id, TransitStopFacility> facilities;
	
	public AddHafasLines2VisumNet(){
		this.visumSc = new ScenarioImpl();
		this.hafasSc = new ScenarioImpl();
		
		visumSc.getConfig().scenario().setUseTransit(true);
		readSchedule(VISUMTRANSITFILE, visumSc);
		visumTransit = visumSc.getTransitSchedule();
		new NetworkReaderMatsimV1(visumSc).readFile(NETFILE);
		visumNet = visumSc.getNetwork();
		
		hafasSc.getConfig().scenario().setUseTransit(true);
		readSchedule(HAFASTRANSITFILE, hafasSc);
		hafasTransit = hafasSc.getTransitSchedule();
		
		this.createHafasLineIdsFromVisum();
		
		newSc = new ScenarioImpl();
		newSc.getConfig().scenario().setUseTransit(true);
		newTransitFactory = newSc.getTransitSchedule().getFactory();
		finalTransitSchedule = newTransitFactory.createTransitSchedule();
	}
	
	
	
	
	
	private void readSchedule(String fileName, ScenarioImpl sc){
		TransitScheduleReader reader = new TransitScheduleReader(sc);
		try {
			reader.readFile(fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void createHafasLineIdsFromVisum(){
		vis2HafLines = new TreeMap<Id, Id>();
		String[] idToChar;
		StringBuffer createdHafasId;
		String hafasId;
		for(TransitLine line : visumSc.getTransitSchedule().getTransitLines().values()){
			createdHafasId = new StringBuffer();
			idToChar = line.getId().toString().split("");
			
			if(idToChar[1].equals("B")){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("U")){
				createdHafasId.append(idToChar[1]);
				createdHafasId.append(idToChar[3]);
				createdHafasId.append("   ");
			}else if(idToChar[1].equals("T") && idToChar[3].equals("M") ){
				if(idToChar[4].equals(" ") ){
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("   ");
				}else{
					createdHafasId.append(idToChar[3]);
					createdHafasId.append(idToChar[4]);
					createdHafasId.append(idToChar[5]);
					createdHafasId.append("  ");
				}
			}else if(idToChar[1].equals("T") && !(idToChar.equals("M")) ){
				createdHafasId.append(idToChar[3]);
				createdHafasId.append(idToChar[4]);
				createdHafasId.append("   ");
			}
			
			hafasId = createdHafasId.toString();
			if(createdHafasId.length()>0 && hafasSc.getTransitSchedule().getTransitLines().containsKey(new IdImpl(hafasId)) ){
				vis2HafLines.put(line.getId() , new IdImpl(hafasId));
			}
		}
	}
}
