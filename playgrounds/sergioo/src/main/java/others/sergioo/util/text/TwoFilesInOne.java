package others.sergioo.util.text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

public class TwoFilesInOne {

	public static void main(String[] args) throws IOException {
		PrintWriter writer = new PrintWriter(args[2]);
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = reader.readLine();
		while(line!=null) {
			writer.println(line);
			line = reader.readLine();
		}
		reader.close();
		reader = new BufferedReader(new FileReader(args[1]));
		line = reader.readLine();
		while(line!=null) {
			writer.println(line);
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}

}
