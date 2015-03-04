package playground.balac.carsharing.preprocess.membership.algos;

import java.util.ArrayList;
import java.util.Collections;

import playground.balac.carsharing.preprocess.FitnessFunction;
import playground.balac.carsharing.preprocess.membership.CumulativeDistribution;
import playground.balac.retailers.RetailerGA.MyPermutator;
import playground.balac.retailers.RetailerGA.RetailerGenome;


public class MembershipGA
{
  private final int populationSize;
  private int genomeLength;
  private ArrayList<RetailerGenome> generation;
  private MyPermutator permutator = new MyPermutator();
  private final FitnessFunction fitnessFunction;
  private final RetailerGenome initialSolution;
  private RetailerGenome incumbent;
  private CumulativeDistribution cdf;
  private double best;
  private double average;
  private double worst;

  public MembershipGA(int populationSize, int genomeLength, FitnessFunction ff, ArrayList<Integer> initialSolution)
  {
    this.populationSize = populationSize;
    this.genomeLength = genomeLength;
    this.fitnessFunction = ff;
    double firstFitness = ff.evaluate(initialSolution).doubleValue();
    this.initialSolution = 
      new RetailerGenome(firstFitness, 
      this.fitnessFunction.isMax(), 
      initialSolution);
    this.incumbent = this.initialSolution;
    this.generation = new ArrayList<RetailerGenome>(populationSize);
    generateFirstGeneration();
  }

  public void generateFirstGeneration() {
    if (this.generation.size() > 0) {
      System.err.println("Trying to overwrite an existing generation!!");
      System.exit(0);
    } else {
      this.generation.add(this.initialSolution);

      for (int i = 0; i < this.populationSize - 1; i++)
      {
        ArrayList<Integer> newSolution = this.initialSolution.clone();
        for (int j = 0; j < 10; j++) {
          newSolution = mutate(newSolution);
        }
        double newSolutionFitness = this.fitnessFunction.evaluate(newSolution);
        RetailerGenome newGenome = new RetailerGenome(newSolutionFitness, 
          this.fitnessFunction.isMax(), 
          newSolution);
        this.generation.add(newGenome);

        checkIncumbent(newGenome);
      }
    }
    Collections.sort(this.generation);
    if (this.fitnessFunction.isMax()) {
      setBest((-1.0D / 0.0D));
      setAverage((-1.0D / 0.0D));
      setWorst((-1.0D / 0.0D));
    } else {
      setBest((1.0D / 0.0D));
      setAverage((1.0D / 0.0D));
      setWorst((1.0D / 0.0D));
    }
    calculateStats();
    buildCDF();
  }

  
  //when mutating we need to ensure that at least one position is from the first *numberofstations* positions
  
  public ArrayList<Integer> mutate(ArrayList<Integer> genome)
  {
    ArrayList<Integer> result = new ArrayList<Integer>(this.genomeLength);
    for (Integer integer : genome) {
      int i = Integer.valueOf(integer.intValue()).intValue();
      result.add(Integer.valueOf(i));
    }

    ArrayList<Integer> strip = this.permutator.permutate(this.genomeLength);
    int pos1 = strip.get(0) - 1;
    int pos2 = strip.get(1) - 1;
    
    while (pos1 > 376 && pos2 > 376) {
    	strip = this.permutator.permutate(this.genomeLength);
        pos1 = strip.get(0) - 1;
        pos2 = strip.get(1) - 1;
    }
    
    Collections.swap(result, pos1, pos2);

    return result;
  }

  public int getPopulationSize() {
    return this.populationSize;
  }

  private void checkIncumbent(RetailerGenome genome)
  {
    if (this.fitnessFunction.isMax()) {
      if (genome.getFitness() > this.incumbent.getFitness()) {
        this.incumbent = genome;
      }
    }
    else if (genome.getFitness() < this.incumbent.getFitness())
      this.incumbent = genome;
  }

