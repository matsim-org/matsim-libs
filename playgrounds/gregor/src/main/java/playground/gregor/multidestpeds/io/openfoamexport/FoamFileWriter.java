package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

abstract class FoamFileWriter {


	protected BufferedWriter writer;

	public FoamFileWriter(String fileName) {
		try {
			this.writer = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeDict(String version, String format, String clazz,
			String location, String object) throws IOException {
		startDict();
		addEntryToDict("version",version);
		addEntryToDict("format",format);
		addEntryToDict("class",clazz);
		addEntryToDict("location",location);
		addEntryToDict("object",object);
		endDict();
	}

	abstract void create() throws IOException;

	private void addEntryToDict(String name, String value) throws IOException {
		this.writer.append("\t" + name + "\t" + value + ";\n");

	}

	private void endDict() throws IOException {
		this.writer.append("}\n");

	}

	private void startDict() throws IOException {
		this.writer.append("\n");
		this.writer.append("FoamFile\n");
		this.writer.append("{\n");
	}

	protected void writeHeader() throws IOException {
		this.writer.append("// -*- C++ -*-\n");
	}
}
