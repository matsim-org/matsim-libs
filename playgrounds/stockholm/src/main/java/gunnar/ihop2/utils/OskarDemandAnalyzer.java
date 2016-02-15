package gunnar.ihop2.utils;

import java.io.IOException;

import floetteroed.utilities.Time;
import floetteroed.utilities.math.Histogram;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class OskarDemandAnalyzer implements TabularFileHandler {

	private double sumDurHomeGivenDur1below8h = 0.0;

	private double sumDurHomeGivenDur1above8h = 0.0;

	private double sumDurHomeGivenDur1below9h = 0.0;

	private double sumDurHomeGivenDur1above9h = 0.0;

	private int dur1below8h = 0;

	private double sumDur1below8h_min = 0.0;

	private int dur1above8h = 0;

	private double sumDur1above8h_min = 0.0;

	private int dur1below9h = 0;

	private double sumDur1below9h_min = 0.0;

	private int dur1above9h = 0;

	private double sumDur1above9h_min = 0.0;

	private double sumDurIntermedGivenDur1Below8h_min = 0.0;

	private double sumDurIntermedGivenDur1Above8h_min = 0.0;

	private double sumDur2givenDur1Below8h_min = 0.0;

	private double sumDur2givenDur1Above8h_min = 0.0;

	private double total;

	private Histogram start1 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	private Histogram end1 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	private Histogram dur1 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	private Histogram start2 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	private Histogram end2 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	private Histogram dur2 = Histogram.newHistogramWithUniformBins(0.0, 15.0,
			24 * 5);

	public OskarDemandAnalyzer(final String fileName) throws IOException {
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		parser.parse(fileName, this);
	}

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void startDocument() {
	}

	private int minFromStr(final String str) {
		if (str.length() == 2) {
			return Integer.parseInt(str);
		} else if (str.length() == 3) {
			return 60 * Integer.parseInt(str.substring(0, 1))
					+ Integer.parseInt(str.substring(1, 3));
		} else if (str.length() == 4) {
			return 60 * Integer.parseInt(str.substring(0, 2))
					+ Integer.parseInt(str.substring(2, 4));
		} else {
			throw new RuntimeException("Unknown time string: \"" + str + "\"");
		}
	}

	@Override
	public void startRow(String[] row) {
		if (row.length != 4 && row.length != 8) {
			throw new RuntimeException("row has length " + row.length);
		}
		this.total++;

		final int start1min = this.minFromStr(row[1].trim());
		final int end1min = this.minFromStr(row[2].trim());
		final int dur1min = end1min - start1min;
		this.start1.add(start1min);
		this.end1.add(end1min);
		this.dur1.add(dur1min);

		if (dur1min <= 8 * 60) {
			this.dur1below8h++;
			this.sumDur1below8h_min += dur1min;
		} else {
			this.dur1above8h++;
			this.sumDur1above8h_min += dur1min;
		}
		if (dur1min <= 9 * 60) {
			this.dur1below9h++;
			this.sumDur1below9h_min += dur1min;
		} else {
			this.dur1above9h++;
			this.sumDur1above9h_min += dur1min;
		}

		if (row.length == 4) {

			final int leaveHome_min = this.minFromStr(row[0].trim());
			final int backHome_min = this.minFromStr(row[3].trim());
			final int durHome_min = leaveHome_min
					+ Math.max(0, 24 * 60 - backHome_min);

			if (dur1min <= 8 * 60) {
				this.sumDurHomeGivenDur1below8h += durHome_min;
			} else {
				this.sumDurHomeGivenDur1above8h += durHome_min;
			}

			if (dur1min <= 9 * 60) {
				this.sumDurHomeGivenDur1below9h += durHome_min;
			} else {
				this.sumDurHomeGivenDur1above9h += durHome_min;
			}
		}

		if (row.length == 8) {

			final int leaveHome_min = this.minFromStr(row[0].trim());
			final int backHome_min = this.minFromStr(row[7].trim());
			final int durHome_min = leaveHome_min
					+ Math.max(0, 24 * 60 - backHome_min);

			if (dur1min <= 8 * 60) {
				this.sumDurHomeGivenDur1below8h += durHome_min;
			} else {
				this.sumDurHomeGivenDur1above8h += durHome_min;
			}

			if (dur1min <= 9 * 60) {
				this.sumDurHomeGivenDur1below9h += durHome_min;
			} else {
				this.sumDurHomeGivenDur1above9h += durHome_min;
			}

			final int startIntermed_min = this.minFromStr(row[3].trim());
			final int endIntermed_min = this.minFromStr(row[4].trim());
			final int durIntermed_min = endIntermed_min - startIntermed_min;

			final int start2min = this.minFromStr(row[5].trim());
			final int end2min = this.minFromStr(row[6].trim());
			final int dur2min = end2min - start2min;
			this.start2.add(start2min);
			this.end2.add(end2min);
			this.dur2.add(dur2min);

			if (dur1min <= 8 * 60) {
				this.sumDur2givenDur1Below8h_min += dur2min;
				this.sumDurIntermedGivenDur1Below8h_min += durIntermed_min;
			} else {
				this.sumDur2givenDur1Above8h_min += dur2min;
				this.sumDurIntermedGivenDur1Above8h_min += durIntermed_min;
			}
		}
	}

	@Override
	public void endDocument() {
	}

	static String strFromMin(final double min) {
		return Time.strFromSec((int) (min * 60));
	}

	public static void main(String[] args) throws IOException {

		// final String filename = "./ihop2-data/demand-input/hwh-2.csv";
		final String filename = "./ihop2-data/demand-input/hwhoh-2.csv";

		final OskarDemandAnalyzer oda = new OskarDemandAnalyzer(filename);

		System.out.println(filename);
		System.out.println();

		System.out.print("P(dur1 <= 8h) = " + (oda.dur1below8h / oda.total));
		System.out.println("\tE(dur1 | <= 8h) = "
				+ strFromMin(oda.sumDur1below8h_min / oda.dur1below8h));

		System.out.print("P(dur1 >  8h) = " + (oda.dur1above8h / oda.total));
		System.out.println("\tE(dur1 | > 8h) = "
				+ strFromMin(oda.sumDur1above8h_min / oda.dur1above8h));

		System.out.println();

		System.out.print("P(dur1 <= 9h) = " + (oda.dur1below9h / oda.total));
		System.out.println("\tE(dur1 | <= 9h) = "
				+ strFromMin(oda.sumDur1below9h_min / oda.dur1below9h));

		System.out.print("P(dur1 >  9h) = " + (oda.dur1above9h / oda.total));
		System.out.println("\tE(dur1 | > 9h) = "
				+ strFromMin(oda.sumDur1above9h_min / oda.dur1above9h));
		System.out.println();

		System.out.println("E(dur2 | dur1 <= 8h) = "
				+ strFromMin(oda.sumDur2givenDur1Below8h_min / oda.dur1below8h));
		System.out.println("E(dur2 | dur1 > 8h) = "
				+ strFromMin(oda.sumDur2givenDur1Above8h_min / oda.dur1above8h));
		System.out.println();

		System.out.println("E(durIntermed | dur1 <= 8h) = "
				+ strFromMin(oda.sumDurIntermedGivenDur1Below8h_min / oda.dur1below8h));
		System.out.println("E(durIntermed | dur1 > 8h) = "
				+ strFromMin(oda.sumDurIntermedGivenDur1Above8h_min / oda.dur1above8h));
		System.out.println();
		
		System.out.println("E(home | dur1 <= 8h) = "
				+ strFromMin(oda.sumDurHomeGivenDur1below8h / oda.dur1below8h));
		System.out.println("E(home | dur1 > 8h) = "
				+ strFromMin(oda.sumDurHomeGivenDur1above8h / oda.dur1above8h));
		System.out.println();
		
		System.out.println("E(home | dur1 <= 9h) = "
				+ strFromMin(oda.sumDurHomeGivenDur1below9h / oda.dur1below9h));
		System.out.println("E(home | dur1 > 9h) = "
				+ strFromMin(oda.sumDurHomeGivenDur1above9h / oda.dur1above9h));
		System.out.println();
		
		// System.out
		// .println("bin\tendtime\tstart1\tdur1\tend1\tstart2\tdur2\tend2");
		// for (int i = 0; i < oda.start1.binCnt(); i++) {
		// System.out.println(i + "\t" + i * 15 + "\t" + oda.start1.cnt(i)
		// + "\t" + oda.dur1.cnt(i) + "\t" + oda.end1.cnt(i) + "\t"
		// + oda.start2.cnt(i) + "\t" + oda.dur2.cnt(i) + "\t"
		// + oda.end2.cnt(i));
		// }
	}
}
