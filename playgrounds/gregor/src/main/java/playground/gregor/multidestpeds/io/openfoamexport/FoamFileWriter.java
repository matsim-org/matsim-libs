package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.BufferedWriter;
import java.io.IOException;

abstract class FoamFileWriter {

	public void writeDict(BufferedWriter bf, String version, String format, String clazz,
			String location, String object) throws IOException {
		startDict(bf);
		addEntryToDict(bf,"version",version);
		addEntryToDict(bf,"format",format);
		addEntryToDict(bf,"class",clazz);
		addEntryToDict(bf,"location",location);
		addEntryToDict(bf,"object",object);
		endDict(bf);
	}

	private void addEntryToDict(BufferedWriter bf, String name, String value) throws IOException {
		bf.append("\t" + name + "\t" + value + ";\n");

	}

	private void endDict(BufferedWriter bf) throws IOException {
		bf.append("}\n");

	}

	private void startDict(BufferedWriter bf) throws IOException {
		bf.append("\n");
		bf.append("FoamFile\n");
		bf.append("{\n");
	}

	protected void writeHeader(BufferedWriter bf) throws IOException {
		bf.append("// -*- C++ -*-\n");
	}
}
