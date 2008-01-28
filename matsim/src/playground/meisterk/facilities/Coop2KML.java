package playground.meisterk.facilities;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.matsim.utils.misc.TimeFormatter;
import org.matsim.utils.vis.kml.*;

public class Coop2KML {

	private static KML myKML = null;
	private static Document myKMLDocument = null;
	private static String inputFilename = "/Users/meisterk/Documents/workspace/matsimJ/input/coopzh.txt";
	private static String kmlFilename = "/Users/meisterk/Documents/workspace/matsimJ/input/coopzh.kmz";
	private static Style openStyle = null;
	private static Style closedStyle = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		setUp();
		generateFacilities();
		write();

	}

	private static void setUp() {

		myKML = new KML();
		myKMLDocument = new Document("the root document");
		myKML.setFeature(myKMLDocument);

		openStyle = new Style("openStyle");
		myKMLDocument.addStyle(openStyle);
		openStyle.setIconStyle(new IconStyle(new Icon("/Users/meisterk/Documents/workspace/matsimJ/input/coop_dhtml.jpg")));
		closedStyle = new Style("closedStyle");
		myKMLDocument.addStyle(closedStyle);
		closedStyle.setIconStyle(new IconStyle(new Icon("/Users/meisterk/Documents/workspace/matsimJ/input/coop_dhtml_invert.jpg")));

	}

	private static void generateFacilities() {

		List<String> lines = null;
		String[] tokens = null;
		Placemark aCoopPhase = null;
		Folder aCoop = null;
		GregorianCalendar gcBegin, gcEnd;

		try {
			lines = FileUtils.readLines(new File(inputFilename), "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Folder wednesdayFolder = new Folder(
				"wednesdayFolder",
				"wednesdayFolder",
				"Wednesday",
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE);

		myKMLDocument.addFeature(wednesdayFolder);

		TimeFormatter tf = new TimeFormatter(TimeFormatter.TIMEFORMAT_HHMM);
		ArrayList<Double> openClose = new ArrayList<Double>();

		for (String line : lines) {
		
			tokens = line.split("\t");
			System.out.println(tokens[7] + "\t" + tokens[9] + "\t" + tokens[10] + "\t" + tokens[11]);
			
			openClose.add(new Double(0.0));

			try {
				for (int pos = 22; pos <= 25; pos++) {

					Double time = tf.parseTime(tokens[pos]) / 3600;
					if (!time.equals(Double.NEGATIVE_INFINITY)) {
						openClose.add(time);
					}

				}
			} catch (java.lang.ArrayIndexOutOfBoundsException e) {
				System.err.println("invalid entry. Ignoring...");
				
				continue;
			}

			openClose.add(new Double(23.0 + 59/60));		

			aCoop = new Folder(
					tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11],
					tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11],
					Feature.DEFAULT_DESCRIPTION,
					Feature.DEFAULT_ADDRESS,
					Feature.DEFAULT_LOOK_AT,
					Feature.DEFAULT_STYLE_URL,
					Feature.DEFAULT_VISIBILITY,
					Feature.DEFAULT_REGION,
					Feature.DEFAULT_TIME_PRIMITIVE);
			
			wednesdayFolder.addFeature(aCoop);
			
			for (int ii=0; ii < openClose.size() - 1; ii++) {

				gcBegin = new GregorianCalendar(
						1970, 0, 1, 
						openClose.get(ii).intValue(), 
						(int) (openClose.get(ii).intValue() - openClose.get(ii).intValue()) * 60, 
						0);
				gcEnd = new GregorianCalendar(
						1970, 0, 1, 
						openClose.get(ii + 1).intValue(), 
						(int) (openClose.get(ii + 1).intValue() - openClose.get(ii + 1).intValue()) * 60, 
						0);

				String styleURL = null;
				String description = null;
				
				if (ii % 2 == 0) {
					styleURL = closedStyle.getStyleUrl();
					description = "closed";
				} else {
					styleURL = openStyle.getStyleUrl();
					description = "opened";
				}
				
				aCoopPhase = new Placemark(
						tokens[7] + " " + tokens[9] + ", " + tokens[10] + " " + tokens[11] + Integer.toString(ii),
						Feature.DEFAULT_NAME,
						description,
						tokens[9] + ", " + tokens[11] + ", Schweiz",
						Feature.DEFAULT_LOOK_AT,
						styleURL,
						Feature.DEFAULT_VISIBILITY,
						Feature.DEFAULT_REGION,
						new TimeSpan(gcBegin, gcEnd));

				aCoop.addFeature(aCoopPhase);		
			}
			
			openClose.clear();
		
		}
		
		
	}

	private static void write() {

		System.out.println("    writing KML files out...");

		KMZWriter writer;
		writer = new KMZWriter(kmlFilename);
		writer.writeMainKml(myKML);
		writer.close();

		System.out.println("    done.");

	}

}
