package playground.yalcin;

import java.io.IOException;

import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class PuT_trip_finder {

	public PuT_trip_finder() {

	}

	public static void findNearestStops() {

		/*
		 * read text file with segments expected format: 0:PersonID 1:TripID
		 * 2:SegmentID 3:XStartingPoint 4:YStartingPoint 5:ZStartingPoint
		 * 6:StartingDate 7:StartingTime 8:XEndingPoint 9:YEndingPoint
		 * 10:ZEndingPoint 11:EndingDate 12:EndingTime 13:Distance 14:TravelTime
		 * 15:Probability_Walk 16:Probability_Bike 17:Probability_Car
		 * 18:Probability_UrbanPuT 19:Probability_Rail
		 */
		final TabularFileParser parser = new TabularFileParser();
		final TabularFileParserConfig parserConfig = new TabularFileParserConfig();
		parserConfig
	//			.setFileName("C:\\Users\\yalcin\\Desktop\\Zurich\\Zurichdata\\Nadie\\Nadie_10.04.2008\\New FolderWalkAndPuTSegmentsYalcin_WithTime\\WalkAndPuTSegmentsYalcin_ZH/javainput.txt");
		.setFileName("C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code\\new\\SegmentInformation.txt");
		parserConfig.setDelimiterTags(new String[] { "\t" });
		PuTTripFinderHandlerDistance handler = new PuTTripFinderHandlerDistance(0.4,
				"C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code/new/results21.txt");

		try {
			// this will read the file AND write out the looked up data
			parser.parse(parserConfig, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		handler.finish();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		findNearestStops();
	}

}
