import java.io.FileNotFoundException;
import java.io.PrintStream;

import floetteroed.utilities.latex.PSTricksDiagramWriter;

public class WriteCoords {

	public WriteCoords() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws FileNotFoundException {

		final PSTricksDiagramWriter writer = new PSTricksDiagramWriter(8, 5);
		writer.setLabelX("transitions [$10^3$]");
		writer.setLabelY("Q [$10^3$]");
		writer.setXMin(0.0);
		writer.setXMax(21.0);
		writer.setXDelta(5.0);
		writer.setYMin(200.0);
		writer.setYMax(450.0);
		writer.setYDelta(50.0);
		writer.addCommand("\\rput[rt](21,400){\\textcolor{blue}{naive}, \\textcolor{red}{new}}");
		writer.printAll(new PrintStream("./output/opdyts/coord.txt"));

	}

}
