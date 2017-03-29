package playground.clruch.gfx;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.io.CsvFormat;

public class PointCloud {
    public static PointCloud fromCsvFile(File file) {
        if (file.isFile())
            try {
                return new PointCloud(CsvFormat.parse(Files.lines(Paths.get(file.toString()))));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        return null;
    }

    final Tensor tensor;

    public PointCloud(Tensor tensor) {
        // TODO
//        MatsimStaticDatabase.INSTANCE.coordinateTransformation.transform(new Coord());
        this.tensor = tensor;
    }

}
