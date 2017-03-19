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
import playground.clruch.netdata.VirtualLink;
import playground.clruch.netdata.VirtualNetwork;
import playground.clruch.netdata.VirtualNode;

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


    public ArrivalInformation(VirtualNetwork virtualNetworkIn, File file) throws JDOMException, IOException {
        virtualNetwork = virtualNetworkIn;
        load(virtualNetwork, file);
    }

    private void load(VirtualNetwork virtualNetwork, File file) throws JDOMException, IOException {
        System.out.println("reading historic travel data");
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(file);
        Element rootNode = document.getRootElement();
        List<Element> children = rootNode.getChildren();

        // get number of time steps and time step size
        numberTimeSteps = children.get(0).getChildren().size();
        Attribute attribute = rootNode.getAttribute("dt");
        dtSeconds = secfromStringTime(attribute.getValue());

        // construct matrices based on xml
        lambda = Tensors.matrix((i, j) -> getlambdafromFile(i, j, dtSeconds, rootNode), numberTimeSteps, virtualNetwork.getvNodesCount());
        pij = Tensors.matrix((i, j) -> getpijfromFile(i, j, dtSeconds, rootNode), numberTimeSteps, virtualNetwork.getvLinksCount());

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

    Scalar getlambdafromFile(int col, int row, int dt, Element rootNode) {
        List<Element> children = rootNode.getChildren();

        // get element with virtualNode row
        VirtualNode correctNode = virtualNetwork.getVirtualNode(row);
        Attribute attribute = children.get(0).getAttribute("id");
        Element vNodeelem = children.stream().filter(v -> v.getAttribute("id").getValue().toString().equals(correctNode.getId())).findFirst().get();

        // get element with number col
        Element timeatvNode = vNodeelem.getChildren().get(col);

        Scalar testscalar = RealScalar.of(Integer.parseInt(timeatvNode.getAttribute("lambda").getValue()));

        if (testscalar.getAbsDouble() > 0) {
            System.out.print("sto phere");
        }


        // return entry
        return RealScalar.of(Integer.parseInt(timeatvNode.getAttribute("lambda").getValue()) / (double) dt);

    }

    // TODO implement this
    Scalar getpijfromFile(int col, int row, int dt, Element rootNode) {
        return  RealScalar.of(0.0);
    }

    public Scalar getLambdaforTime(double time, int vNodeindex) {
        int row = Math.min((int) time / dtSeconds, numberTimeSteps - 1);
        int col = vNodeindex;
        return lambda.get(row).Get(col);
    }

    public Scalar getpijforTime(int time, int from, int to) {
        int row = Math.min((int) time / dtSeconds, numberTimeSteps - 1);

        Optional<VirtualLink> optional = virtualNetwork.getVirtualLinks().stream()
                .filter(v -> v.getFrom().getIndex() == from && v.getTo().getIndex() == to).findAny();

        if (optional.isPresent()) {
            int col = optional.get().getIndex();
            return pij.get(row).Get(col);
        } else {
            return RealScalar.of(0.0);
        }

    }

}