package playground.clruch.dispatcher.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import ch.ethz.idsc.tensor.ZeroScalar;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;

/**
 * Created by Claudio on 3/19/2017.
 */
public class ArrivalInformation {
    Tensor lambda;
    Tensor pij;
    Tensor alpha_ij;
    private int numberTimeSteps;
    private final double dtSeconds; // used as lookup
    public final long populationSize;
    // private final Scalar factor;
    VirtualNetwork virtualNetwork;

    public ArrivalInformation(VirtualNetwork virtualNetworkIn, File lambdaFile, File pijFile, File alphaijFile, long populationSize, int rebalancingPeriod) throws JDOMException, IOException {
        virtualNetwork = virtualNetworkIn;
        this.populationSize = populationSize;
        System.out.println("reading historic travel data");
        {// arrival rates lambda
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(lambdaFile);
            Element rootNode = document.getRootElement();
            List<Element> children = rootNode.getChildren();

            // get number of time steps and time step size
            numberTimeSteps = children.get(0).getChildren().size();
            Attribute attribute = rootNode.getAttribute("dt");
            dtSeconds = secfromStringTime(attribute.getValue());

            // construct matrices based on xml
            // the lambdas are normalized of their population size and time bin width
            Scalar factor = RealScalar.of(populationSize * rebalancingPeriod);
            lambda = Tensors.matrix((i, j) -> getLambdafromFile(i, j, rootNode).multiply(factor), numberTimeSteps, virtualNetwork.getvNodesCount()-1);
        }
        {// transition probabilities pij
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(pijFile);
            Element rootNode = document.getRootElement();
            // List<Element> children = rootNode.getChildren();

            pij = Tensors.empty();
            for (int k = 0; k < numberTimeSteps; ++k) {
                final int timestep = k;
                pij.append(Tensors.matrix((i, j) -> getpijfromFile(i, j, timestep, rootNode), virtualNetwork.getvNodesCount()-1, virtualNetwork.getvNodesCount()-1));
            }
            System.out.println("Do we get here?");
        }
        {// rebalancing rates alpha_ij
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(alphaijFile);
            Element rootNode = document.getRootElement();
            // List<Element> children = rootNode.getChildren();

            alpha_ij = Tensors.empty();
            for (int k = 0; k < numberTimeSteps; ++k) {
                final int timestep = k;
                alpha_ij.append(Tensors.matrix((i, j) -> getalphaijfromFile(i, j, timestep, rootNode), virtualNetwork.getvNodesCount()-1, virtualNetwork.getvNodesCount()-1));
            }
            System.out.println("Do we get here?");
        }
    }

    /**
     * @param timeString
     *            in the format hh:mm:ss
     * @return time in secons
     */
    private int secfromStringTime(String timeString) {
        String[] parts = timeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    private Scalar getLambdafromFile(int row, int col, Element rootNode) {
        List<Element> children = rootNode.getChildren();

        // get element with virtualNode col
        VirtualNode correctNode = virtualNetwork.getVirtualNode(col);
        Element vNodeelem = children.stream().filter(v -> v.getAttribute("id").getValue().toString().equals(correctNode.getId())).findFirst().get();

        // get element with number row
        Element timeatvNode = vNodeelem.getChildren().get(row);

        // return entry
        return RealScalar.of(Double.parseDouble(timeatvNode.getAttribute("lambda").getValue()));
    }

    private Scalar getpijfromFile(int from, int to, int timestep, Element rootNode) {
        List<Element> children = rootNode.getChildren();

        // get element with virtualLink col
        VirtualNode vNodeFrom = virtualNetwork.getVirtualNode(from);
        VirtualNode vNodeTo = virtualNetwork.getVirtualNode(to);

        Optional<Element> optional = children.stream()
                .filter(v -> v.getAttribute("from").getValue().toString().equals(vNodeFrom.getId()) && v.getAttribute("to").getValue().toString().equals(vNodeTo.getId()))
                .findFirst();

        if (optional.isPresent()) {
            Element vLinkElem = optional.get();
            Element timeatvLink = vLinkElem.getChildren().get(timestep);
            return RealScalar.of(Double.parseDouble(timeatvLink.getAttribute("P_ij").getValue()));
        }
        System.out.println("Whats happening?");
        GlobalAssert.that(false);
        return ZeroScalar.get();
    }

    private Scalar getalphaijfromFile(int from, int to, int timestep, Element rootNode) {
        List<Element> children = rootNode.getChildren();

        // get element with virtualLink col
        VirtualNode vNodeFrom = virtualNetwork.getVirtualNode(from);
        VirtualNode vNodeTo = virtualNetwork.getVirtualNode(to);

        Optional<Element> optional = children.stream()
                .filter(v -> v.getAttribute("from").getValue().toString().equals(vNodeFrom.getId()) && v.getAttribute("to").getValue().toString().equals(vNodeTo.getId()))
                .findFirst();

        if (optional.isPresent()) {
            Element vLinkElem = optional.get();
            Element timeatvLink = vLinkElem.getChildren().get(timestep);
            return RealScalar.of(Double.parseDouble(timeatvLink.getAttribute("alpha_ij").getValue()));
        }
        System.out.println("Whats happening?");
        GlobalAssert.that(false);
        return ZeroScalar.get();
    }

    public Tensor getLambdaforTime(int time) {
        int row = (int) Math.min(time / dtSeconds, numberTimeSteps - 1);
        return lambda.get(row).copy();
    }

    public Scalar getLambdaforTime(int time, int vNodeindex) {
        return getLambdaforTime(time).Get(vNodeindex);
    }
    public Tensor getNextNonZeroLambdaforTime(int time) {
        List<Integer> dim_lambda = Dimensions.of(lambda);
        int N                    = dim_lambda.get(1);
        Tensor nZlambda = Array.zeros(N);

        for (int i=0;i<N;i++){
            nZlambda.set(getNextNonZeroLambdaforTime(time,i),i);
        }
        return nZlambda;
    }

    public Scalar getNextNonZeroLambdaforTime(int time, int vNodeindex) {
        int row           = (int) Math.min(time / dtSeconds, numberTimeSteps - 1);
        Scalar nZ_lambda  = lambda.Get(row, vNodeindex);
        List<Integer> dim_lambda = Dimensions.of(lambda);

        while (nZ_lambda.number().doubleValue() == 0){
            row++;
            if (!(row<dim_lambda.get(0))){
                row = 0; //Cylcic Search for non-zero element
            }
            nZ_lambda = lambda.Get(row, vNodeindex);
         }
         return nZ_lambda;
    }


    public Scalar getpijforTime(int time, int from, int to) {
        int timestep = (int) Math.min(time / dtSeconds, numberTimeSteps - 1);
        return pij.Get(timestep, from, to);
    }

    public Tensor getAlphaijforTime(int time) {
        int timestep = (int) Math.min(time / dtSeconds, numberTimeSteps - 1);
        return alpha_ij.get(timestep).copy();
    }
}