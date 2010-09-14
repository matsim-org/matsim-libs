package playground.joschka.lsacvs2kml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;



public class LSANode2KML {
	
	public static void main(String[] args){
	
		
		
		
		Scenario scenario = new ScenarioImpl();
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile("/home/jbischoff/m44_344_big.xml");
		List<LSA> ampelliste = new ArrayList<LSA>();
		Map<Id,Node> nodeList = new HashMap<Id,Node>();
		nodeList=network.getNodes();
		CoordinateTransformation ct = 
			TransformationFactory.getCoordinateTransformation(TransformationFactory.GK4,TransformationFactory.WGS84);
		
		for (Map.Entry<Id,Node> entry: nodeList.entrySet()){
			LSA ampel = new LSA();
			Node current = entry.getValue();
			ampel.setLongName(current.getId().toString());
			ampel.setShortName(current.getId().toString());
			Coord kurt = new CoordImpl(current.getCoord());
			kurt = ct.transform(kurt);
			ampel.setXcord(kurt.getX());
			ampel.setYcord(kurt.getY());
			ampelliste.add(ampel);
		}
		try {
				
			
			
			writeListtoXML(ampelliste,"/home/jbischoff/m44_344_big.kml");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		

		
		
		
		

		
	}
	public static void writeListtoXML(List<LSA> lsaliste, String outputfile) throws IOException{
		String out = outputfile;
		List<LSA> lsalist = lsaliste;
		Element rE = new Element("kml");
		rE.setAttribute("xmlnsns2", "http://www.w3.org/2005/Atom");
		Element Dok = new Element("Document");
		for (LSA ampeln:lsalist){
			Element currentPM = new Element("Placemark");
			Element shortname = new Element("name");
			shortname.setText(ampeln.getShortName());
			currentPM.addContent(shortname);
			
			Element longname = new Element("description");
			longname.setText(ampeln.getLongName());
			currentPM.addContent(longname);
			
			Element currentPoint = new Element("Point");
			Element cord = new Element("coordinates");
			cord.setText(ampeln.getXcord()+","+ampeln.getYcord()+",0.0");
			currentPoint.addContent(cord);
			
			
			currentPM.addContent(currentPoint);
			Dok.addContent(currentPM);
			
		}
		rE.addContent(Dok);
		
		
		
		Document doc = new Document(rE);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		FileOutputStream output = new FileOutputStream(out);
		outputter.output(doc, output);

		
		
		
			}

}

