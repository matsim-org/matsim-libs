package playground.southafrica.projects.digicore.grid;

import playground.southafrica.projects.digicore.grid.DigiGrid3D.Visual;

/**
 * Class to read a {@link DigiGrid_XYZ} from file, and visualise it.
 *
 * @author jwjoubert
 */
public class GridVisualiser_XYZ {

	/**
	 * Implementing the {@link DigiGrid_XYZ} visualisation requires the following
	 * arguments:
	 * @param args
	 * <ol>
	 * 		<li> the {@link DigiGrid_XYZ} file, typically called <code>cellValuesAndRiskClasses.csv</code>;
	 * 		<li> the scale of the original grid (in milli-g); and
	 * 		<li> the type of visualisation, see {@link Visual}.
	 * </ol>
	 */
	public static void main(String[] args) {
		String gridFolder = args[0];
		double scale = Double.parseDouble(args[1]);
		Visual visual = Visual.valueOf(args[2]);
		
		DigiGrid_XYZ grid = new DigiGrid_XYZ(scale);
		grid.populateFromGridFile(gridFolder + (gridFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv");
		grid.setSnapshotsFolder(gridFolder);
		grid.setVisual(visual);
		grid.visualiseGrid();
	}

}
