/**
 * 
 */
package playground.yu.utils.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

/**
 * @author yu
 * 
 */
public class MyFilter extends TableSplitter {
	protected final BufferedWriter writer;
	public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

	/**
	 * @param regex
	 * @param tableFileName
	 * @throws IOException
	 */
	public MyFilter(String regex, String attTableFilename, String outputFilename)
			throws IOException {
		super(regex, attTableFilename);
		writer = IOUtils.getBufferedWriter(outputFilename);
	}

	protected void writeln(final String line) {
		try {
			writer.write(line + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static class SecoLine {
		private final String fromStopId, toStopId, busId;

		public SecoLine(final String fromStopId, final String toStopId,
				final String busId) {
			this.fromStopId = fromStopId;
			this.toStopId = toStopId;
			this.busId = busId;
		}

		public boolean sameSecoLine(final SecoLine sl) {
			return fromStopId.equals(sl.getFromStopId())
					&& toStopId.equals(sl.getToStopId())
					&& busId.equals(sl.getBusId());
		}

		/**
		 * @return the fromStopId
		 */
		public String getFromStopId() {
			return fromStopId;
		}

		/**
		 * @return the toStopId
		 */
		public String getToStopId() {
			return toStopId;
		}

		/**
		 * @return the busId
		 */
		public String getBusId() {
			return busId;
		}
	}

	protected static class PrimLine {
		private final String origZoneNo, destZoneNo;
		private final String travelTime;
		private final int transfers;
		/**
		 * @param secoLines
		 *            Map<arg0, arg1>
		 * @param arg0
		 *            Integer pathLegIndex
		 * @param arg1
		 *            SecoLine an object of SecoLine
		 */
		private final Map<Integer, SecoLine> secoLines = new HashMap<Integer, SecoLine>();
		/**
		 * @param initSecoLines
		 *            List<arg0>
		 * @param arg0
		 *            the secondary line in inputfile
		 */
		private final List<String> initSecoLines = new LinkedList<String>();

		public List<String> getInitSecoLines() {
			return initSecoLines;
		}

		public PrimLine(final String origZoneNo, final String destZoneNo,
				final String travelTime, final String transfers)
				throws ParseException {
			this.origZoneNo = origZoneNo;
			this.destZoneNo = destZoneNo;
			this.travelTime = travelTime;
			this.transfers = Integer.parseInt(transfers);
		}

		public String getOrigZoneNo() {
			return origZoneNo;
		}

		public String getDestZoneNo() {
			return destZoneNo;
		}

		/**
		 * @return the travelTime
		 */
		public String getTravelTime() {
			return travelTime;
		}

		/**
		 * @return the secoLines
		 */
		public Map<Integer, SecoLine> getSecoLines() {
			return secoLines;
		}

		public int getTransfers() {
			return transfers;
		}

		public boolean sameODpair(final PrimLine pl) {
			return origZoneNo.equals(pl.getOrigZoneNo())
					&& destZoneNo.equals(pl.getDestZoneNo());
		}

		public boolean samePrimLine(final PrimLine pl) {
			return origZoneNo.equals(pl.getOrigZoneNo())
					&& destZoneNo.equals(pl.getDestZoneNo())
					&& transfers == pl.getTransfers() ? true : false;
		}

		public boolean sameSecoLines(final PrimLine pl) {
			for (int i = 1; i <= transfers + 1; i++)
				if (!secoLines.isEmpty())
					if (!secoLines.get(i * 2).sameSecoLine(
							pl.getSecoLines().get(i * 2)))
						return false;
			return true;
		}

		public void addSecoLine(final String line) {
			String[] secoLineArray = line.split(";");
			String busId = secoLineArray[7].split(" ")[0];
			secoLines.put(new Integer(secoLineArray[3]), new SecoLine(
					secoLineArray[5], secoLineArray[6], busId));
		}
	}

	protected static boolean isHead(final String line) {
		return line.startsWith("$P");
	}

	static class TimeIntervalReader extends TableSplitter {
		private final List<String> timeIntervalIndexs = new ArrayList<String>();
		private final List<String> minDepTimes = new ArrayList<String>();
		private final List<String> maxDepTimes = new ArrayList<String>();
		private final String attFilepath, outputAttFilepath;
		private int cnt = 0;

		public TimeIntervalReader(final String regex,
				final String tableFileName, final String attFilepath,
				final String outputAttFilepath) throws IOException {
			super(regex, tableFileName);
			this.attFilepath = attFilepath;
			this.outputAttFilepath = outputAttFilepath;
		}

		public void makeParams(final String line) {
			if (line != null) {
				String[] params = split(line);
				timeIntervalIndexs.add(params[0]);
				minDepTimes.add(params[1]);
				maxDepTimes.add(params[2]);
				cnt++;
			}
		}

		public String getInputFilename(final int i) {
			return attFilepath + "MyFirstAttList22R24 ("
					+ timeIntervalIndexs.get(i) + ").att";
		}

		public String getOutputFilename(final int i) {
			return outputAttFilepath + "MyFirstAttList22R24 ("
					+ timeIntervalIndexs.get(i) + ").att";
		}

		public String getMinDepTime(final int i) {
			return minDepTimes.get(i);
		}

		public String getMaxDepTime(final int i) {
			return maxDepTimes.get(i);
		}

		/**
		 * @return the cnt
		 */
		public int getCnt() {
			return cnt;
		}
	}

	public static TimeIntervalReader readTimeInterval(
			final String timeIntervalFileName, final String attFilePath,
			final String outputFilePath) {
		TimeIntervalReader tir = null;
		try {
			tir = new TimeIntervalReader("\t", timeIntervalFileName,
					attFilePath, outputFilePath);
			String line = tir.readLine();
			while (line != null) {
				line = tir.readLine();
				tir.makeParams(line);
			}
			tir.closeReader();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return tir;
	}

	public void writeNewLine(final String line) {
		try {
			String[] words = split(line);
			StringBuilder word = new StringBuilder();
			word.append(words[0]);
			for (int i = 1; i < words.length; i++) {
				word.append("\t");
				word.append(words[i]);
			}
			writer.write(word + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
