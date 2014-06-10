package playground.balac.retailers.RetailerGA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.balac.retailers.models.RetailerModel;


public class RunRetailerGA
{
  public static final String CONFIG_MATRICES_FOLDER = "matricesFolder";
  private int numberOfGenerations = 0;
  private int populationSize = 0;
  private static final Logger log = Logger.getLogger(RunRetailerGA.class);

  public RunRetailerGA(Integer populationSize, Integer numberGenerations) {
    this.numberOfGenerations = numberGenerations.intValue();
    this.populationSize = populationSize.intValue();
  }

  public ArrayList<Integer> runGA(RetailerModel rm)
  {
    int genomeLength = rm.getInitialSolution().size();
    double elites = 0.30D;

    double mutants = 0.05D;

    int crossoverType = 3;
    ArrayList<ArrayList<Double>> solutionProgress = new ArrayList<ArrayList<Double>>(this.numberOfGenerations);

    MyFitnessFunction ff = new MyFitnessFunction(true, genomeLength, rm);
    RetailerGA ga = new RetailerGA(this.populationSize, genomeLength, ff, rm.getInitialSolution());
    solutionProgress.add(ga.getStats());
    long tNow = 0L;
    long total = 0L;
    for (int i = 0; i < this.numberOfGenerations; ++i) {
      tNow = System.currentTimeMillis();
      ga.evolve(elites, mutants, crossoverType, ff.getPrecedenceVector());
      total += System.currentTimeMillis() - tNow;
      solutionProgress.add(ga.getStats());
      //writeSolutionProgressToFile(solutionProgress, "C:/Users/balacm/Desktop/Avignon/teleatlas_1pc_differentiated_1.0_fsf_dis_1/solutionProgress.txt");

     writeSolutionProgressToFile(solutionProgress, "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/EIRASS_2014_paper/land_price/solutionProgress.txt");

    }
    writeSolutionProgressToFile(solutionProgress, "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/EIRASS_2014_paper/land_price/solutionProgress.txt");

 //   writeSolutionProgressToFile(solutionProgress, "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/output/Avignon/out/teleatlas_retailers_50_fsf_1.0_10pc/retailers_rp3_moreLinks_test_new_fitness3/solutionProgress.txt");

    double avgTime = total / this.numberOfGenerations;

    String out = ga.toString();
    System.out.printf(out, new Object[0]);
    System.out.printf("\nStatistics for crossover type %d:\n", new Object[] { Integer.valueOf(crossoverType) });
    System.out.printf("\t                   Genome length:  %d\n", new Object[] { Integer.valueOf(genomeLength) });
    System.out.printf("\t                 Population size:  %d\n", new Object[] { Integer.valueOf(this.populationSize) });
    System.out.printf("\t           Number of generations:  %d\n", new Object[] { Integer.valueOf(this.numberOfGenerations) });
    System.out.printf("\t               Incumbent fitness:  %6.2f\n", new Object[] { Double.valueOf(ga.getIncumbent().getFitness()) });
    System.out.printf("\tAverage time per generation (ms):  %6.2f\n", new Object[] { Double.valueOf(avgTime) });

    ArrayList<Integer> solution = ga.getIncumbent().getGenome();
    log.info("The optimized solution is: " + solution);
    return solution;
  }

  private static void writeSolutionProgressToFile(ArrayList<ArrayList<Double>> solutionProgress, String fileName)
  {
    try
    {
      BufferedWriter output = new BufferedWriter(new FileWriter(new File(fileName)));
      try {
        output.write("Iteration,Best,Average,Worst");
        output.newLine();
        int iteration = 0;
        for (ArrayList<Double> solution : solutionProgress) {
          output.write(((Integer)iteration).toString());
          output.write(",");
          output.write((solution.get(0)).toString());
          output.write(",");
          output.write((solution.get(1)).toString());
          output.write(",");
          output.write((solution.get(2)).toString());
          output.newLine();
          ++iteration;
        }
      } finally {
        output.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
