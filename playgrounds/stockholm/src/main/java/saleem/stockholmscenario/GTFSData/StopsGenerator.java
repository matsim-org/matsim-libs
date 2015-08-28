package saleem.stockholmscenario.GTFSData;

import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

public class StopsGenerator {
	String busstops = "";
	String tramstops = "";
	String subwaystops = "";
	String ferrystops = "";
	String railstops = "";
	Element transitStops;
	public StopsGenerator(Document doc){
		this.transitStops=(Element)doc.getRootElement().getChild("transitStops");
	}
	public void GenerateStops(){
		String stop = "stop_id" + "," + "stop_name" + "," + "stop_lat" + "," + "stop_lon" + System.lineSeparator();
		busstops=busstops+stop;
		tramstops=tramstops+stop;
		subwaystops=subwaystops+stop;
		ferrystops=ferrystops+stop;
		railstops=railstops+stop;
		List stopfacilities = transitStops.getChildren("stopFacility");
		Iterator<Element> iter = stopfacilities.iterator();
		while(iter.hasNext()){
			Element stopfacility = iter.next();
			stop = stopfacility.getAttributeValue("id") + "," + stopfacility.getAttributeValue("name") + "," + stopfacility.getAttributeValue("x") + "," + stopfacility.getAttributeValue("y") + System.lineSeparator();
			switch(stopfacility.getAttributeValue("mode")){
				case "FERRY":
					ferrystops=ferrystops+stop;break;
				case "BUS":
					busstops=busstops+stop;break;
				case "TRAM":
					tramstops=tramstops+stop;break;
				case "TRAIN":
					subwaystops=subwaystops+stop;break;
				case "PENDELTAG":
					railstops=railstops+stop;
					
			}
		}
	}
	public void writeStops(){
		ReaderWriter rwiter = new ReaderWriter();
		rwiter.writeTextFile("H:\\GTFS Data\\bus\\stops.txt", busstops);
		rwiter.writeTextFile("H:\\GTFS Data\\rail\\stops.txt", railstops);
		rwiter.writeTextFile("H:\\GTFS Data\\tram\\stops.txt", tramstops);
		rwiter.writeTextFile("H:\\GTFS Data\\subway\\stops.txt", subwaystops);
		rwiter.writeTextFile("H:\\GTFS Data\\ferry\\stops.txt", ferrystops);
	}
	public void handleStops(){
		GenerateStops();
		writeStops();
	}
	
}
