package saleem.stockholmscenario.GTFSData;

import org.jdom.Document;

public class GTFSGenerator {
	public static void main(String[] args) {
		String path = "H:\\transitSchedule.xml";
		ReaderWriter rwiter = new ReaderWriter();
		Document doc = rwiter.readTrasnitSchedule(path);
		StopsGenerator stopgen = new StopsGenerator(doc);
		stopgen.handleStops();//creates and writes stops to a text file
		
	}
}
