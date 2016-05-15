package misc;

import java.io.IOException;
import java.io.PrintWriter;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TwoLinkDataPlotter implements TabularFileHandler {

	private final String exp1fileName;

	private final String exp1lowerFileName;

	private final String exp1upperFileName;

	private final String exp2fileName;

	private final String exp2lowerFileName;

	private final String exp2upperFileName;

	private final String corrFileName;

	private PrintWriter exp1writer = null;

	private PrintWriter exp1lowerWriter = null;

	private PrintWriter exp1upperWriter = null;

	private PrintWriter exp2writer = null;

	private PrintWriter exp2lowerWriter = null;

	private PrintWriter exp2upperWriter = null;

	private PrintWriter corrWriter = null;

	TwoLinkDataPlotter(String exp1fileName, String exp1lowerFileName, String exp1upperFileName, String exp2fileName,
			String exp2lowerFileName, final String exp2upperFileName, String corrFileName) {
		this.exp1fileName = exp1fileName;
		this.exp1lowerFileName = exp1lowerFileName;
		this.exp1upperFileName = exp1upperFileName;
		this.exp2fileName = exp2fileName;
		this.exp2lowerFileName = exp2lowerFileName;
		this.exp2upperFileName = exp2upperFileName;
		this.corrFileName = corrFileName;
	}

	@Override
	public String preprocess(String line) {
		return line;
	}

	@Override
	public void startDocument() {
		try {
			this.exp1writer = new PrintWriter(this.exp1fileName);
			this.exp1lowerWriter = new PrintWriter(this.exp1lowerFileName);
			this.exp1upperWriter = new PrintWriter(this.exp1upperFileName);
			this.exp2writer = new PrintWriter(this.exp2fileName);
			this.exp2lowerWriter = new PrintWriter(this.exp2lowerFileName);
			this.exp2upperWriter = new PrintWriter(this.exp2upperFileName);
			this.corrWriter = new PrintWriter(this.corrFileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void startRow(String[] row) {

		final double time_s = Double.parseDouble(row[0]);
		final double exp1 = Double.parseDouble(row[1]);
		final double sigma1 = Math.sqrt(Double.parseDouble(row[2]));
		final double exp2 = Double.parseDouble(row[3]);
		final double sigma2 = Math.sqrt(Double.parseDouble(row[3]));
		final double corr = Double.parseDouble(row[4]) / sigma1 / sigma2;

		final String timeStr = "" + time_s;

		this.exp1writer.println("(" + timeStr + "," + exp1 + ")");
		this.exp1lowerWriter.println("(" + timeStr + "," + (exp1 - sigma1) + ")");
		this.exp1upperWriter.println("(" + timeStr + "," + (exp1 + sigma1) + ")");

		this.exp2writer.println("(" + timeStr + "," + exp2 + ")");
		this.exp2lowerWriter.println("(" + timeStr + "," + (exp2 - sigma2) + ")");
		this.exp2upperWriter.println("(" + timeStr + "," + (exp2 + sigma2) + ")");

		this.corrWriter.println("(" + timeStr + "," + corr + ")");
	}

	@Override
	public void endDocument() {
		this.exp1writer.flush();
		this.exp1writer.close();

		this.exp1lowerWriter.flush();
		this.exp1lowerWriter.close();

		this.exp1upperWriter.flush();
		this.exp1upperWriter.close();

		this.exp2writer.flush();
		this.exp2writer.close();

		this.exp2lowerWriter.flush();
		this.exp2lowerWriter.close();

		this.exp2upperWriter.flush();
		this.exp2upperWriter.close();

		this.corrWriter.flush();
		this.corrWriter.close();
	}

	public static void main(String[] args) {

		final String path = "./output/caro-queues/latex/";
		final String exp1fileName = path + "exp1.txt";
		final String exp1lowerFileName = path + "exp1lower.txt";
		final String exp1upperFileName = path + "exp1upper.txt";
		final String exp2fileName = path + "exp2.txt";
		final String exp2lowerFileName = path + "exp2lower.txt";
		final String exp2upperFileName = path + "exp2upper.txt";
		final String corrFileName = path + "corr.txt";

		final TwoLinkDataPlotter plotter = new TwoLinkDataPlotter(exp1fileName, exp1lowerFileName, exp1upperFileName,
				exp2fileName, exp2lowerFileName, exp2upperFileName, corrFileName);
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		try {
			parser.parse("./output/caro-queues/results.txt", plotter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
