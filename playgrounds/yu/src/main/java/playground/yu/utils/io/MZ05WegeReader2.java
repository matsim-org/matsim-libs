/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * acquires the Wege/{@code Leg} distance and travel distination information
 * from MZ05
 * 
 * @author yu
 * 
 */
public class MZ05WegeReader2 implements TabularFileHandler {
	private SimpleWriter writer = null;
	private double personCnt = 0d, lineCnt = 0d, w_dist_obj2Sum = 0d,
			w_dist_obj2Cnt = 0d, WP;
	private int wmittela = 0;
	/** Map<zweck,Tuple<w_dist_obj2Summe,Cnt>[]> */
	private Map<String, Tuple<Double, Double>[]> w_dist_obj2Map = new HashMap<String, Tuple<Double, Double>[]>();
	final String outputBase;
	private String personId = "";

	// private boolean justChangedPerson = false;

	public MZ05WegeReader2(final String outputBase) {
		this.outputBase = outputBase;
		writer = new SimpleWriter(outputBase + ".txt");
		// writer.writeln("Wegezwecke\tw_dist_obj2 [km]");
	}

	public void startRow(final String[] row) {
		int W_KANTON = Integer.parseInt(row[9]);// 1-ZH-Zuerich
		if (W_KANTON == 1) {// only for inhabitants in Kanton Zuerich
			lineCnt++;
			String tmpPersonId = row[0] + "+" + row[1];
			if (!this.personId.equals(tmpPersonId)) {
				this.personId = tmpPersonId;
				this.personCnt++;
				// this.justChangedPerson = true;
			} else {
				// this.justChangedPerson = false;
			}
			wmittela = Integer.parseInt(row[44]);// 1-LV, 2-MIV, 3-OeV,
			// 4-Andere
			int wzweck1 = Integer.parseInt(row[46]);
			this.WP = Double.parseDouble(row[2]);

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
		Tuple<Double, Double>[] tuple = this.w_dist_obj2Map.get(zweck);
		if (tuple == null) {
			tuple = new Tuple[4];
			for (int i = 0; i < tuple.length; i++)
				tuple[i] = new Tuple<Double, Double>(0d, 0d);
		}
		tuple[this.wmittela - 1] = new Tuple<Double, Double>(
				tuple[this.wmittela - 1].getFirst() + w_dist_obj2 * WP,
				tuple[this.wmittela - 1].getSecond() + WP);
		this.w_dist_obj2Map.put(zweck, tuple);
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

		double w_dist_obj2Sum[] = new double[4], w_dist_obj2Cnt[] = new double[4];
		for (Entry<String, Tuple<Double, Double>[]> entry : this.w_dist_obj2Map
				.entrySet()) {
			Tuple<Double, Double>[] tuple = entry.getValue();
			StringBuffer line = new StringBuffer(entry.getKey());
			for (int i = 0; i < tuple.length; i++) {
				line.append('\t');
				double sum = tuple[i].getFirst(), cnt = tuple[i].getSecond();
				line.append(sum / cnt);
				w_dist_obj2Sum[i] += sum;
				w_dist_obj2Cnt[i] += cnt;
			}

			writer.writeln(line);
		}
		writer.write("total\t");
		for (int i = 0; i < w_dist_obj2Sum.length; i++)
			writer.write(w_dist_obj2Sum[i] / w_dist_obj2Cnt[i] + "\t");

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
		String outputBase = "../matsimTests/MZComp/test";
		// String outputBase = "../matsimTests/MZComp/testInland";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05WegeReader2 mz05wr = new MZ05WegeReader2(outputBase);

		try {
			new TabularFileParser().parse(tfpc, mz05wr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05wr.write();
	}
}
