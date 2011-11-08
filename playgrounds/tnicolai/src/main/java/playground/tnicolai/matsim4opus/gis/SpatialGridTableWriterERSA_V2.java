package playground.tnicolai.matsim4opus.gis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.contrib.matsim4opus.gis.SpatialGrid;

import playground.tnicolai.matsim4opus.utils.helperObjects.SquareLayer;

public class SpatialGridTableWriterERSA_V2 {


	public void write(SpatialGrid<SquareLayer> grid, String fileName, String fileType) throws IOException {
		
		BufferedWriter layer1 = new BufferedWriter(new FileWriter(fileName + "_Centroid" + fileType));
		BufferedWriter layer2 = new BufferedWriter(new FileWriter(fileName + "_Interpolated" + fileType));
		BufferedWriter layer3 = new BufferedWriter(new FileWriter(fileName + "_Derivation" + fileType));
		
		for(int j = 0; j < grid.getNumCols(0); j++) {
			layer1.write("\t");
			layer1.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
			layer2.write("\t");
			layer2.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
			layer3.write("\t");
			layer3.write(String.valueOf(grid.getXmin() + j * grid.getResolution()));
		}
		layer1.newLine();
		layer2.newLine();
		layer3.newLine();
		
		for(int i = grid.getNumRows() - 1; i >=0 ; i--) {
			layer1.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			layer2.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			layer3.write(String.valueOf(grid.getYmax() - i * grid.getResolution()));
			
			for(int j = 0; j < grid.getNumCols(i); j++) {
				layer1.write("\t");
				Double centroid = grid.getValue(i, j).getCentroidAccessibility();
				if(centroid != null)
					layer1.write(String.valueOf(centroid));
				else
					layer1.write("NA");
				
				layer2.write("\t");
				Double interpolation = grid.getValue(i, j).getInterpolationAccessibility();
				if(interpolation != null)
					layer2.write(String.valueOf(interpolation));
				else
					layer2.write("NA");
								
				layer3.write("\t");
				Double derivation = grid.getValue(i, j).getAccessibilityDerivation();
				if(derivation != null)
					layer3.write(String.valueOf(derivation));
				else
					layer3.write("NA");
			}
			layer1.newLine();
			layer2.newLine();
			layer3.newLine();
		}
		layer1.flush();
		layer1.close();
		layer2.flush();
		layer2.close();
		layer3.flush();
		layer3.close();
	}
}
