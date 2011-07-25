package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.IOException;
import java.util.List;

public class DensityFileCreator extends FoamFileWriter{

	private final List<Double> rhos;
	private final String group;
	private final String location;

	public DensityFileCreator(String baseDir, String location, String group, List<Double> rhos) {
		super(baseDir + "/" + location + "/" + group);
		this.location = location;
		this.group = group;
		this.rhos = rhos;
	}


	public void create() throws IOException {
		writeHeader();
		writeDict("2.0","ascii", "scalarAverageField", "\"" + this.location + "\"", this.group);
		writeDensities();

		super.writer.close();
	}


	private void writeDensities() throws IOException {
		super.writer.append("\n");
		super.writer.append("\n");
		super.writer.append(this.rhos.size() +"\n");
		super.writer.append("(\n");
		for (double d : this.rhos) {
			float f = (float)d;
			super.writer.append(" " + f);
		}
		super.writer.append("\n )\n");
	}

}
