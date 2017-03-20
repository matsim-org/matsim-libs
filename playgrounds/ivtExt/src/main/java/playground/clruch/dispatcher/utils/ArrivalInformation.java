package playground.clruch.dispatcher.utils;

import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.Tensors;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;
import playground.clruch.utils.GlobalAssert;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Created by Claudio on 3/19/2017.
 */
public class ArrivalInformation {
    Tensor lambda;
    Tensor pij;
    private int numberTimeSteps;
    private int dtSeconds;
    VirtualNetwork virtualNetwork;


    public ArrivalInformation(VirtualNetwork virtualNetworkIn, File lambdaFile, File pijFile) throws JDOMException, IOException {
        virtualNetwork = virtualNetworkIn;
        load(virtualNetwork, lambdaFile, pijFile);
    }

    private void load(VirtualNetwork virtualNetwork, File lambdaFile, File pijFile) throws JDOMException, IOException {
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
            lambda = Tensors.matrix((i, j) -> getlambdafromFile(i, j, dtSeconds, rootNode), numberTimeSteps, virtualNetwork.getvNodesCount());
        }
        {// transition probabilities pij
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(pijFile);
            Element rootNode = document.getRootElement();
            List<Element> children = rootNode.getChildren();

            pij = Tensors.empty();
            for (int k = 0; k < numberTimeSteps; ++k) {
                final int timestep = k;
                pij.append(Tensors.matrix((i, j) -> getpijfromFile(i, j, timestep, rootNode), virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount()));
            }

            System.out.println("Do we get here?");
        }
    }

    /**
     * @param timeString in the format hh:mm:ss
     * @return time in secons
     */
    int secfromStringTime(String timeString) {
        String[] parts = timeString.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    Scalar getlambdafromFile(int row, int col, int dt, Element rootNode) {

        List<Element> children = rootNode.getChildren();

        // get element with virtualNode col
        VirtualNode correctNode = virtualNetwork.getVirtualNode(col);
        Element vNodeelem = children.stream().filter(v -> v.getAttribute("id").getValue().toString().equals(correctNode.getId())).findFirst().get();

        // get element with number row
        Element timeatvNode = vNodeelem.getChildren().get(row);

        // return entry
        return RealScalar.of(Integer.parseInt(timeatvNode.getAttribute("lambda").getValue()) / (double) dt);

    }

    Scalar getpijfromFile(int from, int to, int timestep, Element rootNode) {

        List<Element> children = rootNode.getChildren();

        // get element with virtualLink col
        VirtualNode vNodeFrom = virtualNetwork.getVirtualNode(from);
        VirtualNode vNodeTo = virtualNetwork.getVirtualNode(to);

        Optional<Element> optional = children.stream()
                .filter(v -> v.getAttribute("from").getValue().toString().equals(vNodeFrom.getId()) &&
                        v.getAttribute("to").getValue().toString().equals(vNodeTo.getId())).findFirst();

        if (optional.isPresent()) {
            Element vLinkElem = optional.get();
            Element timeatvLink = vLinkElem.getChildren().get(timestep);
            return RealScalar.of(Double.parseDouble(timeatvLink.getAttribute("P_ij").getValue()));
        } else {
            System.out.println("Whats happening?");
            GlobalAssert.that(false);
            return RealScalar.of(0.0);
        }

    }

    public Scalar getLambdaforTime(double time, int vNodeindex) {
        int row = Math.min((int) time / dtSeconds, numberTimeSteps - 1);
        int col = vNodeindex;
        return lambda.get(row).Get(col);
    }

    public Scalar getpijforTime(int time, int from, int to) {
        int timestep = Math.min(time / dtSeconds, numberTimeSteps - 1);
        return  pij.get(timestep).get(from).Get(to);
    }

}