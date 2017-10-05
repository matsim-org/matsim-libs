/**
 * 
 */
package playground.clruch.analysis.performancefleetsize;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.network.Link;

import ch.ethz.idsc.queuey.core.networks.VirtualLink;
import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.core.networks.VirtualNode;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.red.Norm;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.analysis.DiagramCreator;
import playground.clruch.traveldata.TravelData;

/** @author Claudio Ruch */
public enum PerformanceFleetSizeUtils {
    ;

    /* package */ static Set<Integer> calcPeakSteps(VirtualNetwork<Link> virtualNetwork, TravelData tData, double PEAKPERCENTAGE) {
        int numberTimeSteps = tData.getNumbertimeSteps();
        int dt = tData.getdt();

        // list arrivals per time step
        List<Double> arrivalNumbers = new ArrayList<>();
        Tensor totalArrivals = RealScalar.ZERO;
        for (int k = 0; k < numberTimeSteps; ++k) {
            Tensor lambdaii = tData.getLambdaPSFforTime(k * dt);
            Tensor totalArrivalRate = Total.of(lambdaii);
            Tensor totalArivalTS = (totalArrivalRate.multiply(RealScalar.of(dt)));
            System.out.println("arrivals in this time step: " + totalArivalTS);
            totalArrivals = totalArrivals.add(totalArivalTS);
            RealScalar arrivals = (RealScalar) totalArrivalRate.multiply(RealScalar.of(dt));
            arrivalNumbers.add(arrivals.number().doubleValue());
        }
        System.out.println("arrivals in total: " + totalArrivals);

        double maxArrival = Collections.max(arrivalNumbers);

        Set<Integer> peakSteps = new HashSet<Integer>();

        for (int k = 0; k < numberTimeSteps; ++k) {
            if (arrivalNumbers.get(k) > PEAKPERCENTAGE * maxArrival) {
                peakSteps.add(k);

            }
        }

        return peakSteps;

    }

    /* package */ static Tensor calctpTot(Tensor tpStation, Tensor pij) {
        int numdisjointSol = Dimensions.of(tpStation).get(0);
        System.out.println("number of disjoint solutions =  " + numdisjointSol);
        int numVNode = Dimensions.of(pij).get(0);

        Tensor tpRoad = Array.zeros(numVNode, numVNode);
        for (int i = 0; i < numVNode; ++i) {
            for (int j = 0; j < numVNode; ++j) {
                Scalar tpstati = tpStation.Get(0, i);
                Scalar pijel = pij.Get(i, j);
                Scalar tpij = pijel.multiply(tpstati);
                tpRoad.set(tpij, i, j);
            }
        }

        Tensor tp = Tensors.empty();

        for (int sol = 0; sol < numdisjointSol; ++sol) {
            Tensor tpsol = tpStation.get(sol);

            for (int i = 0; i < numVNode; ++i) {
                for (int j = 0; j < numVNode; ++j) {
                    if (i != j) {
                        tpsol.append(tpRoad.Get(i, j));
                    }
                }
            }
            tp.append(tpsol);
        }

        return tp;

    }

    /* package */static Tensor calcSRRoad(VirtualNetwork<Link> virtualNetwork, int avSpeed) {
        int numVNode = virtualNetwork.getvNodesCount();
        Tensor serviceRateRoads = Array.zeros(numVNode, numVNode);
        for (int i = 0; i < numVNode; ++i) {
            for (int j = 0; j < numVNode; ++j) {
                VirtualNode<Link> from = virtualNetwork.getVirtualNode(i);
                VirtualNode<Link> to = virtualNetwork.getVirtualNode(j);
                Scalar serviceRate = RealScalar.ZERO;
                if (i != j) {
                    VirtualLink<Link> vLink = virtualNetwork.getVirtualLink(from, to);
                    Scalar distance = (Scalar) RealScalar.of(vLink.getDistance());   
                    serviceRate = RealScalar.of(avSpeed).divide(distance);
                }
                serviceRateRoads.set(serviceRate, i, j);
            }
        }

        Tensor serviceRateRoadsVector = Tensors.empty();

        for (int i = 0; i < Dimensions.of(serviceRateRoads).get(0); ++i) {
            for (int j = 0; j < Dimensions.of(serviceRateRoads).get(1); ++j) {
                if (i != j) {
                    serviceRateRoadsVector.append(serviceRateRoads.Get(i, j));
                }
            }
        }
        return serviceRateRoadsVector;
    }
    
    

    /* package */ static Tensor calcMeanByVehicles(Tensor a, Set<Integer> steps) {
        int numVNode = Dimensions.of(a).get(0);
        int vehicleBins = Dimensions.of(a).get(2);

        Tensor meanByVehicles = Array.zeros(vehicleBins);
        int numElem = 0;
        for (int i = 0; i < numVNode; ++i) {
            System.out.println("---");
            for (int j : steps) {
                Tensor aByVehicle = a.get(i, j);
                if (Scalars.lessThan(RealScalar.of(0.001), Norm._1.of(aByVehicle))) {
                    meanByVehicles = meanByVehicles.add(aByVehicle);
                    ++numElem;
                }
            }
        }

        meanByVehicles = meanByVehicles.multiply(RealScalar.of(1.0 / numElem));
        return meanByVehicles;

    }

    /* package */ static void plot(Tensor values, Tensor valuesPeak, File relativeDirectory) throws Exception {
        XYSeries series = new XYSeries("Availability");
        for (int i = 0; i < Dimensions.of(values).get(0); ++i) {
            series.add(i, values.Get(i).number().doubleValue());
        }
        XYSeries seriesPeak = new XYSeries("Availability Peak");
        for (int i = 0; i < Dimensions.of(values).get(0); ++i) {
            seriesPeak.add(i, valuesPeak.Get(i).number().doubleValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        dataset.addSeries(seriesPeak);

        JFreeChart timechart = ChartFactory.createXYLineChart("Vehicle Availability", "Number of Vehicles", //
                "Availability", dataset);

        // range and colors of the background/grid
        timechart.getPlot().setBackgroundPaint(Color.white);
        timechart.getXYPlot().setRangeGridlinePaint(Color.lightGray);
        timechart.getXYPlot().setDomainGridlinePaint(Color.lightGray);

        // line thickness
        for (int k = 0; k < values.length(); k++) {
            timechart.getXYPlot().getRenderer().setSeriesStroke(k, new BasicStroke(2.0f));
        }

        // set text fonts
        timechart.getTitle().setFont(DiagramCreator.titleFont);
        timechart.getXYPlot().getDomainAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getRangeAxis().setLabelFont(DiagramCreator.axisFont);
        timechart.getXYPlot().getDomainAxis().setTickLabelFont(DiagramCreator.tickFont);
        timechart.getXYPlot().getRangeAxis().setTickLabelFont(DiagramCreator.tickFont);

        // save plot as png
        int width = 1000; // Width of the image
        int height = 750; // Height of the image
        DiagramCreator.savePlot(relativeDirectory, "availbilitiesByNumberVehicles", timechart, width, height);
    }

}
