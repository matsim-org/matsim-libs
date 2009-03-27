/**
 * 
 */
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private int lineCount = 0;

	private String[] chartRows;

	private List<String> chartColumns = new ArrayList<String>();

	private Map<String, Map<String, Double>> values = new HashMap<String, Map<String, Double>>();

	public void setData2Compare(MZComparisonData data2compare) {
		this.data2compare = data2compare;
	}

	public void startRow(String[] row) {
		// TODO save information from MZ
		if (lineCount > 0) {
			chartColumns.add(row[0]);
			for (int i = 1; i < row.length; i++) {
				Map m = new HashMap<String, Double>();
				m.put(row[0], Double.valueOf(row[i]));
				values.put(chartRows[i - 1], m);
				// chartRows[i]->String0
				// row[0]->String1;
			}
		} else {
			chartRows = new String[row.length - 1];
			for (int i = 1; i < row.length; i++) {
				chartRows[i - 1] = row[i];
			}
		}
		lineCount++;
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
		// tfpc.setCommentTags(new String[] { "Verkehrsmittel" });
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
