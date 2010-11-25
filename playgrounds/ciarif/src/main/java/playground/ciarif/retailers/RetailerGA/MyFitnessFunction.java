package playground.ciarif.retailers.RetailerGA;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import playground.ciarif.retailers.models.RetailerModel;

public class MyFitnessFunction
{
  private ArrayList<Coordinate> points;
  private int numberOfPoints;
  private final boolean max;
  private ArrayList<Integer> precedenceVector;
  private RetailerModel rm;
  private static final Logger log = Logger.getLogger(RetailerGA.class);

  public MyFitnessFunction(boolean isMax, int number) {
    this.max = isMax;
    this.numberOfPoints = number;
    this.points = new ArrayList(this.numberOfPoints);
    this.precedenceVector = generateRandomInstance(this.numberOfPoints, -100, 100, -100, 100);
  }

  public MyFitnessFunction(boolean isMax, int number, RetailerModel rm) {
    this.max = isMax;
    this.numberOfPoints = number;
    this.points = new ArrayList(this.numberOfPoints);
    this.precedenceVector = generateRandomInstance(this.numberOfPoints, -100, 100, -100, 100);
    this.rm = rm;
  }

  public Double evaluate(ArrayList<Integer> solution) {
    Double fitness = Double.valueOf(0.0D);
    fitness = Double.valueOf(this.rm.computePotential(solution));

    return fitness;
  }

  public ArrayList<Integer> generateRandomInstance(int numberOfPoints, int xMin, int xMax, int yMin, int yMax)
  {
    Coordinate depot = new Coordinate(0.0D, 0.0D);
    this.points.add(depot);

    double xDif = xMax - xMin;
    double yDif = yMax - yMin;
    for (int i = 1; i < numberOfPoints; ++i) {
      double x = xMin + MatsimRandom.getRandom().nextDouble() * xDif;
      double y = yMin + MatsimRandom.getRandom().nextDouble() * yDif;
      Coordinate c = new Coordinate(x, y);
      this.points.add(c);
    }
    ArrayList precedenceVector = createPrecedenceVector();
    return precedenceVector;
  }

  private ArrayList<Integer> createPrecedenceVector() {
    ArrayList result = new ArrayList(this.numberOfPoints);
    ArrayList<Double> distances = new ArrayList(this.numberOfPoints);
    Coordinate depot = new Coordinate(0.0D, 0.0D, 0.0D);
    for (Coordinate c : getPoints()) {
      distances.add(Double.valueOf(depot.distance(c)));
    }

    ArrayList<Double> newDistances = new ArrayList<Double>(this.numberOfPoints);
    for (Double double1 : distances) {
      newDistances.add(Double.valueOf(double1.doubleValue()));
    }
    Collections.sort(newDistances);

    while (newDistances.size() > 0) {
      int index = distances.indexOf(newDistances.get(0));
      result.add(Integer.valueOf(index + 1));
      newDistances.remove(0);
    }

    return result;
  }

  public ArrayList<Coordinate> getPoints() {
    return this.points;
  }

  public boolean isMax() {
    return this.max;
  }

  public ArrayList<Integer> getPrecedenceVector() {
    return this.precedenceVector;
  }

  public void setPrecedenceVector(ArrayList<Integer> precedenceVector) {
    this.precedenceVector = precedenceVector;
  }
}
