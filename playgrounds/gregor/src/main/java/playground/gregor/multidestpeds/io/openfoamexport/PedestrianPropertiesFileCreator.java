package playground.gregor.multidestpeds.io.openfoamexport;

import java.io.IOException;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.multidestpeds.io.openfoamexport.PedestrianGroup.Destination;

public class PedestrianPropertiesFileCreator extends FoamFileWriter{

	private static final String OBJECT = "pedestrianProperties";
	private final String location;
	private final List<PedestrianGroup> groups;

	private static final String indent = "  ";
	private int numIndent = 0;

	public PedestrianPropertiesFileCreator(String baseDir, String location, List<PedestrianGroup> groups) {
		super(baseDir + "/" + location + "/" + OBJECT);
		this.location = location;
		this.groups = groups;

	}

	@Override
	void create() throws IOException {
		writeHeader();
		writeDict("2.0","ascii", "dictionary", "\"" + this.location + "\"", OBJECT);
		startPeds();
		for (PedestrianGroup group : this.groups) {
			startGroup(group.getName());
			writeFreespeed(group.getFreespeed());
			appendFreeString("b    b       [0 0 0 0 0 0 0] .05;\n\n");
			appendFreeString("normailzedGradTerm false;\n\n");
			startGroup("potentialControls");
			startGroup("boundaryField");
			writeBoundaryField("default","fixedValue",100);
			for ( Destination d : group.getDestinations()) {
				writeBoundaryField(d.name,"fixedValue", d.potential);
			}
			endGroup();//boundaryField
			startGroup("source");
			writeBoundaryField("default","fixedValue",0);
			super.writer.append("\n");
			appendFreeString("internalField\tuniform 0;\n");
			super.writer.append("\n");
			appendFreeString("defaultPedestrianAttraction -0.0;\n");

			startGroup("pedestrianAttractions");
			appendFreeString(group.getName() + " 100;\n");
			endGroup();//pedestrianAttractions

			appendFreeString("jamAttraction -100;\n");
			endGroup();//source
			endGroup();//potential

			startGroup("directionControls");
			startGroup("boundaryField");
			writeBoundaryPotentialDirection("default",new Coordinate(0,0,0));
			writeBoundaryPotentialDirection(group.getOrigin(), group.getOriginDirection());
			for (Destination d : group.getDestinations()) {
				writeBoundaryPotentialDirection(d.name, d.direction);
			}
			endGroup();//boundaryField
			endGroup();//directionsControls

			startGroup("rhoControls");
			startGroup("boundaryField");
			startGroup("default");
			appendFreeString("type\tzeroGradient;\n");
			endGroup();//default
			for (Destination  d: group.getDestinations()) {
				writeBoundaryField(d.name,"calculated",0);
			}
			endGroup();//boundaryField
			endGroup();//rhoControls

			appendFreeString("VProfiler linear;\n");
			endGroup();//ped
		}

		endPeds();

		appendFreeString("VMin    VMin    [0 1 -1 0 0 0 0] 0;\n\n");
		appendFreeString("VMax    VMax    [0 1 -1 0 0 0 0] 1;\n");
		super.writer.close();

	}

	private void writeBoundaryPotentialDirection(String name,
			Coordinate direction) throws IOException {
		startGroup(name);
		appendFreeString("type\tfixedValue;\n");
		appendFreeString("value\tuniform (" + (float)direction.x + " " + (float)direction.y + " " + (float)direction.z + ");\n");
		endGroup();

	}

	private void writeBoundaryField(String name, String valType, double val) throws IOException {
		startGroup(name);
		appendFreeString("type\t" + valType + ";\n");
		appendFreeString("value\tuniform " + (float)val +";\n");
		endGroup();

	}

	private void writeFreespeed(double freespeed) throws IOException {
		indent();
		super.writer.append("a    a       [0 -1 0 0 0 0 0] ");
		super.writer.append(((float)freespeed) + ";\n");

	}

	private void indent() throws IOException {
		for (int i = 0; i < this.numIndent; i++) {
			super.writer.append(this.indent);
		}
	}

	private void appendFreeString(String string) throws IOException {
		indent();
		super.writer.append(string);

	}

	private void endGroup() throws IOException {
		this.numIndent--;
		indent();
		super.writer.append("}\n\n");
	}

	private void startGroup(String name) throws IOException {
		indent();
		super.writer.append(name + "\n");
		this.numIndent++;
		indent();
		super.writer.append("{\n");
	}

	private void endPeds() throws IOException {
		this.numIndent--;
		indent();
		super.writer.append(");\n\n");
	}

	private void startPeds() throws IOException {
		indent();
		super.writer.append("\n");
		indent();
		super.writer.append("pedestrians\n");
		super.writer.append("(\n");
		this.numIndent++;

	}

}
