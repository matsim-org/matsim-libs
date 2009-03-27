/**
 * 
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author yu
 * 
 */
public class MZComparisonDataIO implements TabularFileHandler {
	private MZComparisonData data2compare;

	public void setData2Compare(MZComparisonData data2compare) {
		this.data2compare = data2compare;
	}

	public void startRow(String[] row) {
		// TODO save information from MZ
	}

	public void write(String outputBase) {
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(outputBase + ".txt");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// TODO draw chart
	}

	public void readMZData(String inputFilename) {
		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "Verkehrsmittel" });// don't forget
		// it!!!!!!!!!!
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(inputFilename);

		try {
			new TabularFileParser().parse(tfpc, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// -----------only for testing------------------------------
	public static void main(String[] args) {
		String inputFilename = "../matsimTests/analysis/Vergleichswert.txt";
		String outputBase = "../matsimTests/??????";
		MZComparisonDataIO mzcdi = new MZComparisonDataIO();
		mzcdi.readMZData(inputFilename);
		mzcdi.write(outputBase);
	}
}