  private void checkDiversity()
  {
    ArrayList<RetailerGenome> incumbentList = new ArrayList<RetailerGenome>();
    RetailerGenome thisGenome = null;

    int pos = 0;
    boolean end = false;
    while ((!end) && (pos < this.populationSize)) {
      thisGenome = (RetailerGenome)this.generation.get(pos);
      if (thisGenome.getFitness() == this.incumbent.getFitness()) {
        incumbentList.add(thisGenome);
        pos++;
      } else {
        end = true;
      }
    }
    if (incumbentList.size() / this.populationSize > 0.2D) {
      for (int i = 0; i < incumbentList.size() * 0.2D; i++) {
        thisGenome = (RetailerGenome)incumbentList.get(i);
        if (thisGenome != this.incumbent) {
          ArrayList<Integer> newGenome = mutate(thisGenome.getGenome());
          for (int j = 0; j < this.genomeLength; j++) {
            thisGenome.getGenome().set(j, (Integer)newGenome.get(j));
          }
        }
      }
      Collections.sort(this.generation);
    }
  }

  private void buildCDF()
  {
    double[] x = new double[this.populationSize + 1];
    double[] y = new double[this.populationSize + 1];
    x[0] = -0.5D;
    y[0] = 0.0D;
    x[this.populationSize] = (this.populationSize - 1);
    y[this.populationSize] = 1.0D;

    ArrayList<Double> copy = new ArrayList<Double>(this.populationSize);
    if (this.fitnessFunction.isMax()) {
      double minValue = 0.95D * ((RetailerGenome)this.generation.get(0)).getFitness();
      for (RetailerGenome gn : this.generation)
        copy.add(((gn.getFitness()) - minValue));
    }
    else {
      double maxValue = 1.05D * ((RetailerGenome)this.generation.get(this.populationSize - 1)).getFitness();
      for (RetailerGenome gn : this.generation) {
        copy.add((maxValue - (gn.getFitness())));
      }
    }
    double total = 0.0D;
    for (Double d : copy) {
      total += d.doubleValue();
    }
    for (int i = 0; i < this.populationSize - 1; i++) {
      x[(i + 1)] = (Double.valueOf(i).doubleValue() + 0.5D);
      y[(i + 1)] = (y[i] + ((Double)copy.get(i)) / total);
    }

    this.cdf = new CumulativeDistribution(x, y);
  }

  public String toString()
  {
    Collections.sort(this.generation);
    String result = new String();
    for (int i = 0; i < this.populationSize; i++) {
      result = result + String.valueOf(i + 1) + ":\t";
      for (int j = 0; j < this.genomeLength - 1; j++) {
        result = result + String.valueOf(((RetailerGenome)this.generation.get(i)).getGenome().get(j)) + " -> ";
      }
      result = result + String.valueOf(((RetailerGenome)this.generation.get(i)).getGenome().get(this.genomeLength - 1)) + 
        "\t\t" + String.valueOf(((RetailerGenome)this.generation.get(i)).getFitness());
      if (this.incumbent == this.generation.get(i)) {
        result = result + "*";
      }
      result = result + "\n";
    }
    return result;
  }

