package playground.clruch.io.fleet;
import static java.lang.System.out;

import java.awt.List;
import java.util.Scanner;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Objects;

public class ChangeDataFilesSF {

	public static void main(String args[]) throws IOException {

		File folder = new File("/home/anape/Documents/taxiTracesPrueba");
		File[] listOfFiles = folder.listFiles();
		System.out.println(listOfFiles.length);
		int x;
		for (File file : listOfFiles) {

			if (file.isFile()) {
				System.out.println(file.getName());
				System.out.println(file.getAbsolutePath());

				String source = File.separator + file.getAbsolutePath();
				String dest = File.separator + "/home/anape/Documents/taxiTracesPrueba (copy)/" + file.getName();

				File fin = new File(source);
				// System.out.print(fin);

				PrintWriter pw = new PrintWriter(dest);
				pw.close();

				FileInputStream fis = new FileInputStream(fin);
				BufferedReader in = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file)));

				FileWriter fstream = new FileWriter(dest, true);
				BufferedWriter out = new BufferedWriter(fstream);

				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					java.util.List<String> lista = CSVUtils.csvLineToList(aLine, " ");
					Long timeStamp = Long.parseLong(lista.get(3));
					if (timeStamp < 1211147969 && timeStamp > 1211061600) {
						out.write(aLine);
						out.newLine();
					}
				}
				in.close();
				fis.close();
				out.close();
			}
		}

	}
}