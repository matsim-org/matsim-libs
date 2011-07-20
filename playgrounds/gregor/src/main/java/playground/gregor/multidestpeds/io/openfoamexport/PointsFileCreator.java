package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class PointsFileCreator extends FoamFileWriter {

	private final List<Coordinate> points;
	private final String fileName;
	private final String portName;

	public PointsFileCreator(String fileName, List<Coordinate> points, String portName) {
		this.fileName = fileName;
		this.points = points;
		this.portName = portName;
	}

	public void create() throws IOException {
		BufferedWriter bf = new BufferedWriter(new FileWriter(this.fileName));
		writeHeader(bf);
		writeDict(bf,"2.0", "ascii", "vectorField", "\"const/boundaryData/"+this.portName + "\"", "points");
		writePoints(bf);

		bf.close();
	}


	private void writePoints(BufferedWriter bf) throws IOException {
		bf.append("\n");
		bf.append("(\n");
		for (Coordinate c : this.points){
			bf.append(" (" + c.x + " " + c.y + " " + c.z + ")\n");
		}


		bf.append(" )\n");
	}



}
