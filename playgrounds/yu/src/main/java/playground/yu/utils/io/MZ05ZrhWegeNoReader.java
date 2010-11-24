/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.yu.utils.charts.PieChart;

/**
 * @author yu
 * 
 */
public class MZ05ZrhWegeNoReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null;
	private Double tmpPersonWeight = null;
	final String outputBase;
	private double personCnt = 0d;
	// private double personWeight = 0d;
	private double MIV_wegNo = 0.0, OeV_wegNo = 0.0, LV_wegNo = 0.0,
			others_wegNo = 0.0;
	private int tmpLV_wegNo = 0, tmpMIV_wegNo = 0, tmpOeV_wegNo = 0,
			tmpOthers_wegNo = 0;
	private boolean changePerson = false;

	public MZ05ZrhWegeNoReader(final String outputBase) {
		this.outputBase = outputBase;
		sw = new SimpleWriter(outputBase + "txt");
		sw
				.writeln("Hauptverkehrsmittel aggregiert\tavg. No. of Hauptverkehrsmittel aggregiert von Wegen");
	}

	private void reset(final String personId, Double personWeight) {
		if (!changePerson) {
			append();
		}
		changePerson = false;
		tmpPersonId = personId;
		this.tmpPersonWeight = personWeight;
		tmpLV_wegNo = 0;
		tmpMIV_wegNo = 0;
		tmpOeV_wegNo = 0;
		tmpOthers_wegNo = 0;
	}

	private void append() {
		personCnt += tmpPersonWeight;
		LV_wegNo += tmpLV_wegNo * tmpPersonWeight;
		MIV_wegNo += tmpMIV_wegNo * tmpPersonWeight;
		OeV_wegNo += tmpOeV_wegNo * tmpPersonWeight;
		others_wegNo += tmpOthers_wegNo * tmpPersonWeight;

	}

	public void startRow(final String[] row) {
		String personId = row[0] + row[1];
		String personWeight = row[2];
		String kantonNo = row[9];
		if (kantonNo != null) {
			if (kantonNo.equals("1")) {
				if (tmpPersonId == null || tmpPersonWeight == null) {
					reset(personId, Double.valueOf(personWeight));
				} else if (!tmpPersonId.equals(personId)) {// new person
					reset(personId, Double.valueOf(personWeight));
				} else if (changePerson) {// same person
					return;
				}

				String wmittela = row[44];
				if (wmittela != null) {
					int tmpAggrMode = Integer.parseInt(wmittela);// 1,2,3,4
					if (tmpAggrMode > 0) {
						// int mode = Integer.parseInt(row[44]);
						switch (tmpAggrMode) {
						case 1:
							tmpLV_wegNo++;
							break;
						case 2:
							tmpMIV_wegNo++;
							break;
						case 3:
							tmpOeV_wegNo++;
							break;
						case 4:
							tmpOthers_wegNo++;
							break;
						}

					} else {
						changePerson = true;
						return;
					}
				} else {
					changePerson = true;
					return;
				}
			} else {
				changePerson = true;
				return;
			}
		} else {
			changePerson = true;
			return;
		}
	}

	public void write() {
		if (!changePerson) {
			append();
		}

		sw.writeln("avg. No. of Wege LV Kanton Zurich :\t" + LV_wegNo
				/ this.personCnt);
		sw.writeln("avg. No. of Wege MIV Kanton Zurich :\t" + MIV_wegNo
				/ this.personCnt);
		sw.writeln("avg. No. of Wege OeV Kanton Zurich :\t" + OeV_wegNo
				/ this.personCnt);
		sw.writeln("avg. No. of Wege Others Kanton Zurich :\t" + others_wegNo
				/ this.personCnt);

		sw.writeln("\nNo. of persons :\t" + personCnt + "\tNo. of Wege :\t"
				+ (LV_wegNo + MIV_wegNo + OeV_wegNo + others_wegNo));

		sw.close();

		PieChart chart2 = new PieChart(
				"ModalSplit (Kanton Zurich)-- No. of Wege");
		chart2.addSeries(new String[] { "MIV", "OeV", "LV", "others" },
				new double[] { MIV_wegNo, OeV_wegNo, LV_wegNo, others_wegNo });
		chart2.saveAsPng(outputBase + "png", 800, 600);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String wegeFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Wege.dat";
		String outputBase = "../matsimTests/MZComp/MZ05KantonWegeNo.";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05ZrhWegeNoReader mz05wr = new MZ05ZrhWegeNoReader(outputBase);

		try {
			new TabularFileParser().parse(tfpc, mz05wr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05wr.write();
	}
}
