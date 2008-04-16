package playground.meisterk.varia;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class VariousRuns {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		VariousRuns.standardizeLoechlsPoliceList();
	}

	private static void standardizeLoechlsPoliceList() {

		String inputFilename = "/home/meisterk/Unique_ID_Ort.txt";
		String outputFilename = "/home/meisterk/Unique_ID_Ort-standardized.txt";
		File outFile = new File(outputFilename);

		String line = null;
		String[] tokens = null;
		String outString = null;

		TreeMap<String, String> replacements = new TreeMap<String, String>();

		// numbers
		replacements.put("[0-9]", "");
		
		// special characters
		String replaceThemAll = "[";
		replaceThemAll += "()-\\[\\]/´#%$*+& ";
		//That's right: 4 backslashes to match a single one.
		replaceThemAll += "\\\\";
		replaceThemAll += "]";
		
		replacements.put(replaceThemAll, "");
		
		// umlaute
		replacements.put("ä", "ae");
		replacements.put("ö", "oe");
		replacements.put("ü", "ue");
		replacements.put("[àá]", "a");
		replacements.put("[êèëé]", "e");
		replacements.put("[îìíï]", "i");
		replacements.put("[ôóò]", "o");
		replacements.put("[ûú]", "u");
		replacements.put("[ç]", "c");

		// encoding fehler
		replacements.put(",", "e");
		replacements.put("_", "ue");
		replacements.put("\"", "o");
		replacements.put("\\^", "e");
		
		// Kantonsbezeichnungen weg
		for (String str : new String[]{
				"ag", "ar", "ai", "bl", "bs", "be", "fr", "ge", "gl", "gr", "ju", "lu", "ne", "nw", 
				"ow", "sh", "sz", "sg", "so", "ti", "tg", "ur", "vd", "vs", "zg", "zh"}
		) {
			replacements.put(" " + str, "");
		}

		LineIterator it = null;
		int lineCount = 0;
		int step = 1;

		try {
			it = FileUtils.lineIterator(new File(inputFilename), "UTF-8");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String from = null;
		String to = null;

		try {
			while (it.hasNext() && (lineCount < 1000)) {

				line = it.nextLine();
//				System.out.println(line);
				tokens = line.split(";");		
				
				if (
						(tokens.length == 5) ||
						(tokens[0].equals("\"Unique_ID\""))
						) {

					System.out.println(tokens[4]);
					tokens[4] = tokens[4].substring(1, tokens[4].length() - 1);
					System.out.println(tokens[4]);
					tokens[4] = tokens[4].toLowerCase();
					System.out.println(tokens[4]);
					System.out.println();

					Iterator<String> replacementIt = replacements.keySet().iterator();
					while(replacementIt.hasNext()) {

						from = replacementIt.next();
						to = replacements.get(from);
						tokens[4] = tokens[4].replaceAll(from, to);

					}

				}

				lineCount++;
				if (lineCount % step == 0) {
					System.out.println("line # " + lineCount);
					step *= 2;
				}

				for (String token : tokens) {
					outString += token;
					if (!token.equals(tokens[tokens.length - 1])) {
						outString += ";";
					}
				}
				outString += System.getProperty("line.separator");

				FileUtils.writeStringToFile(outFile, outString, "UTF-8");

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			System.out.println("# of lines processed: " + lineCount);
			LineIterator.closeQuietly(it);
		}

	}

}
