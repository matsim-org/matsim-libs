/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.yu.utils.charts.PieChart;
import playground.yu.utils.container.CollectionSum;

/**
 * @author yu
 * 
 */
public class MZ05WegeReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null, tmpPersonKantonZurichId = null;
	final String outputBase;
	private int personCnt = 0, personKantonZurichCnt = 0, wCnt = 0,
			wKantonZurichCnt = 0;
	private double w_dist_obj1 = 0.0, w_dist_obj1KantonZurich = 0.0,
			wMIV_KZ = 0.0, wOeV_KZ = 0.0, wLV_KZ = 0.0, wOthers_KZ = 0.0;
	private double tmpWLV = 0.0, tmpWMIV = 0.0, tmpWOeV = 0.0,
			tmpWOthers = 0.0;
	private final Set<Double> tmpWegeDists = new HashSet<Double>();
	private boolean changePerson = false, belongs2KantonZurich = false;

	public MZ05WegeReader(final String outputBase) {
		this.outputBase = outputBase;
		sw = new SimpleWriter(outputBase + ".txt");
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	private void reset(final String personId) {
		if (!changePerson)
			append();
		changePerson = false;
		tmpPersonId = personId;
		tmpWegeDists.clear();
		tmpWLV = 0.0;
		tmpWMIV = 0.0;
		tmpWOeV = 0.0;
		tmpWOthers = 0.0;
	}

	private void resetKantonZurich(final String personId) {
		belongs2KantonZurich = true;
		tmpPersonKantonZurichId = personId;
	}

	private void append() {
		personCnt++;
		wCnt += tmpWegeDists.size();
		w_dist_obj1 += CollectionSum.getSum(tmpWegeDists);
		if (belongs2KantonZurich) {
			personKantonZurichCnt++;
			wKantonZurichCnt += tmpWegeDists.size();
			w_dist_obj1KantonZurich += CollectionSum.getSum(tmpWegeDists);
			wLV_KZ += tmpWLV;
			wMIV_KZ += tmpWMIV;
			wOeV_KZ += tmpWOeV;
			wOthers_KZ += tmpWOthers;
		}
	}

	public void startRow(final String[] row) {
		String personId = row[0] + row[1];
		if (tmpPersonId == null)
			reset(personId);
		else if (!tmpPersonId.equals(personId))
			reset(personId);
		else if (changePerson)
			return;

		String w_dist_obj1 = row[42];
		if (w_dist_obj1 != null) {
			double tmpWegDist = Double.parseDouble(w_dist_obj1);
			if (tmpWegDist >= 0) {
				tmpWegeDists.add(tmpWegDist);
				int mode = Integer.parseInt(row[44]);
				// switch (mode) {
				// case 1:
				// tmpWLV++;
				// break;
				// case 2:
				// tmpWMIV++;
				// break;
				// case 3:
				// tmpWOeV++;
				// break;
				// case 4:
				// tmpWOthers++;
				// break;
				// }
				if (mode == 1)
					tmpWLV++;
				else if (mode == 2)
					tmpWMIV++;
				else if (mode == 3)
					tmpWOeV++;
				else
					tmpWOthers++;
				if (row[9] != null) {
					if (row[9].equals("1")) {
						if (tmpPersonKantonZurichId == null)
							resetKantonZurich(personId);
						else if (!tmpPersonKantonZurichId.equals(personId))
							resetKantonZurich(personId);
					} else
						belongs2KantonZurich = false;
				} else
					belongs2KantonZurich = false;
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
		if (!changePerson)
			append();
		double avgW_dist_obj1 = w_dist_obj1 / wCnt;
		double avgW_dist_obj1KantonZurich = w_dist_obj1KantonZurich
				/ wKantonZurichCnt;
		sw.writeln("avg. w_dist_obj1\t" + avgW_dist_obj1 * 1000.0 + "\t"
				+ avgW_dist_obj1);
		sw.writeln("avg. w_dist_obj1 (KantonZurich)\t"
				+ avgW_dist_obj1KantonZurich * 1000.0 + "\t"
				+ avgW_dist_obj1KantonZurich);
		sw.writeln("\npersons :\t" + personCnt + "\tWege :\t" + wCnt);
		sw.writeln("persons KantonZurich :\t" + personKantonZurichCnt
				+ "\tWege KantonZurich :\t" + wKantonZurichCnt);
		sw.writeln("\nWege LV Kanton Zurich :\t" + wLV_KZ);
		sw.writeln("Wege MIV Kanton Zurich :\t" + wMIV_KZ);
		sw.writeln("Wege OeV Kanton Zurich :\t" + wOeV_KZ);
		sw.writeln("Wege Others Kanton Zurich :\t" + wOthers_KZ);

		sw.close();

		PieChart chart2 = new PieChart("ModalSplit Center (toll area) -- Wege");
		chart2.addSeries(new String[] { "MIV", "OeV", "LV", "Anders" },
				new double[] { wMIV_KZ, wOeV_KZ, wLV_KZ, wOthers_KZ });
		chart2.saveAsPng(outputBase + "_Toll.png", 800, 600);
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String wegeFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Wege.dat";
		String outputBase = "../matsimTests/LinearDistance/MZ05linearDistanceWege_2";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05WegeReader mz05wr = new MZ05WegeReader(outputBase);

		try {
			new TabularFileParser().parse(tfpc, mz05wr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05wr.write();
	}
}
