package gunnar.ihop2.regent;

import java.io.IOException;

import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class EmmeTravelTimes implements TabularFileHandler {

	private final Matrices travelTimes = new Matrices();

	private Matrix currentlyReadMatrix = null;
	
	public EmmeTravelTimes() {
	}
	
	public void read(final String fileName) {
		this.currentlyReadMatrix = this.travelTimes.createMatrix(fileName, "");
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setCommentTags(new String[] { "c", "t", "a" });
		try {
			parser.parse(fileName, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void write(final String fileName) {
		final MatricesWriter writer = new MatricesWriter(this.travelTimes);
		writer.write(fileName);
	}

	@Override
	public String preprocess(String line) {
		return line;
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void startRow(String[] row) {
		final String fromZoneId = row[0];
		for (int i = 1; i < row.length; i++) {
			final String[] destData = row[i].split("\\Q" + ":" + "\\E");
			final String toZoneId = destData[0];
			final double value = Double.parseDouble(destData[1]);
			this.currentlyReadMatrix.createEntry(fromZoneId, toZoneId, value);
		}
	}

	@Override
	public void endDocument() {

	}

	public static void main(String[] args) {

		System.out.println("STARTED");
		
		final String work = "./test/original/EMME_traveltimes_WORK_mf8.txt";
		final EmmeTravelTimes emmeTT = new EmmeTravelTimes();
		emmeTT.read(work);
		emmeTT.write("./test/original/EMME_traveltimes_WORK_mf8.xml");

		System.out.println("DONE");
	}

}
