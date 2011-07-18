package playground.mmoyo.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.log4j.Logger;

public class TextFileWriter {
	private static final Logger log = Logger.getLogger(TextFileWriter.class);
	
	public void write(final String text, final String outTxtFile, boolean append){
		try {
			FileWriter filewriter = new FileWriter(outTxtFile, append);
			BufferedWriter buffWriter = new BufferedWriter(filewriter);
			buffWriter.write(text);
			buffWriter.close();
			log.info("text file written: " + outTxtFile);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public static void main(String[] args) {
		final String text;
		final String outTxtFile;
		if (args.length>0){
			text = args[0];
			outTxtFile= args[0];
		}else{
			text = "";
			outTxtFile = "";
		}

		new TextFileWriter().write(text, outTxtFile, false);
	}

}
