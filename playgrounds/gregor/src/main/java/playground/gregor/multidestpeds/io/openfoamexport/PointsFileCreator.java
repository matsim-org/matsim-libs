package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.IOException;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

public class PointsFileCreator extends FoamFileWriter {


	private final List<Coordinate> points;
	private final String location;
	private static final String OBJECT = "points";

	public PointsFileCreator(String baseDir, String location, List<Coordinate> points) {
		super(baseDir + "/" + location + "/" + OBJECT);
		this.points = points;
		this.location =location;
	}

	@Override
	public void create() throws IOException {
		writeHeader();
		writeDict("2.0", "ascii", "vectorField", "\"" + this.location + "\"", OBJECT);
		writePoints();

		super.writer.close();
	}


	private void writePoints() throws IOException {
		super.writer.append("\n");
		super.writer.append("(\n");
		for (Coordinate c : this.points){
			super.writer.append(" (" + c.x + " " + c.y + " " + c.z + ")\n");
		}
		super.writer.append(" )\n");
	}



}
