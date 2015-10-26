package gunnar.ihop2.regent;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CreateScatterPlots {

	private CreateScatterPlots() {
	}

	public static void run(final String file1, final String matrixID1,
			final String file2, final String matrixID2, final String scatterFile)
			throws FileNotFoundException {

		final Matrix xMatrix;
		{
			final Matrices m1 = new Matrices();
			final MatsimMatricesReader r1 = new MatsimMatricesReader(m1, null);
			r1.readFile(file1);
			xMatrix = m1.getMatrix(matrixID1);
		}

		final Matrix yMatrix;
		{
			final Matrices m2 = new Matrices();
			final MatsimMatricesReader r2 = new MatsimMatricesReader(m2, null);
			r2.readFile(file2);
			yMatrix = m2.getMatrix(matrixID2);
		}

		final PrintWriter writer = new PrintWriter(scatterFile);
		for (List<Entry> row1 : xMatrix.getFromLocations().values()) {
			for (Entry entry1 : row1) {
				final Entry entry2 = yMatrix.getEntry(entry1.getFromLocation(),
						entry1.getToLocation());
				if (entry2 != null) {
					writer.println(entry1.getValue() + "," + entry2.getValue());
				}
			}
		}
		writer.flush();
		writer.close();
	}

	public static void main(String[] args) throws FileNotFoundException {
//		run("./test/10percentCarNetworkPlain/travelTimeMatrices.xml",
//				"TT_06:30:00", "./test/original/EMME_traveltimes_WORK_mf8.xml",
//				"./test/original/EMME_traveltimes_WORK_mf8.txt",
//				"./test/matsim07-00_vs_EMME.csv");
//		run("./test/10percentCarNetworkPlain/tourTravelTimeMatrices.xml",
//				"WORK", "./test/original/EMME_traveltimes_WORK_mf8.xml",
//				"./test/original/EMME_traveltimes_WORK_mf8.txt",
//				"./test/matsimWORK_vs_EMME.csv");
	}

}
