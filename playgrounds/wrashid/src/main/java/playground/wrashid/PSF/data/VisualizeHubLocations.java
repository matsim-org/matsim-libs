package playground.wrashid.PSF.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.lib.tools.kml.BasicPointVisualizer;
import playground.wrashid.lib.tools.kml.Color;

public class VisualizeHubLocations {

	public static void main(String[] args) {
		StringMatrix matrix=GeneralLib.readStringMatrix("A:/data/ewz daten/GIS_coordinates_of_managers.txt");
		BasicPointVisualizer visualizer=new BasicPointVisualizer();
		
		for (int i=0;i<matrix.getNumberOfRows();i++){
			Coord coord=new CoordImpl(matrix.getDouble(i, 1), matrix.getDouble(i, 2));
			visualizer.addPointCoordinate(coord, Long.toString(Math.round(matrix.getDouble(i, 0))), Color.RED);
		}
		
		visualizer.write("A:/data/ewz daten/GIS_coordinates_of_managers.kml");
	}
	
}