  public void evolve(double elites, double mutants, int crossoverType, ArrayList<Integer> precedenceVector)
  {
    int numElites = (int)Math.max(1L, Math.round(elites * this.populationSize));
    int numMutants = (int)Math.max(1L, Math.round(mutants * this.populationSize));
    int numCrossovers = this.populationSize - numElites - numMutants;

    ArrayList<RetailerGenome> newGeneration = new ArrayList<RetailerGenome>(this.populationSize);

    newGeneration.add(this.incumbent);
    for (int i = populationSize - numElites; i < populationSize - 1; i++) {
      newGeneration.add((RetailerGenome)this.generation.get(i));
    }

    for (int i = 0; i < numMutants; i++) {
      int pos = (int)this.cdf.sampleFromCDF();
      ArrayList<Integer> al = ((RetailerGenome)this.generation.get(pos)).clone();
      for (int j = 0; j < 5; j++) {
        ArrayList<Integer> newAl = mutate(al);
        for (int k = 0; k < this.genomeLength; k++) {
          al.set(k, (Integer)newAl.get(k));
        }
      }
      RetailerGenome newGn = new RetailerGenome(this.fitnessFunction.evaluate(al).doubleValue(), 
        this.fitnessFunction.isMax(), 
        al);
      newGeneration.add(newGn);
    }

    switch (crossoverType)
    {
    case 1:
      break;
    case 2:
      if (precedenceVector != null) {
        int i = 0;
        while (i < numCrossovers) {
          ArrayList<Integer> P1 = ((RetailerGenome)this.generation.get((int)this.cdf.sampleFromCDF())).clone();
          ArrayList<Integer> P2 = ((RetailerGenome)this.generation.get((int)this.cdf.sampleFromCDF())).clone();

          RetailerGenome offspring = performMX(P1, P2, precedenceVector);
          newGeneration.add(offspring);
          i++;
        }
      } else {
        System.err.println("Trying to perform Merged Crossover without a precedence vector!");
        System.exit(0);
      }

      break;
    case 3:
      int i = 0;
      while (i < numCrossovers)
      {
        ArrayList<Integer> P1 = ((RetailerGenome)this.generation.get((int)this.cdf.sampleFromCDF())).clone();
        ArrayList<Integer> P2 = ((RetailerGenome)this.generation.get((int)this.cdf.sampleFromCDF())).clone();

        ArrayList<RetailerGenome> offspring = performPMX(P1, P2);

        if (newGeneration.size() < this.populationSize - 1) {
          newGeneration.addAll(offspring);
          i += 2;
        } else {
          newGeneration.add((RetailerGenome)offspring.get(0));
          i++;
        }
      }
      if (newGeneration.size() != this.populationSize) {
        System.err.printf("After PMX the new generation is of size %d, and not %d", new Object[] { Integer.valueOf(newGeneration.size()), Integer.valueOf(this.populationSize) });
        System.exit(0);
      }
      break;
    default:
      System.err.printf("Crossover type %d not implemented!", new Object[] { Integer.valueOf(crossoverType) });
    }

    for (int j = 0; j < this.populationSize; j++) {
      this.generation.set(j, (RetailerGenome)newGeneration.get(j));
      checkIncumbent((RetailerGenome)newGeneration.get(j));
    }
    Collections.sort(this.generation);

    checkDiversity();
    calculateStats();
    buildCDF();
  }

  private void calculateStats() {
    double total = 0.0D;
    for (RetailerGenome rg : this.generation) {
      total += rg.getFitness();
    }
    setBest(this.incumbent.getFitness());
    setAverage(total / this.populationSize);
    setWorst(((RetailerGenome)this.generation.get(0)).getFitness());
  }

