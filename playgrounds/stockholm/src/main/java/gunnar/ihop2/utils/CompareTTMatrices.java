package gunnar.ihop2.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatrixUtils;
import org.matsim.matrices.MatsimMatricesReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CompareTTMatrices {

	public CompareTTMatrices() {
	}

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final Matrix xMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile("./test/travelTimeMatrices_18-22.xml");
			xMatrix = m1.getMatrix("TT_18:15:00");
		}

		final Matrix yMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile("./test/travelTimeMatrices_1800-1830_parallel.xml");
			yMatrix = m1.getMatrix("TT_18:15:00");
		}

		final double frac = 0.01;
		final PrintWriter writer = new PrintWriter("./test/scatter-par.txt");

		for (List<Entry> row1 : new FractionalIterable<ArrayList<Entry>>(
				xMatrix.getFromLocations().values(), Math.sqrt(frac))) {
			for (Entry entry1 : new FractionalIterable<Entry>(row1,
					Math.sqrt(frac))) {
				final Entry entry2 = yMatrix.getEntry(entry1.getFromLocation(),
						entry1.getToLocation());
				if (entry2 != null) {
					writer.println(entry1.getValue() + "," + entry2.getValue());
				}
			}
		}
		writer.flush();
		writer.close();

		System.out.println("... DONE");
	}

}
