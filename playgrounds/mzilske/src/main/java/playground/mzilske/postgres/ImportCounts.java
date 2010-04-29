package playground.mzilske.postgres;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;
import org.xml.sax.SAXException;

public class ImportCounts {
	
	private static final String COUNTS_IMPORT_FILE = "../detailedEval/net/counts.csv";
	
	private static final String COUNTS_FILE = "../detailedEval/net/counts.xml";
	
	private Counts counts = new Counts();
	
	private Counts rereadCounts = new Counts();
	
	private CountsWriter countsWriter = new CountsWriter(counts);
	
	private void parseCounts() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(COUNTS_IMPORT_FILE);
		tabFileParserConfig.setDelimiterTags(new String[] { ";" });
		new TabularFileParser().parse(tabFileParserConfig,
				new TabularFileHandler() {

					@Override
					public void startRow(String[] row) {
						String messstelle = row[0];
						String richtung = row[1];
						int h = Integer.parseInt(row[2]);
						int sum = Integer.parseInt(row[3]);
						Id linkId = new IdImpl(row[4]);
						
						Count count = counts.getCounts().get(linkId);
						if (count == null) {
							count = counts.createCount(linkId, messstelle+richtung);
						}
						count.createVolume(h, sum);
					}

				});
		counts.setYear(666);
		counts.setName("Uwe");
		counts.setLayer("7");
		countsWriter.write(COUNTS_FILE);
	}
	
	private void rereadCounts() throws SAXException, ParserConfigurationException, IOException {
		CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(rereadCounts);
		countsReader.parse(COUNTS_FILE);
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		ImportCounts importCounts = new ImportCounts();
		importCounts.parseCounts();
		importCounts.rereadCounts();
	}

}
