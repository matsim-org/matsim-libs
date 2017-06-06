// code by jph
package playground.clruch.gfx;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.CsvFormat;

public class PointCloud extends ArrayList<Coord> {
    public static PointCloud fromCsvFile(File file, CoordinateTransformation coordinateTransformation) {
        if (file.isFile())
            try {
                PointCloud pc = new PointCloud();
                for (Tensor row : CsvFormat.parse(Files.lines(Paths.get(file.toString()))))
                    pc.add(coordinateTransformation.transform(new Coord( //
                            row.Get(0).number().doubleValue(), //
                            row.Get(1).number().doubleValue() //
                    )));
                return pc;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        else
            System.out.println("pointcloud of virtual network not found. ignore file.");
        return null;
    }

}
