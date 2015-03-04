package playground.balac.carsharing.preprocess.membership.algos;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.balac.carsharing.preprocess.FitnessFunction;
import playground.balac.carsharing.preprocess.membership.MembershipAssigner;
import playground.balac.retailers.RetailerGA.RetailerGenome;


public class RunMembershipGA
{
  public static final String CONFIG_MATRICES_FOLDER = "matricesFolder";
  private int numberOfGenerations = 0;
  private int populationSize = 0;
  private static final Logger log = Logger.getLogger(RunMembershipGA.class);

  public RunMembershipGA(Integer populationSize, Integer numberGenerations) {
    this.numberOfGenerations = numberGenerations.intValue();
    this.populationSize = populationSize.intValue();
  }

  public ArrayList<Integer> runGA(MembershipAssigner membershipAssigner)
  {
    int genomeLength = membershipAssigner.getInitialSolution().size();
    double elites = 0.30D;

    double mutants = 0.2D;

    int crossoverType = 3;
    ArrayList solutionProgress = new ArrayList(this.numberOfGenerations);

    FitnessFunction ff = new FitnessFunction(true, genomeLength, membershipAssigner);
    MembershipGA ga = new MembershipGA(this.populationSize, genomeLength, ff, membershipAssigner.getInitialSolution());
    solutionProgress.add(ga.getStats());
    long tNow = 0L;
    long total = 0L;
     
	try {
		
		//BufferedWriter output = new BufferedWriter(new FileWriter(new File("C:/Users/balacm/Desktop/CarSharing/solutionProgress1.txt")));

		BufferedWriter output = new BufferedWriter(new FileWriter(new File("/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/balacm/MATSim/input/CarsharingStationLocations/solutionProgress_13_new.txt")));
	
		boolean ind = true;
		int i = 1;
		double max = 0.0;
		double maxA = 0.0;
		int counter = 0;
		int counterA = 0;
		double previous = 0.0;
   // for (int i = 0; i < this.numberOfGenerations; i++) {
	while(ind) {	
    	
      tNow = System.currentTimeMillis();
      ga.evolve(elites, mutants, crossoverType, ff.getPrecedenceVector());
      total += System.currentTimeMillis() - tNow;
      //solutionProgress.add(ga.getStats());
      output.newLine();
      output.write(String.valueOf(i));
      output.flush();
      
      double cur = 0.0;
      double sum  = 0.0;

      int l = 0;
      for(RetailerGenome rg:ga.getGeneration()) {  	
    	        l++;
    	       output.write("	");
    	       output.write(String.valueOf(rg.getFitness()));
    	       if (l > 75)
    	    	   sum += rg.getFitness();
    	       output.flush();
      }
		//ind = false;
		if (sum/5.0 <= previous)
			ind = false;
		previous = sum/5.0;
     /* if (cur > max) {
    	  
    	  max = cur;
    	  counter = 0;
    	  
      }
      else {
    	  if (i > 10)
    		  counter++;
    	  
      }
      output.write("	");
      output.write(String.valueOf(ga.getStats().get(1)));
      
      if (ga.getStats().get(1) > maxA) {
    	  maxA = ga.getStats().get(1);
    	  counterA = 0;
      }
      else {
    	  if (i > 10)
    		  counterA++;
      }
      
      output.flush();
      if (counter > 10 && counterA > 10)
    	  ind = false;*/
      
      i++;
      //if (i == 2) ind = false;
      
    }
  output.close();
    } catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	

    String out = ga.toString();
    System.out.printf(out, new Object[0]);
    System.out.printf("\nStatistics for crossover type %d:\n", new Object[] { Integer.valueOf(crossoverType) });
    System.out.printf("\t                   Genome length:  %d\n", new Object[] { Integer.valueOf(genomeLength) });
    System.out.printf("\t                 Population size:  %d\n", new Object[] { Integer.valueOf(this.populationSize) });
    System.out.printf("\t           Number of generations:  %d\n", new Object[] { Integer.valueOf(this.numberOfGenerations) });
    System.out.printf("\t               Incumbent fitness:  %6.2f\n", new Object[] { Double.valueOf(ga.getIncumbent().getFitness()) });

    ArrayList<Integer> solution = ga.getIncumbent().getGenome();
    log.info("The optimized solution is: " + solution);	
	
    membershipAssigner.computePotential(solution);
 return ga.getIncumbent().getGenome();
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
          iteration++;
        }
      } finally {
        output.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}