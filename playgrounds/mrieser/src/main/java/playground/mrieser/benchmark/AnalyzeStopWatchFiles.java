package playground.mrieser.benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;

public class AnalyzeStopWatchFiles {

	private final static Logger log = Logger.getLogger(AnalyzeStopWatchFiles.class);

	public static void analyzeDirectory(final String directoryName) throws FileNotFoundException, IOException {
		File dir = new File(directoryName);
		if (!dir.isDirectory()) {
			throw new IOException("Not a directory: " + directoryName);
		}
		for (String name : dir.list()) {
			File file = new File(directoryName + name);
			if (file.isFile()) {
				analyzeOneFile(directoryName + name);
			}
		}
	}

	public static void analyzeOneFile(final String filename) throws FileNotFoundException, IOException {
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		String header = reader.readLine();
		if (header == null) {
			log.warn("missing header in " + filename);
			return;
		}
		String[] parts = StringUtils.explode(header, '\t');
		if ("replanning".equals(parts[10]) && "dump all plans".equals(parts[11]) && "mobsim".equals(parts[12]) && "iteration".equals(parts[13])) {
			String line = null;
			double replanning = 0;
			double dump = 0;
			double mobsim = 0;
			double iteration = 0;
			double nOfLines = 0;
			double nOfDumps = 0;
			while ((line = reader.readLine()) != null) {
				nOfLines++;
				parts = StringUtils.explode(line, '\t');
				if (parts[10].length() > 0) {
					replanning += Time.parseTime(parts[10]);
				}
				if (parts[11].length() > 0) {
					dump += Time.parseTime(parts[11]);
					nOfDumps++;
				}
				mobsim+= Time.parseTime(parts[12]);
				iteration += Time.parseTime(parts[13]);
			}
			System.out.println(filename + '\t' + (replanning / nOfLines) + '\t' + (dump / nOfLines) + '\t' + (mobsim / nOfLines) + '\t' + (iteration / nOfLines));
		} else {
			log.warn("wrong header in " + filename);
			log.warn("header: " + header);
		}
	}

	public static void main(String[] args) throws IOException {
		analyzeDirectory("/Users/mrieser/Desktop/stats/");
	}
}
