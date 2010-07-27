/*
 * Created on Mar 21, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import bibtex.dom.BibtexFile;
import bibtex.expansions.CrossReferenceExpander;
import bibtex.expansions.ExpansionException;
import bibtex.expansions.MacroReferenceExpander;
import bibtex.expansions.PersonListExpander;
import bibtex.parser.BibtexParser;

/**
 * args[0] und args[1] : Input files JabRef 
 * 
 * args[2] und args[3] : Output Filepath
 * 
 * @author fuerbas
 */
public final class Main {
 

	public static void main(String[] args) {

		BibtexFile bibtexFile1 = new BibtexFile();		
		BibtexFile bibtexFile2 = new BibtexFile();
		BibtexParser parser = new BibtexParser(false);

		try {
			String filename = args[0];
			System.err.println("Parsing \"" + filename + "\" ... ");
			parser.parse(bibtexFile1, new FileReader(args[0]));
			filename = args[1];
			System.err.println("Parsing \"" + filename + "\" ... ");
			parser.parse(bibtexFile2, new FileReader(args[1]));

			Map<String, String> fillIn = new HashMap<String, String>();
			fillIn=parser.fillIn;
			BufferedReader br1 = new BufferedReader(new FileReader(args[0]));
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(new File(args[2])));

			while (br1.ready()) {
				String aLine = br1.readLine();
				if (aLine.contains("crossref")) {
					String aKey = null;
					String[] keys = aLine.split("=");
					if (keys[1].contains(",")) {
						aKey=keys[1].substring(2, keys[1].length()-2);
					}
					else 
					{
						aKey=keys[1].substring(2, keys[1].length()-1);
					}
					if (fillIn.get(aKey)!=null) {
						bw1.write("crossref = "+fillIn.get(aKey)+",");
					}
					else bw1.write(aLine);
				}  
				else if (!aLine.contains("crossref")){
					bw1.write(aLine);
					bw1.newLine();
				}
			}
			br1.close();
			bw1.close();
			
			BufferedReader br2 = new BufferedReader(new FileReader(args[1]));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(new File(args[3])));
			while (br2.ready()) {
				String aLine = br2.readLine();
				if (aLine.contains("crossref")) {
					String aKey = null;
					String[] keys = aLine.split("=");
					if (keys[1].contains(",")) {
						aKey=keys[1].substring(2, keys[1].length()-2);
					}
					else {
						aKey=keys[1].substring(2, keys[1].length()-1);
					}
					if (fillIn.get(aKey)!=null) {
						bw2.write("crossref = "+fillIn.get(aKey)+",");
					}
					else bw2.write(aLine);
				}  
				else if (!aLine.contains("crossref")){
					bw2.write(aLine);
					bw2.newLine();
				}
			}
			br2.close();
			bw2.close();
			

		} catch (Exception e) {
			System.err.println("Fatal exception: ");
			e.printStackTrace();
			return;
		} finally {
			printNonFatalExceptions(parser.getExceptions());
		}

		System.err.println("\n\nGenerating output ...");

	}

	private static void printNonFatalExceptions(Exception[] exceptions) {
		if (exceptions.length > 0) {
			System.err.println("Non-fatal exceptions: ");
			for (int i = 0; i < exceptions.length; i++) {
				exceptions[i].printStackTrace();
				System.err.println("===================");
			}
		}
	}
}
