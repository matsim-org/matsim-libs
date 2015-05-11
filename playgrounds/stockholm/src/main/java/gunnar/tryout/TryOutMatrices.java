package gunnar.tryout;

import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

public class TryOutMatrices {

	public static void main(String[] args) {

		Matrices matrices = new Matrices();

		{
			final Matrix m1 = matrices.createMatrix("WORK",
					"work tour costs, averaged over entire population");
			m1.createEntry("fromId1", "toId2", 12.34);
			m1.createEntry("fromId1", "toId3", 56.78);
			m1.createEntry("fromId4", "toId4", 90.12);
		}

		{
			final Matrix m2 = matrices.createMatrix("OTHER",
					"other tour costs, averaged over entire population");
			m2.createEntry("fromId10", "toId20", 11);
			m2.createEntry("fromId10", "toId30", 22);
			m2.createEntry("fromId40", "toId40", 33);
		}

		final MatricesWriter writer = new MatricesWriter(matrices);
		writer.setIndentationString("    ");
		writer.setPrettyPrint(true);
		writer.write("test.matrix");
	}

}
