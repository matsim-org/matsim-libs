/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.RuntimeErrorException;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * acquires the Wege/{@code Leg} distance and travel distination information
 * from MZ05 with unusual statistic method
 * 
 * @author yu
 * 
 */
public class MZ05WegeReader2Test implements TabularFileHandler {
	private SimpleWriter writer = null;
	private double personCnt = 0d, lineCnt = 0d, w_dist_obj2Sum = 0d,
			w_dist_obj2Cnt = 0d, WP, personCntWeigted = 0d;
	private int wmittela = 0;
	/**
	 * Map<zweck,Tuple<w_dist_obj2Summe,Cnt>>
	 */
	private Map<String, double[]> w_dist_obj2Map = new HashMap<String, double[]>();
	final String outputBase;
	private String personId = "";
	private String[] rows;

	// private boolean justChangedPerson = false;

	public MZ05WegeReader2Test(final String outputBase) {
		this.outputBase = outputBase;
		writer = new SimpleWriter(outputBase + ".txt");
		// writer.writeln("Wegezwecke\tw_dist_obj2 [km]");
	}

	public void startRow(final String[] row) {
		int W_KANTON = Integer.parseInt(row[9]);// 1-ZH-Zuerich
		if (W_KANTON == 1) {// only for inhabitants in Kanton Zuerich
			this.rows = row;
			lineCnt++;
			String tmpPersonId = row[0] + "+" + row[1];
			if (!this.personId.equals(tmpPersonId)) {
				this.personId = tmpPersonId;
				this.personCnt++;
				this.WP = Double.parseDouble(row[2]);
				this.personCntWeigted += this.WP;
			}
			wmittela = Integer.parseInt(row[44]);// 1-LV, 2-MIV, 3-OeV,
			// 4-Andere
			int wzweck1 = Integer.parseInt(row[46]);

			double w_dist_obj2 = Double.parseDouble(row[48]);

			if (w_dist_obj2 != -99d) {
				this.w_dist_obj2Sum += w_dist_obj2 * WP;
				// if (this.justChangedPerson)
				this.w_dist_obj2Cnt += WP;
			}

			switch (wzweck1) {
			case 2:
				this.handleZweck("Arbeit", w_dist_obj2);
				break;
			case 3:
				this.handleZweck("Ausbildung/Schule", w_dist_obj2);
				break;
			case 4:
				this.handleZweck("Einkauf", w_dist_obj2);
				break;
			case 5:
				this.handleZweck("Einkauf", w_dist_obj2);
				break;
			case 6:
				this.handleZweck("Geschaeftliche Taetigkeit und Dienstfahrt",
						w_dist_obj2);
				break;
			case 7:
				this.handleZweck("Geschaeftliche Taetigkeit und Dienstfahrt",
						w_dist_obj2);
				break;
			case 8:
				this.handleZweck("Freizeit", w_dist_obj2);
				break;
			case 9:
				this.handleZweck("Service- und Begleitwege", w_dist_obj2);
				break;
			case 10:
				this.handleZweck("Service- und Begleitwege", w_dist_obj2);
				break;
			case 11:
				this.handleZweck(
						"Rueckkehr nach Hause bzw. auswaertige Unterkunft",
						w_dist_obj2);
				break;
			default:// 12 and -99 ...
				this.handleZweck("Andere", w_dist_obj2);
			}
		}

	}

	private void handleZweck(String zweck, double w_dist_obj2) {
		if (w_dist_obj2 == -99d)
			return;
		double[] w_dist_obj2Local = this.w_dist_obj2Map.get(zweck);
		if (w_dist_obj2Local == null) {
			w_dist_obj2Local = new double[4];
		}
		if (w_dist_obj2 < 0 || WP < 0)
			throw new RuntimeErrorException(new Error(), "dist =\t"
					+ w_dist_obj2 + " < 0 oder WP =\t" + WP + " < 0\nrow:\t"
					+ rows.toString());// TODO ...
		w_dist_obj2Local[this.wmittela - 1] += w_dist_obj2 * WP;
		this.w_dist_obj2Map.put(zweck, w_dist_obj2Local);
	}

	public void write() {
		writer.writeln("mittlere w_dist_obj2\t" + this.w_dist_obj2Sum
				/ this.w_dist_obj2Cnt);
		writer.writeln("w_dist_obj2Cnt\t" + this.w_dist_obj2Cnt);
		writer
				.writeln("------------------------------------------\nlineCnt\t=\t"
						+ this.lineCnt + "\tpersonCnt\t=\t" + this.personCnt);
		writer.writeln("-----------------------------------------");
		writer
				.writeln("wegezwecke\tmittlere w_dist_obj2(LV)\tmittlere w_dist_obj2(MIV)\tmittlere w_dist_obj2(OeV)\tmittlere w_dist_obj2(Andere)");

		for (Entry<String, double[]> entry : this.w_dist_obj2Map.entrySet()) {
			double[] w_dist_obj2local = entry.getValue();
			StringBuffer line = new StringBuffer(entry.getKey());
			for (int i = 0; i < w_dist_obj2local.length; i++) {
				line.append('\t');
				line.append(w_dist_obj2local[i] / this.personCnt);
			}
			writer.writeln(line);
		}

		writer.close();

		// PieChart chart2 = new
		// PieChart("ModalSplit Center (toll area) -- Wege");
		// // chart2.addSeries(new String[] { "MIV", "OeV", "LV", "Anders" },
		// // new double[] { wMIV_KZ, wOeV_KZ, wLV_KZ, wOthers_KZ });
		// chart2.saveAsPng(outputBase + "_Toll.png", 800, 600);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String wegeFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Wege.dat";
		// String wegeFilename =
		// "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Wegeinland.dat";
		String outputBase = "../matsimTests/MZComp/test2Test";
		// String outputBase = "../matsimTests/MZComp/testInland";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05WegeReader2Test mz05wr = new MZ05WegeReader2Test(outputBase);

		try {
			new TabularFileParser().parse(tfpc, mz05wr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05wr.write();
	}
}
