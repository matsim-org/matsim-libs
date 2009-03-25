/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.yu.utils.CollectionSum;

/**
 * @author yu
 * 
 */
public class MZ05WegeReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null, tmpPersonKantonZurichId = null;
	private int personCnt = 0, personKantonZurichCnt = 0, wCnt = 0,
			wKantonZurichCnt = 0;
	private double w_dist_obj1 = 0.0, w_dist_obj1KantonZurich = 0.0;
	private Set<Double> tmpWegeDists = new HashSet<Double>();
	private boolean changePerson = false, belongs2KantonZurich = false;

	public MZ05WegeReader(String outputFilename) {
		sw = new SimpleWriter(outputFilename);
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	private void reset(String personId) {
		if (!changePerson) {
			append();
		}
		changePerson = false;
		tmpPersonId = personId;
		tmpWegeDists.clear();
	}

	private void resetKantonZurich(String personId) {
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
		}
	}

	public void startRow(String[] row) {
		String personId = row[0] + row[1];
		if (tmpPersonId == null) {
			reset(personId);
		} else if (!tmpPersonId.equals(personId)) {
			reset(personId);
		} else {
			if (changePerson)
				return;
		}

		String w_dist_obj1 = row[42];
		if (w_dist_obj1 != null) {
			double tmpWegDist = Double.parseDouble(w_dist_obj1);
			if (tmpWegDist >= 0) {
				tmpWegeDists.add(new Double(tmpWegDist));
				if (row[9] != null) {
					if (row[9].equals("1")) {
						if (tmpPersonKantonZurichId == null) {
							resetKantonZurich(personId);
						} else if (!tmpPersonKantonZurichId.equals(personId)) {
							resetKantonZurich(personId);
						}
					} else {
						belongs2KantonZurich = false;
					}
				} else {
					belongs2KantonZurich = false;
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
		double avgW_dist_obj1 = w_dist_obj1 / (double) wCnt;
		double avgW_dist_obj1KantonZurich = w_dist_obj1KantonZurich
				/ (double) wKantonZurichCnt;
		sw.writeln("avg. w_dist_obj1\t" + avgW_dist_obj1 * 1000.0 + "\t"
				+ avgW_dist_obj1);
		sw.writeln("avg. w_dist_obj1 (KantonZurich)\t"
				+ avgW_dist_obj1KantonZurich * 1000.0 + "\t"
				+ avgW_dist_obj1KantonZurich);
		sw.writeln("\npersons :\t" + personCnt + "\tWege :\t" + wCnt);
		sw.writeln("persons KantonZurich :\t" + personKantonZurichCnt
				+ "\tWege KantonZurich :\t" + wKantonZurichCnt);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String wegeFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Wege.dat";
		String outputFilename = "../matsimTests/LinearDistance/MZ05linearDistanceWege_2.txt";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(wegeFilename);

		MZ05WegeReader mz05wr = new MZ05WegeReader(outputFilename);

		try {
			new TabularFileParser().parse(tfpc, mz05wr);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05wr.write();
	}
}
