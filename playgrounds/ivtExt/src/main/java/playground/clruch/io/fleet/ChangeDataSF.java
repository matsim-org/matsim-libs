package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class ChangeDataSF {

	public static void head(List<java.io.File> trailFilesComplete, java.io.File headerDirectory,
			List<java.io.File> trailFiles) throws IOException {
		for (int i = 0; i < trailFilesComplete.size(); ++i) {
			BufferedReader reader = new BufferedReader(new FileReader(headerDirectory));
			String line = null;
			StringBuilder stringBuilder = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line + " ");
			}
			String Header = stringBuilder.toString();
			BufferedReader reader1 = new BufferedReader(new FileReader(trailFilesComplete.get(i)));
			ArrayList<String> lista = new ArrayList<String>();
			String line1 = reader1.readLine();
			reader1.close();

			if (!line1.equals(Header)) {
				reader1 = new BufferedReader(new FileReader(trailFilesComplete.get(i)));
				String dataRow = reader1.readLine();
				while (dataRow != null) {
					dataRow = reader1.readLine();
					lista.add(dataRow);
				}
				FileWriter writer = new FileWriter(trailFilesComplete.get(i));
				writer.append(Header + "\n" + line1);
				for (int x = 0; x < lista.size() - 1; x++) {
					writer.append("\n" + lista.get(x));
				}
				reader1.close();
				writer.flush();
				writer.close();
				reader.close();
			}
		}
	}

	public static ArrayList<String> name(java.io.File idDirectory) throws IOException {
		Scanner id = new Scanner(idDirectory);
		ArrayList<String> people = new ArrayList<String>();
		while (id.hasNext()) {
			id.useDelimiter("\"");
			id.next();
			people.add(id.next());
			id.nextLine();
		}
		id.close();
		return people;
	}

	public static ArrayList<String> id(java.io.File idDirectory) throws IOException {
		ArrayList<String> number = new ArrayList<String>();	
		for (int i = 0; i < 536; i++)
			number.add(i, Integer.toString(i + 1));
		return number;
	}
	
}
