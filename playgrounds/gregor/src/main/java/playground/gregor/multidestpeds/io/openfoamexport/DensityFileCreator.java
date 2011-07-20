package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class DensityFileCreator extends FoamFileWriter{

	private final List<Double> rhos;
	private final String group;
	private final String location;

	public DensityFileCreator(String location, String group, List<Double> rhos) {
		this.location = location;
		this.group = group;
		this.rhos = rhos;
	}


	public void create() throws IOException {
		BufferedWriter bf = new BufferedWriter(new FileWriter(this.location + "/" + this.group));
		writeHeader(bf);
		writeDict(bf,"2.0","ascii", "scalarAverageField", "\"" + this.location + "\"", this.group);
		writeDensities(bf);

		bf.close();
	}


	private void writeDensities(BufferedWriter bf) throws IOException {
		bf.append("\n");
		bf.append("\n");
		bf.append(this.rhos.size() +"\n");
		bf.append("(\n");
		for (double d : this.rhos) {
			float f = (float)d;
			bf.append(" " + f);
		}
		bf.append("\n )\n");
	}

}
