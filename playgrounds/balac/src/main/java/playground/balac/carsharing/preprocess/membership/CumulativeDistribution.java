package playground.balac.carsharing.preprocess.membership;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

public class CumulativeDistribution
{
  double[] xs;
  double[] ys;
  int numObservations;
  private static final Logger log = Logger.getLogger(CumulativeDistribution.class);

  public CumulativeDistribution(double lowerBound, double upperBound, int numBins) {
    this.xs = new double[numBins + 1];
    this.ys = new double[numBins + 1];
    for (int i = 0; i < numBins + 1; i++)
      this.xs[i] = (lowerBound + (upperBound - lowerBound) * i / numBins);
    this.ys[numBins] = 1.0D;
  }

  public CumulativeDistribution(double[] xs, double[] ys) {
    assert (xs.length == ys.length);
    this.xs = ((double[])xs.clone());
    this.ys = ((double[])ys.clone());
    this.numObservations = 1;
  }

  public void addObservation(double x) {
    addObservations(x, 1.0D);
  }

  public double getLowerBound() {
    return this.xs[0];
  }

  public double getUpperBound() {
    return this.xs[(this.xs.length - 1)];
  }

  public int getNumBins() {
    return this.xs.length - 1;
  }

  public double error(CumulativeDistribution dist2)
  {
    double error = 0.0D;
    for (int i = 0; i < this.xs.length - 1; i++)
      error += (this.ys[i] - dist2.ys[i]) * (this.ys[i] - dist2.ys[i]);
    return Math.sqrt(error);
  }

  public void addObservations(double x, double y) {
    int idx = 0;
    while ((x >= this.xs[idx]) && (idx < this.xs.length - 1))
      idx++;
    for (int i = 0; i < this.xs.length; i++) {
      if (i < idx)
        this.ys[i] = (this.ys[i] * this.numObservations / (this.numObservations + y));
      else {
        this.ys[i] = ((this.ys[i] * this.numObservations + y) / (this.numObservations + y));
      }
    }
    while (this.ys[(idx++)] < 1.0D);
    while (idx < this.ys.length) {
      this.ys[(idx++)] = 1.0D;
    }
    this.numObservations = ((int)(this.numObservations + y));
  }

  public void print() {
    for (int i = 0; i < this.xs.length; i++)
      System.out.println(this.xs[i] + "\t" + this.ys[i]);
  }

  public static CumulativeDistribution readDistributionFromFile(String filename)
  {
    Vector x = new Vector();
    Vector y = new Vector();

    LineNumberReader lnr = null;
    try {
      lnr = new LineNumberReader(IOUtils.getBufferedReader(filename));
      String line = null;
      while ((line = lnr.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(line);
        x.add(Double.valueOf(st.nextToken()));
        y.add(Double.valueOf(st.nextToken()));
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();

      if (lnr != null) try {
          lnr.close(); } catch (IOException e1) {
          log.warn("Could not close stream.", e);
        }
    }
    catch (IOException e)
    {
      e.printStackTrace();

      if (lnr != null) try {
          lnr.close(); } catch (IOException e1) {
          log.warn("Could not close stream.", e);
        }
    }
    finally
    {
      if (lnr != null) try {
          lnr.close(); } catch (IOException e) {
          log.warn("Could not close stream.", e);
        }
    }
    double[] xs = new double[x.size()];
    double[] ys = new double[x.size()];
    for (int i = 0; i < x.size(); i++) {
      xs[i] = ((Double)x.get(i)).doubleValue();
      ys[i] = ((Double)y.get(i)).doubleValue();
    }
    return new CumulativeDistribution(xs, ys);
  }

  public double sampleFromCDF()
  {
    Double d = null;
    double rnd = MatsimRandom.getRandom().nextDouble();

    int index = 1;
    while ((d == null) && (index <= getNumBins())) {
      if (this.ys[index] > rnd)
        d = Double.valueOf(this.xs[(index - 1)] + (this.xs[index] - this.xs[(index - 1)]) / 2.0D);
      else {
        index++;
      }
    }
    assert (d != null) : "Could not draw from the cumulative distribution function";
    return d.doubleValue();
  }
}