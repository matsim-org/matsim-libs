package floetteroed.utilities.latex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Compiles and displays a latex file.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class CompilerAndRunner {

	// -------------------- MEMBERS --------------------

	private String latex = "latex";

	private String dvips = "dvips";

	private String ps2pdf = "ps2pdf";

	private String acroread = "\"C:\\Program Files (x86)\\Adobe\\Reader 10.0\\Reader\\AcroRd32.exe\"";
	
	// -------------------- CONSTRUCTION --------------------

	public CompilerAndRunner() {
	}

	// -------------------- SETTERS --------------------

	public void setLatex(final String latex) {
		this.latex = latex;
	}

	public void setDvips(final String dvips) {
		this.dvips = dvips;
	}

	public void setPs2pdf(final String ps2pdf) {
		this.ps2pdf = ps2pdf;
	}

	public void setAcroread(final String acroread) {
		this.acroread = acroread;
	}

	// -------------------- IMPLEMENTATION --------------------
	
	public void run(final String path, final String nameNoSuffix)
			throws IOException {

		final File script = File.createTempFile("tmp", ".bat");

		final PrintWriter writer = new PrintWriter(script);
		writer.println("CD " + path);
		writer.println(this.latex + " " + nameNoSuffix + ".tex");
		writer.println(this.dvips + " " + nameNoSuffix + ".dvi");
		writer.println(this.ps2pdf + " " + nameNoSuffix + ".ps");
		writer.println(this.acroread + " " + nameNoSuffix + ".pdf");
		writer.flush();
		writer.close();

		final String cmd = "cmd /c \"" + script + "\"";
		final Process p = Runtime.getRuntime().exec(cmd);

		final BufferedReader bri = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		final BufferedReader bre = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		String line;
		while ((line = bri.readLine()) != null) {
			System.out.println(line);
		}
		bri.close();
		while ((line = bre.readLine()) != null) {
			System.err.println(line);
		}
		bre.close();

		script.delete();
	}
	
	
	public static void main(String[] args) throws IOException {
		final CompilerAndRunner comp = new CompilerAndRunner();
		comp.run("testdata/latex", "plain");
	}
}
