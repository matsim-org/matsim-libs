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
public class MZ05EtappenReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null, tmpPersonKantonZurichId = null;
	private double e_dist_obj = 0.0, e_dist_obj_KantonZurich = 0.0;
	private int eCnt = 0, eCnt_KantonZurich = 0, personCnt = 0,
			personKantonZurichCnt = 0;
	private Set<Double> tmpEtappenDists = new HashSet<Double>();
	private boolean changePerson = false, belongs2KantonZurich = false;

	/**
	 * 
	 */
	public MZ05EtappenReader(String outputFilename) {
		sw = new SimpleWriter(outputFilename);
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	private void reset(String personId) {
		if (!changePerson) {
			append();
		}
		changePerson = false;
		tmpPersonId = personId;
		tmpEtappenDists.clear();
	}

	private void resetKantonZurich(String personId) {
		belongs2KantonZurich = true;
		tmpPersonKantonZurichId = personId;
	}

	private void append() {
		personCnt++;
		eCnt += tmpEtappenDists.size();
		e_dist_obj += CollectionSum.getSum(tmpEtappenDists);
		if (belongs2KantonZurich) {
			personKantonZurichCnt++;
			eCnt_KantonZurich += tmpEtappenDists.size();
			e_dist_obj_KantonZurich += CollectionSum.getSum(tmpEtappenDists);
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

		String e_dist_obj = row[37];
		if (e_dist_obj != null) {
			double tmpEtappeDist = Double.parseDouble(e_dist_obj);
			if (tmpEtappeDist >= 0) {
				tmpEtappenDists.add(new Double(tmpEtappeDist));
				if (row[14] != null) {
					if (row[14].equals("1")) {
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
		double avgE_DIST_OBJ = e_dist_obj / (double) eCnt;
		double avgE_DIST_OBJ_KantonZurich = e_dist_obj_KantonZurich
				/ (double) eCnt_KantonZurich;
		sw.writeln("avg. E_DIST_OBJ\t" + avgE_DIST_OBJ * 1000.0 + "\t"
				+ avgE_DIST_OBJ);
		sw.writeln("avg. E_DIST_OBJ (KantonZurich)\t"
				+ avgE_DIST_OBJ_KantonZurich * 1000.0 + "\t"
				+ avgE_DIST_OBJ_KantonZurich);
		sw.writeln("\npersons :\t" + personCnt + "\tEttapen :\t" + eCnt);
		sw.writeln("persons KantonZurich :\t" + personKantonZurichCnt
				+ "\tEtappen KantonZurich :\t" + eCnt_KantonZurich);
		sw.close();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String etappenFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Etappen.dat";
		String outputFilename = "../matsimTests/LinearDistance/MZ05linearDistanceEttappen_2.txt";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(etappenFilename);

		MZ05EtappenReader mz05er = new MZ05EtappenReader(outputFilename);

		try {
			new TabularFileParser().parse(tfpc, mz05er);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05er.write();
	}
}
