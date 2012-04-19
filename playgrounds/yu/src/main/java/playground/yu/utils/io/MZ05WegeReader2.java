/* *********************************************************************** *
 * project: org.matsim.*
 * MZ05WegeReader2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.yu.utils.io;

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
	private final Map<String, Tuple<Double, Double>[]> w_dist_obj2Map = new HashMap<String, Tuple<Double, Double>[]>();
	final String outputBase;
	private String personId = "";

	// private boolean justChangedPerson = false;

	public MZ05WegeReader2(final String outputBase) {
		this.outputBase = outputBase;
		writer = new SimpleWriter(outputBase + ".txt");
		// writer.writeln("Wegezwecke\tw_dist_obj2 [km]");
	}

	@Override
	public void startRow(final String[] row) {
		int W_KANTON = Integer.parseInt(row[9]);// 1-ZH-Zuerich
		if (W_KANTON == 1) {// only for inhabitants in Kanton Zuerich
			lineCnt++;
			String tmpPersonId = row[0] + "+" + row[1];
			if (!personId.equals(tmpPersonId)) {
				personId = tmpPersonId;
				personCnt++;
				// this.justChangedPerson = true;
			} else {
				// this.justChangedPerson = false;
			}
			wmittela = Integer.parseInt(row[44]);// 1-LV, 2-MIV, 3-OeV,
			// 4-Andere
			int wzweck1 = Integer.parseInt(row[46]);
			WP = Double.parseDouble(row[2]);

			double w_dist_obj2 = Double.parseDouble(row[48]);

			if (w_dist_obj2 != -99d) {
				w_dist_obj2Sum += w_dist_obj2 * WP;
				// if (this.justChangedPerson)
				w_dist_obj2Cnt += WP;
			}

			switch (wzweck1) {
			case 2:
				handleZweck("Arbeit", w_dist_obj2);
				break;
			case 3:
				handleZweck("Ausbildung/Schule", w_dist_obj2);
				break;
			case 4:
				handleZweck("Einkauf", w_dist_obj2);
				break;
			case 5:
				handleZweck("Einkauf", w_dist_obj2);
				break;
			case 6:
				handleZweck("Geschaeftliche Taetigkeit und Dienstfahrt",
						w_dist_obj2);
				break;
			case 7:
				handleZweck("Geschaeftliche Taetigkeit und Dienstfahrt",
						w_dist_obj2);
				break;
			case 8:
				handleZweck("Freizeit", w_dist_obj2);
				break;
			case 9:
				handleZweck("Service- und Begleitwege", w_dist_obj2);
				break;
			case 10:
				handleZweck("Service- und Begleitwege", w_dist_obj2);
				break;
			case 11:
				handleZweck("Rueckkehr nach Hause bzw. auswaertige Unterkunft",
						w_dist_obj2);
				break;
			default:// 12 and -99 ...
				handleZweck("Andere", w_dist_obj2);
			}
		}

	}

	private void handleZweck(String zweck, double w_dist_obj2) {
		if (w_dist_obj2 == -99d) {
			return;
		}
		Tuple<Double, Double>[] tuple = w_dist_obj2Map.get(zweck);
		if (tuple == null) {
			tuple = new Tuple[4];
			for (int i = 0; i < tuple.length; i++) {
				tuple[i] = new Tuple<Double, Double>(0d, 0d);
			}
		}
		tuple[wmittela - 1] = new Tuple<Double, Double>(
				tuple[wmittela - 1].getFirst() + w_dist_obj2 * WP,
				tuple[wmittela - 1].getSecond() + WP);
		w_dist_obj2Map.put(zweck, tuple);
	}

	public void write() {
		writer.writeln("mittlere w_dist_obj2\t" + w_dist_obj2Sum
				/ w_dist_obj2Cnt);
		writer.writeln("w_dist_obj2Cnt\t" + w_dist_obj2Cnt);
		writer.writeln("------------------------------------------\nlineCnt\t=\t"
				+ lineCnt + "\tpersonCnt\t=\t" + personCnt);
		writer.writeln("-----------------------------------------");
		writer.writeln("wegezwecke\tmittlere w_dist_obj2(LV)\tmittlere w_dist_obj2(MIV)\tmittlere w_dist_obj2(OeV)\tmittlere w_dist_obj2(Andere)");

		double w_dist_obj2Sum[] = new double[4], w_dist_obj2Cnt[] = new double[4];
		for (Entry<String, Tuple<Double, Double>[]> entry : w_dist_obj2Map
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
		for (int i = 0; i < w_dist_obj2Sum.length; i++) {
			writer.write(w_dist_obj2Sum[i] / w_dist_obj2Cnt[i] + "\t");
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
		String outputBase = "../matsimTests/MZComp/test";
		// String outputBase = "../matsimTests/MZComp/testInland";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05WegeReader2 mz05wr = new MZ05WegeReader2(outputBase);

		new TabularFileParser().parse(tfpc, mz05wr);

		mz05wr.write();
	}
}
