package playground.southafrica.projects.digicore.grid;

import playground.southafrica.projects.digicore.grid.DigiGrid.Visual;

/**
 * Class to read a {@link DigiGrid_XYZ} from file, and visualise it.
 *
 * @author jwjoubert
 */
public class GridVisualiser_XYSpeed {

	/**
	 * Implementing the {@link DigiGrid_XYSpeed} visualisation requires the following
	 * arguments:
	 * @param args
	 * <ol>
	 * 		<li> the {@link DigiGrid_XYSpeed} file, typically called <code>cellValuesAndRiskClasses.csv</code>;
	 * 		<li> the scale of the original grid (in milli-g); and
	 * 		<li> the type of visualisation, see {@link Visual}.
	 * </ol>
	 */
	public static void main(String[] args) {
		String gridFolder = args[0];
		double scale = Double.parseDouble(args[1]);
		Visual visual = Visual.valueOf(args[2]);
		
		DigiGrid_XYSpeed grid = new DigiGrid_XYSpeed(scale);
		grid.populateFromGridFile(gridFolder + (gridFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv");
		grid.setSnapshotsFolder(gridFolder);
		grid.setVisual(visual);
		grid.visualiseGrid();
	}

}
