package playground.jbischoff.taxi.berlin.converters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;


public class TaxiDemandToMatrix
{

    /**
     * @param args
     */
    private Matrices matrices;
    private String folder;
    private List<File> files = new ArrayList<File>();


    public static void main(String[] args)
    {
        TaxiDemandToMatrix m = new TaxiDemandToMatrix("/Users/jb/sustainability-w-michal-and-dlr/data/taxi_berlin/2014_10_bahnstreik/OD_BRB_2014-10/nov/");
        m.read();
        m.write();
    }


    public void write()
    {
        new MatricesWriter(this.matrices).write(folder + "demandMatrices.xml");
    }


    public TaxiDemandToMatrix(String folder)
    {
        this.folder = folder;
        this.matrices = new Matrices();
    }


    public void read()
    {
        this.listFilesForFolder(new File(folder));
        for (File f : files) {

            String shortName = f.getName().substring(0, f.getName().lastIndexOf(".")).split("_")[1];

            Matrix currentMatrix = this.matrices.createMatrix(shortName, null);
            TaxiDemandParser tsp = new TaxiDemandParser(currentMatrix);
            this.read(f.getAbsolutePath(), tsp);

        }
    }


    private void listFilesForFolder(final File folder)
    {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            }
            else {
                if (!fileEntry.getName().endsWith(".dat"))
                    continue;
                System.out.println(fileEntry.getName());
                files.add(fileEntry);
            }
        }
        System.out.println(files.size());

    }


    private void read(String file, TabularFileHandler handler)
    {
        TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] { "\t", " " });
        config.setFileName(file);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, handler);
    }

}


class TaxiDemandParser
    implements TabularFileHandler
{

    Matrix matrix;


    public TaxiDemandParser(Matrix matrix)
    {
        this.matrix = matrix;
    }


    @Override
    public void startRow(String[] row)
    {
        String from = row[0];
        if (from.equals("null"))
            return;
        String to = row[1];
        if (to.equals("null"))
            return;

        double demand = Double.parseDouble(row[2]);
        System.out.println(from);
        this.matrix.createEntry(from, to, demand);

    }
}