  public ArrayList<RetailerGenome> performPMX(ArrayList<Integer> P1, ArrayList<Integer> P2) {
	  ArrayList<RetailerGenome> offspring = new ArrayList<RetailerGenome>(2);

	  ArrayList<Integer> c1 = new ArrayList<Integer>(this.genomeLength);
	  ArrayList<Integer> c2 = new ArrayList<Integer>(this.genomeLength);
	  for (int i = 0; i < this.genomeLength; i++) {
		  c1.add(Integer.valueOf(-2147483648));
		  c2.add(Integer.valueOf(-2147483648));
	  }

	  ArrayList<Integer> twoPoints = this.permutator.permutate(this.genomeLength);
	  int pointA = Math.min(((Integer)twoPoints.get(0)).intValue(), ((Integer)twoPoints.get(1)).intValue());
	  int pointB = Math.max(((Integer)twoPoints.get(0)).intValue(), ((Integer)twoPoints.get(1)).intValue());
	  
	  while (pointA > 376) {
		  twoPoints = this.permutator.permutate(this.genomeLength);
		  pointA = Math.min(((Integer)twoPoints.get(0)).intValue(), ((Integer)twoPoints.get(1)).intValue());
		  pointB = Math.max(((Integer)twoPoints.get(0)).intValue(), ((Integer)twoPoints.get(1)).intValue());
	  }
	  for (int j = pointA - 1; j <= pointB - 1; j++) {
		  c1.set(j, (Integer)P2.get(j));
		  c2.set(j, (Integer)P1.get(j));
	  }

	  int pos = 0;
	  while (pos < this.genomeLength) {
		  if (c1.contains(P1.get(pos))) {
			  pos++;
		  } else {
			  int thePos = findPmxPosition(pos, c1, P1);
			  c1.set(thePos, (Integer)P1.get(pos));
			  pos++;
		  }
	  }

	  pos = 0;
	  while (pos < this.genomeLength) {
		  if (c2.contains(P2.get(pos))) {
			  pos++;
		  } else {
			  int thePos = findPmxPosition(pos, c2, P2);
			  c2.set(thePos, (Integer)P2.get(pos));
			  pos++;
		  }
	  }
	  RetailerGenome offspring1 = new RetailerGenome(this.fitnessFunction.evaluate(c1).doubleValue(), 
			  this.fitnessFunction.isMax(), 
			  c1);
	  RetailerGenome offspring2 = new RetailerGenome(this.fitnessFunction.evaluate(c2).doubleValue(), 
			  this.fitnessFunction.isMax(), 
			  c2);
	  offspring.add(offspring1);
	  offspring.add(offspring2);

	  return offspring;
  	}

  private RetailerGenome performMX(ArrayList<Integer> p1, ArrayList<Integer> p2, ArrayList<Integer> precedence)
  {
    ArrayList c1 = new ArrayList(this.genomeLength);
    for (int i = 0; i < this.genomeLength; i++) {
      int p1Element = ((Integer)p1.get(i)).intValue();
      int p2Element = ((Integer)p2.get(i)).intValue();
      int posP1 = precedence.indexOf(Integer.valueOf(p1Element));
      int posP2 = precedence.indexOf(Integer.valueOf(p2Element));
      if (posP1 < posP2) {
        c1.add(Integer.valueOf(p1Element));
        int p2SwapPos = p2.indexOf(Integer.valueOf(p1Element));
        Collections.swap(p2, i, p2SwapPos);
      } else {
        c1.add(Integer.valueOf(p2Element));
        int p1SwapPos = p1.indexOf(Integer.valueOf(p2Element));
        Collections.swap(p1, i, p1SwapPos);
      }
    }
    RetailerGenome gn = new RetailerGenome(this.fitnessFunction.evaluate(c1).doubleValue(), 
      this.fitnessFunction.isMax(), 
      c1);
    return gn;
  }

  private int findPmxPosition(int position, ArrayList<Integer> thisList, ArrayList<Integer> parentList) {
    int result = -2147483648;
    if (((Integer)thisList.get(position)).intValue() == -2147483648) {
      result = position;
    } else {
      int filledPoint = ((Integer)thisList.get(position)).intValue();
      int filledPointPosition = parentList.indexOf(Integer.valueOf(filledPoint));
      result = findPmxPosition(filledPointPosition, thisList, parentList);
    }

    return result;
  }

  public ArrayList<RetailerGenome> getGeneration() {
	  
	  return generation;
  }
  public RetailerGenome getIncumbent()
  {
    return this.incumbent;
  }

  private void setBest(double best) {
    this.best = best;
  }

  private void setAverage(double average) {
    this.average = average;
  }

  private void setWorst(double worst) {
    this.worst = worst;
  }

  public ArrayList<Double> getStats() {
    ArrayList<Double> result = new ArrayList<Double>(3);
    result.add(Double.valueOf(this.best));
    result.add(Double.valueOf(this.average));
    result.add(Double.valueOf(this.worst));
    return result;
  }
}