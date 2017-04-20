package playground.dziemke.cemdapMatsimCadyts;

import playground.dziemke.accessibility.OTPMatrix.CSVReader;
import playground.dziemke.accessibility.ptmatrix.CSVFileWriter;
import playground.dziemke.analysis.GnuplotUtils;

import java.util.ArrayList;

/**
 * @author gthunig on 23.02.2017.
 */
public class PersonDistHistoAnalyzer {

    public static void main(String[] args) {
        analyze("withoutCalibration");
    }

    public static void analyze(String runName) {
        String analysisDir = "../../../shared-svn/projects\\cemdapMatsimCadyts\\cadyts\\equil\\output\\" + runName + "/ITERS/it.50/";
        String persoDistHosto = analysisDir + "perso-dist-histo.txt";
        String outputFile = analysisDir + "perso-dist-histo-bins.txt";
        CSVReader reader = new CSVReader(persoDistHosto, "x");
        Integer[] bins = new Integer[9];
        for (int i = 0; i < bins.length; i++) bins[i] = 0;
        ArrayList<String> file = reader.readFile();
        for (String line : file) System.out.println("line = " + line);
        for (String line : file) {
//            System.out.println("line = " + line);
            String dist = line.split("\t")[1];
//            System.out.println("dist = " + dist);
            switch (dist) {
                case "89200,00":
                    bins[0]++;
                    break;
                case "89400,00":
                    bins[1]++;
                    break;
                case "89600,00":
                    bins[2]++;
                    break;
                case "89800,00":
                    bins[3]++;
                    break;
                case "90000,00":
                    bins[4]++;
                    break;
                case "90200,00":
                    bins[5]++;
                    break;
                case "90400,00":
                    bins[6]++;
                    break;
                case "90600,00":
                    bins[7]++;
                    break;
                case "90800,00":
                    bins[8]++;
                    break;
                default:
                    break;
            }
        }
        reader.close();
        CSVFileWriter writer = new CSVFileWriter(outputFile, "  ");
        int e = 89200;
        for (Integer i : bins) {
            writer.writeField(e);
            writer.writeField(i);
            writer.writeNewLine();
            e += 200;
        }
        writer.close();

        GnuplotUtils.runGnuplotScript(analysisDir,"../../../../../../analysis/gnuplot/plot-disto-hist.gnu");
    }
}
