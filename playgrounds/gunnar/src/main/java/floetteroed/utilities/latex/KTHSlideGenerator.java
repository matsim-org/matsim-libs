package floetteroed.utilities.latex;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class KTHSlideGenerator {

	// -------------------- MEMBERS --------------------

	private final List<String> lines = new ArrayList<String>();

	private String path = "testdata/latex/";

	private String templateFile = "kth-plain";

	private String contentFile = "kth-content.tex";

	// -------------------- CONSTRUCTION --------------------

	public KTHSlideGenerator() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setContentFile(final String contentFile) {
		this.contentFile = contentFile;
	}
	
	public void add(final String line) {
		this.lines.add(line);
	}

	public void run() throws FileNotFoundException, IOException {

		final PrintWriter writer = new PrintWriter(this.path + this.contentFile);
		for (String line : this.lines) {
			writer.println(line);
		}
		writer.flush();
		writer.close();

		final CompilerAndRunner runner = new CompilerAndRunner();
		runner.run(this.path, this.templateFile);

	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		final KTHSlideGenerator gen = new KTHSlideGenerator();
		gen.add("\\begin{frame}");
		gen.add("\\frametitle{Automatically generated slide Nr. 2}");
		gen.add("\\end{frame}");
		gen.run();
	}

}
