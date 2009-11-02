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

import playground.yu.utils.CollectionSum;
import playground.yu.utils.charts.PieChart;

/**
 * @author yu
 * 
 */
public class MZ05EtappenReader implements TabularFileHandler {
	private SimpleWriter sw = null;
	private String tmpPersonId = null, tmpPersonKantonZurichId = null,
			outputBase = null;
	private double e_dist_obj = 0.0, e_dist_obj_KantonZurich = 0.0, eLV = 0.0,
			eMIV = 0.0, eOeV = 0.0, eOthers = 0.0, eLV_KZ = 0.0, eMIV_KZ = 0.0,
			eOeV_KZ = 0.0, eOthers_KZ = 0.0;
	private int eCnt = 0, eCnt_KantonZurich = 0, personCnt = 0,
			personKantonZurichCnt = 0;
	private final Set<Double> tmpEtappenDists = new HashSet<Double>();
	private double tmpELV = 0.0, tmpEMIV = 0.0, tmpEOeV = 0.0,
			tmpEOthers = 0.0;
	private boolean changePerson = false, belongs2KantonZurich = false;

	/**
	 * 
	 */
	public MZ05EtappenReader(final String outputFilename) {
		outputBase = outputFilename;
		sw = new SimpleWriter(outputFilename + ".txt");
		sw.writeln("linearDistance\tlinearDistance [m]\tlinearDistance [km]");
	}

	private void reset(final String personId) {
		if (!changePerson)
			append();
		changePerson = false;
		tmpPersonId = personId;
		tmpEtappenDists.clear();
		tmpELV = 0.0;
		tmpEMIV = 0.0;
		tmpEOeV = 0.0;
		tmpEOthers = 0.0;
	}

	private void resetKantonZurich(final String personId) {
		belongs2KantonZurich = true;
		tmpPersonKantonZurichId = personId;
	}

	private void append() {
		personCnt++;
		eCnt += tmpEtappenDists.size();
		e_dist_obj += CollectionSum.getSum(tmpEtappenDists);
		eLV += tmpELV;
		eMIV += tmpEMIV;
		eOeV += tmpEOeV;
		eOthers += tmpEOthers;
		if (belongs2KantonZurich) {
			personKantonZurichCnt++;
			eCnt_KantonZurich += tmpEtappenDists.size();
			e_dist_obj_KantonZurich += CollectionSum.getSum(tmpEtappenDists);
			eLV_KZ += tmpELV;
			eMIV_KZ += tmpEMIV;
			eOeV_KZ += tmpEOeV;
			eOthers_KZ += tmpEOthers;
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

		String e_dist_obj = row[37];
		String F510 = row[6];
		if (e_dist_obj != null) {
			double tmpEtappeDist = Double.parseDouble(e_dist_obj);
			if (tmpEtappeDist >= 0) {
				tmpEtappenDists.add(tmpEtappeDist);
				int mode = Integer.parseInt(F510);
				if (mode >= 1 && mode <= 3)
					tmpELV++;
				else if (mode >= 4 && mode <= 8)
					tmpEMIV++;
				else if (mode >= 9 && mode <= 12)
					tmpEOeV++;
				else
					tmpEOthers++;
				if (row[14] != null) {
					if (row[14].equals("1")) {
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
		double avgE_DIST_OBJ = e_dist_obj / eCnt;
		double avgE_DIST_OBJ_KantonZurich = e_dist_obj_KantonZurich
				/ eCnt_KantonZurich;
		sw.writeln("avg. E_DIST_OBJ\t" + avgE_DIST_OBJ * 1000.0 + "\t"
				+ avgE_DIST_OBJ);
		sw.writeln("avg. E_DIST_OBJ (KantonZurich)\t"
				+ avgE_DIST_OBJ_KantonZurich * 1000.0 + "\t"
				+ avgE_DIST_OBJ_KantonZurich);
		sw.writeln("\npersons :\t" + personCnt + "\tEttapen :\t" + eCnt);
		sw.writeln("persons KantonZurich :\t" + personKantonZurichCnt
				+ "\tEtappen KantonZurich :\t" + eCnt_KantonZurich);
		sw.writeln("LV etappen (walk leg):\t" + eLV
				+ "\tLV etappen Kanton Zurich (walk leg):\t" + eLV_KZ);
		sw.writeln("MIV etappen (car leg):\t" + eMIV
				+ "\tMIV etappen (car leg):\t" + eMIV_KZ);
		sw.writeln("OeV etappen (pt leg):\t" + eOeV
				+ "\tOeV etappen (pt leg):\t" + eOeV_KZ);
		sw.writeln("Others etappen (others leg):\t" + eOthers
				+ "\tOthers etappen (others leg):\t" + eOthers_KZ);
		sw.close();
		PieChart chart = new PieChart("ModalSplit -- Etappen");
		chart.addSeries(new String[] { "car", "pt", "walk", "other" },
				new double[] { eMIV, eOeV, eLV, eOthers });
		chart.saveAsPng(outputBase + ".png", 800, 600);

		PieChart chart2 = new PieChart(
				"ModalSplit Center (toll area) -- Etappen");
		chart2.addSeries(new String[] { "car", "pt", "walk", "other" },
				new double[] { eMIV_KZ, eOeV_KZ, eLV_KZ, eOthers_KZ });
		chart2.saveAsPng(outputBase + "_Toll.png", 800, 600);

	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		String etappenFilename = "D:/fromNB04/Archieve/MikroZensus2005/4_DB_ASCII(Sep_TAB)/Etappen.dat";
		String outputBase = "../matsimTests/LinearDistance/MZ05linearDistanceEttappen_ModalSplit";

		TabularFileParserConfig tfpc = new TabularFileParserConfig();
		tfpc.setCommentTags(new String[] { "HHNR" });
		tfpc.setDelimiterRegex("\t");
		tfpc.setFileName(etappenFilename);

		MZ05EtappenReader mz05er = new MZ05EtappenReader(outputBase);

		try {
			new TabularFileParser().parse(tfpc, mz05er);
		} catch (IOException e) {
			e.printStackTrace();
		}

		mz05er.write();
	}
}
