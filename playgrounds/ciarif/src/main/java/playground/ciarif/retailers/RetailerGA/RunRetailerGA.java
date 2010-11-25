package playground.ciarif.retailers.RetailerGA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import playground.ciarif.retailers.models.RetailerModel;
import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;

public class RunRetailerGA
{
  public static final String CONFIG_MATRICES_FOLDER = "matricesFolder";
  private int numberOfGenerations = 0;
  private int populationSize = 0;
  private static final Logger log = Logger.getLogger(GravityModelRetailerStrategy.class);

  public RunRetailerGA(Integer populationSize, Integer numberGenerations) {
    this.numberOfGenerations = numberGenerations.intValue();
    this.populationSize = populationSize.intValue();
  }

  public ArrayList<Integer> runGA(RetailerModel rm)
  {
    int genomeLength = rm.getInitialSolution().size();
    double elites = 0.1D;

    double mutants = 0.1D;

    int crossoverType = 3;
    ArrayList solutionProgress = new ArrayList(this.numberOfGenerations);

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
    }
    double avgTime = total / this.numberOfGenerations;

    String out = ga.toString();
    System.out.printf(out, new Object[0]);
    System.out.printf("\nStatistics for crossover type %d:\n", new Object[] { Integer.valueOf(crossoverType) });
    System.out.printf("\t                   Genome length:  %d\n", new Object[] { Integer.valueOf(genomeLength) });
    System.out.printf("\t                 Population size:  %d\n", new Object[] { Integer.valueOf(this.populationSize) });
    System.out.printf("\t           Number of generations:  %d\n", new Object[] { Integer.valueOf(this.numberOfGenerations) });
    System.out.printf("\t               Incumbent fitness:  %6.2f\n", new Object[] { Double.valueOf(ga.getIncumbent().getFitness()) });
    System.out.printf("\tAverage time per generation (ms):  %6.2f\n", new Object[] { Double.valueOf(avgTime) });

    ArrayList solution = ga.getIncumbent().getGenome();
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
        for (ArrayList solution : solutionProgress) {
          output.write(String.valueOf(iteration));
          output.write(",");
          output.write(String.valueOf(solution.get(0)));
          output.write(",");
          output.write(String.valueOf(solution.get(1)));
          output.write(",");
          output.write(String.valueOf(solution.get(2)));
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
