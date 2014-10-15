package playground.southafrica.projects.digicore.grid;

import playground.southafrica.projects.digicore.grid.DigiGrid.Visual;

/**
 * Class to read a {@link DigiGrid} from file, and visualise it.
 *
 * @author jwjoubert
 */
public class GridVisualiser {

	/**
	 * Implementing the {@link DigiGrid} visualisation requires the following
	 * arguments:
	 * @param args
	 * <ol>
	 * 		<li> the {@link DigiGrid} file, typically called <code>cellValuesAndRiskClasses.csv</code>;
	 * 		<li> the scale of the original grid (in milli-g); and
	 * 		<li> the type of visualisation, see {@link Visual}.
	 * </ol>
	 */
	public static void main(String[] args) {
		String gridFile = args[0];
		double scale = Double.parseDouble(args[1]);
		Visual visual = Visual.valueOf(args[2]);
		
		DigiGrid grid = new DigiGrid(scale);
		grid.populateFromGridFile(gridFile);
		grid.setVisual(visual);
		grid.visualiseGrid();
	}

}
