package playground.ciarif.retailers.RetailerGA;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;

import playground.ciarif.retailers.models.GravityModel;
import playground.jjoubert.Utilities.DateString;


public class RunRetailerGA
{
 public final static String CONFIG_GROUP = "Retailers";
 public final static String CONFIG_MATRICES_FOLDER = "matricesFolder";
 
 public ArrayList<Integer> runGA(int size, GravityModel gm)
 
  {
	ArrayList<Integer> initialSolution = new ArrayList<Integer>();
	for (int i=0; i<size;  i=i+1){
		initialSolution.add(i);
	}
    int genomeLength = initialSolution.size();
    int populationSize = 20;
    int numberOfGenerations = 40;
    double elites = 0.1;
    //default: double mutants = 0.05;
    double mutants = 0.1;

    int crossoverType = 3;
    ArrayList<ArrayList<Double>> solutionProgress = new ArrayList<ArrayList<Double>>(numberOfGenerations);

    MyFitnessFunction ff = new MyFitnessFunction(true, genomeLength, gm);
    RetailerGA ga = new RetailerGA(populationSize, genomeLength, ff, initialSolution);
    solutionProgress.add(ga.getStats());
    long tNow = 0;
    long total = 0;
    for (int i = 0; i < numberOfGenerations; ++i) {
      tNow = System.currentTimeMillis();
      ga.evolve(elites, mutants, crossoverType, ff.getPrecedenceVector());
      total += System.currentTimeMillis() - tNow;
      solutionProgress.add(ga.getStats());
    }
    double avgTime = total / numberOfGenerations;

    String out = ga.toString();
    System.out.printf(out, new Object[0]);
    System.out.printf("\nStatistics for crossover type %d:\n", new Object[] { Integer.valueOf(crossoverType) });
    System.out.printf("\t                   Genome length:  %d\n", new Object[] { Integer.valueOf(genomeLength) });
    System.out.printf("\t                 Population size:  %d\n", new Object[] { Integer.valueOf(populationSize) });
    System.out.printf("\t           Number of generations:  %d\n", new Object[] { Integer.valueOf(numberOfGenerations) });
    System.out.printf("\t               Incumbent fitness:  %6.2f\n", new Object[] { Double.valueOf(ga.getIncumbent().getFitness()) });
    System.out.printf("\tAverage time per generation (ms):  %6.2f\n", new Object[] { Double.valueOf(avgTime) });

    DateString ds = new DateString();
    
    //String fileName = "C:/Documents and Settings/ciarif/My Documents/output/triangle/GA_results/GA-Progress-" + ds.toString() + ".txt";
    //String fileName = "/scr/baug/ciarif/output/zurich_10pc/GA_results/GA-Progress-" + ds.toString() + ".txt"; //change in a way that it 
    //is taken from the config file
    //writeSolutionProgressToFile(solutionProgress, fileName);
    ArrayList<Integer> solution = ga.getIncumbent().getGenome();
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
