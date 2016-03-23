package playground.dziemke.examples;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

public class TryExecutingBashCommand {

	public static void main(String[] args) throws IOException, InterruptedException {
		executeCommands();
	}
	
	
	public static void executeCommands() throws IOException, InterruptedException {

	    File tempScript = createTempScript();

	    try {
	        ProcessBuilder pb = new ProcessBuilder("bash", tempScript.toString());
	        pb.inheritIO();
	        Process process = pb.start();
	        process.waitFor();
	    } finally {
	        tempScript.delete();
	    }
	}

	
	public static File createTempScript() throws IOException {
//	    File tempScript = File.createTempFile("script", null);
	    File tempScript = new File("script");

	    Writer streamWriter = new OutputStreamWriter(new FileOutputStream(
	            tempScript));
	    PrintWriter printWriter = new PrintWriter(streamWriter);

	    printWriter.println("cd ../../../runs-svn/cemdapMatsimCadyts/run_168a/analysis_300_ber_dist_5/");
	    printWriter.println("pwd");
	    printWriter.println("/usr/local/bin/gnuplot ../../../../shared-svn/projects/cemdapMatsimCadyts/analysis/plot_local.gnu");

	    printWriter.close();

	    return tempScript;
	}
}