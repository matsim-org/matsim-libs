package playground.clruch.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.io.CsvFormat;

/**
 * Created by Joel on 04.04.2017.
 */
public class DiagramDemo {
    public static void main(String[] args) throws IOException {
        Tensor table = CsvFormat.parse(Files
                .lines(Paths.get("C:\\Users\\Joel\\Documents\\Studium\\ETH\\Bachelorarbeit\\Simulation_Data\\2017_03_22_Sioux_Hungarian_check1av\\output\\data\\basicdemo.csv")));
        System.out.println(Dimensions.of(table));

        table = Transpose.of(table);

        try {
            File dir = new File("C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_03_22_Sioux_Hungarian_check1av/output/data");
            // DiagramCreator.createDiagram(dir, "binnedWaitingTimes", "waiting times", table.get(0), table.extract(4,5));
            DiagramCreator.createDiagram(dir, "binnedWaitingTimes", "waiting times", table.get(0), Tensors.of(table.get(5), table.get(7), table.get(9), table.get(10)));
            // DiagramCreator.createDiagram(dir, "binnedTimeRatios", "occupancy ratio", occupancy);
        } catch (Exception e) {
            System.out.println("Error creating the diagrams");
        }
    }
}
