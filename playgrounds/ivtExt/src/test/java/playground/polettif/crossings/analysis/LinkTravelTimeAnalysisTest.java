package playground.polettif.crossings.analysis;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkTravelTimeAnalysisTest {

	private List<Id<Link>> crossingIds = new ArrayList<>();
	String eventsFile;
	String outputCSV;

	@Before
	public void prepareTest() {
		/*
		// set small
	//	eventsFile = "C:/Users/polettif/Desktop/output/small_02/ITERS/it.1/1.events.xml.gz";
	//	outputCSV = "C:/Users/polettif/Desktop/output/analysis/small2.csv";
		crossingIds.add(Id.createLinkId("34"));
		crossingIds.add(Id.createLinkId("43"));
		
		// set for pt-tutorial
		linkIdsPtTutorial.put(Id.createLinkId("1222-x22"), Id.createLinkId("1222"));
		linkIdsPtTutorial.put(Id.createLinkId("2212-x12"), Id.createLinkId("2212"));
		linkIdsPtTutorial.put(Id.createLinkId("2322-x22"), Id.createLinkId("2322"));
		linkIdsPtTutorial.put(Id.createLinkId("2223-x23"), Id.createLinkId("2223"));
		linkIdsPtTutorial.put(Id.createLinkId("2232-x32"), Id.createLinkId("2232"));
		linkIdsPtTutorial.put(Id.createLinkId("3222-x22"), Id.createLinkId("3222"));
		linkIdsPtTutorial.put(Id.createLinkId("3231-x31"), Id.createLinkId("3231"));
		linkIdsPtTutorial.put(Id.createLinkId("3132-x32"), Id.createLinkId("3132"));

		crossingIds.add(Id.createLinkId("1222-x"));
		crossingIds.add(Id.createLinkId("2212-x"));
		crossingIds.add(Id.createLinkId("2322-x"));
		crossingIds.add(Id.createLinkId("2223-x"));
		*/

	}

	@Test
	public void runAnalysis() throws FileNotFoundException, UnsupportedEncodingException {
		//LinkTravelTimeAnalysis.run(crossingIds, eventsFile, outputCSV, "06:00:00", "07:00:00");
	}

}