package air;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class OsmAerowayParser extends MatsimXmlParser {
	
	private final CoordinateTransformation transform;
	private OsmNode currentNode;
	protected Map<String, Coord> airports = new HashMap<String, Coord>();

	public OsmAerowayParser(final CoordinateTransformation transform) {
		this.transform = transform;
		this.setValidating(false);
	}
	

	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		if ("node".equals(name)) {
			Id id = new IdImpl(atts.getValue("id"));				
			double lat = Double.parseDouble(atts.getValue("lat"));
			double lon = Double.parseDouble(atts.getValue("lon"));
			this.currentNode = new OsmNode(id, this.transform.transform(new CoordImpl(lon, lat)));
		} else if ("way".equals(name)) {
			System.out.println("way gefunden.");
		} else if ("nd".equals(name)) {
			System.out.println("nd gefunden.");
			}
		 else if ("tag".equals(name)) {
			if (this.currentNode != null) {
				if (atts.getValue("k").equals("iata")) {
					this.currentNode.tags.put(atts.getValue("k"), atts.getValue("v"));
					if (!airports.containsKey(atts.getValue("v"))) {
						String iataCode = this.currentNode.tags.get("iata");
						if (iataCode.length()>=3)iataCode = iataCode.substring(0, 3);
						airports.put(iataCode, this.currentNode.coord);
					}
				}
			}
		}
	}
	



	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {
		if ("way".equals(name)) { }
		else if ("node".equals(name)){
			this.currentNode = null;
		}
	}
	
	
	
	private void parse(final String osmFilename, final InputStream stream) throws SAXException, ParserConfigurationException, IOException {
		
		OsmAerowayParser parser = new OsmAerowayParser(transform);
		if (stream != null) {
			parser.parse(new InputSource(stream));
		} else {
			parser.parse(osmFilename);
		}
	}
	
	public void writeToFile(String filename) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			Iterator it = this.airports.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pairs = (Map.Entry)it.next();
		        bw.write(pairs.getKey().toString()+pairs.getValue().toString());
		        bw.newLine();
		    }
		    bw.flush();
		    bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	class OsmNode {
		public Id id;
		public final Coord coord;
		public final Map<String, String> tags = new HashMap<String, String>();

		public OsmNode(final Id id, final Coord coord) {
			this.id = id;
			this.coord = coord;
		}
	}
	
	
}


	

