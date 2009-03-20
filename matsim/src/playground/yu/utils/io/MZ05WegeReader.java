/**
 * 
 */
package playground.yu.utils.io;

import java.io.IOException;

import org.matsim.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

/**
 * @author yu
 * 
 */
public class MZ05WegeReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null, tmpPersonKantonZurichId = null;
	private int personCnt = 0, personKantonZurichCnt = 0, wCnt = 0,
			wKantonZurichCnt = 0;
	private double w_dist_obj2 = 0.0, w_dist_obj2KantonZurich = 0.0;

	public MZ05WegeReader(String outputFilename) {
		sw = new SimpleWriter(outputFilename);
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	public void startRow(String[] row) {
		String personId = row[0] + row[1];
		if (tmpPersonId == null) {
			tmpPersonId = personId;
			personCnt++;
		} else if (!tmpPersonId.equals(personId)) {
			tmpPersonId = personId;
			personCnt++;
		}
		String w_dist_obj2 = row[48];
		if (w_dist_obj2 != null) {
			double tmpDist = Double.parseDouble(w_dist_obj2);
			if (tmpDist > 0) {
				wCnt++;
				this.w_dist_obj2 += tmpDist;
				if (row[9] != null)
					if (row[9].equals("1")) {
						if (tmpPersonKantonZurichId == null) {
							tmpPersonKantonZurichId = personId;
							personKantonZurichCnt++;
						} else if (!tmpPersonKantonZurichId.equals(personId)) {
							tmpPersonKantonZurichId = personId;
							personKantonZurichCnt++;
						}
						wKantonZurichCnt++;
						w_dist_obj2KantonZurich += tmpDist;
					}
			}
		}
	}

	public void write() {
		double avgW_dist_obj2 = w_dist_obj2 / (double) wCnt;
		double avgW_dist_obj2KantonZurich = w_dist_obj2KantonZurich
				/ (double) wKantonZurichCnt;
		sw.writeln("avg. w_dist_obj2\t" + avgW_dist_obj2 * 1000.0 + "\t"
				+ avgW_dist_obj2);
		sw.writeln("avg. w_dist_obj2 (KantonZurich)\t"
				+ avgW_dist_obj2KantonZurich * 1000.0 + "\t"
				+ avgW_dist_obj2KantonZurich);
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
		String outputFilename = "../matsimTests/LinearDistance/MZ05linearDistanceWege.txt";

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
