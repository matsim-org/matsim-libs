package playground.southafrica.projects.digicore.grid;

import playground.southafrica.projects.digicore.grid.DigiGrid3D.Visual;

/**
 * Class to read a {@link DigiGrid_XYSpeed} from file, and visualise it by making 
 * multiple slices in the xy-plane through the blob.
 *
 * @author jwjoubert
 */
public class GridSlicer_XYSpeed {

	/**
	 * Implementing the {@link DigiGrid_XYZ} visualisation requires the following
	 * arguments:
	 * @param args
	 * <ol>
	 * 		<li> the folder where the {@link DigiGrid_XYSpeed} file is found, typically 
	 * 		     called <code>cellValuesAndRiskClasses.csv</code>;
	 * 		<li> the scale of the original grid (in milli-g);
	 * 		<li> the minimum level where slicing will start (in milli-g);
	 * 		<li> the maximum level where slicing will end (in milli-g); and
	 * 		<li> the step-wise increment of each slice (in milli-g).
	 * </ol>
	 */
	public static void main(String[] args) {
		String gridFolder = args[0];
		double scale = Double.parseDouble(args[1]);
		double sliceStart = Double.parseDouble(args[2]);
		double sliceEnd = Double.parseDouble(args[3]);
		double sliceStep = Double.parseDouble(args[4]);
		
		DigiGrid_XYSpeed grid = new DigiGrid_XYSpeed(scale);
		grid.populateFromGridFile(gridFolder + (gridFolder.endsWith("/") ? "" : "/") + "cellValuesAndRiskClasses.csv");
		grid.setSnapshotsFolder(gridFolder);
		grid.setVisual(Visual.SLICE);
		grid.setVisualiseOnScreen(false);
		
		for(double level = sliceStart; level <= sliceEnd; level += sliceStep){
			grid.setSliceDepth(level);
			grid.visualiseGrid();
		}
	}

}
