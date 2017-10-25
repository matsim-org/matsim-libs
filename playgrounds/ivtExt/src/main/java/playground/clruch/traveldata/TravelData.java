// code by jph
package playground.clruch.traveldata;

import java.io.Serializable;
import java.util.List;

import org.gnu.glpk.GLPKConstants;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.queuey.core.networks.VirtualNetwork;
import ch.ethz.idsc.queuey.math.TensorOperations;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import ch.ethz.idsc.tensor.RealScalar;
import ch.ethz.idsc.tensor.Scalar;
import ch.ethz.idsc.tensor.Scalars;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.idsc.tensor.alg.Array;
import ch.ethz.idsc.tensor.alg.Dimensions;
import ch.ethz.idsc.tensor.alg.Transpose;
import ch.ethz.idsc.tensor.red.Total;
import playground.clruch.dispatcher.utils.LPVehicleRebalancing;

/**
 * Created by Claudio on 5/6/2017.
 */
public class TravelData implements Serializable {
    /**
     * 
     */
    // general
    private Tensor lambda; // tensor of dimension (numberofTimeSteps, numberVirtualNodes) that contains mean arrival rates per virtual Node in timestep
    private Tensor pij; // tensor of dimension (numberofTimeSteps, numberVirtualNodes, numberVirtualNodes) pij(t,i,j) probability of moving from i to j in
                // timestep t
    // framework from Pavone, Marco, Stephen L. Smith, and Emilio Frazzoli Daniela Rus. "Load balancing for mobility-on-demand systems." (2011).
    private Tensor lambdaPSF;
    private Tensor pijPSF;
    private Tensor alphaijPSF; // tensor of dimension (numberofTimeSteps, numberVirtualNodes, numberVirtualNodes) alpha(t,i,j) static rebalancing rates from i
                       // to j at timestep t
    private Tensor lambdaPSFij;
    
    private int numberTimeSteps;
    private final int dt; // used as lookup
    public final long populationSize;
    public final int dayduration = 30 * 60 * 60;
    // private final Scalar factor;
    protected transient VirtualNetwork virtualNetwork;
    private final long virtualNetworkID;

    /**
     * Constructor for TravelData object creating historic travel information based on virtualNetwork and population
     * 
     * @param virtualNetworkIn
     * @param population
     * @param dtIn
     *            time step for calculation
     */
    public TravelData(VirtualNetwork virtualNetworkIn, Network network, Population population, int dtIn) {
        System.out.println("reading travel data for population of size " + population.getPersons().size());
        populationSize = population.getPersons().size();
        virtualNetwork = virtualNetworkIn;
        virtualNetworkID = virtualNetworkIn.getvNetworkID();
        System.out.println("the ID of the virtualNetwork used for travel data construction is: " + virtualNetworkID);

        // ensure that dayduration / timeInterval is integer value
        dt = TravelDataUtils.greatestNonRestDt(dtIn, dayduration);
        numberTimeSteps = dayduration / dt;
        System.out.println("Number of time steps = " + numberTimeSteps);
        System.out.println("dt = " + dt);
        GlobalAssert.that(dayduration % dt == 0);

        // create lambda, pij and alphaijPSF Tensors
        lambda = Array.zeros(numberTimeSteps, virtualNetwork.getvNodesCount());
        pij = Array.zeros(numberTimeSteps, virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());
        alphaijPSF = Array.zeros(numberTimeSteps, virtualNetwork.getvNodesCount(), virtualNetwork.getvNodesCount());

        // fill based on population file
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {

                for (int i = 1; i < plan.getPlanElements().size() - 1; ++i) {
                    PlanElement planElMins = plan.getPlanElements().get(i - 1);
                    PlanElement planElMidl = plan.getPlanElements().get(i);
                    PlanElement planElPlus = plan.getPlanElements().get(i + 1);

                    if (planElMidl instanceof Leg) {
                        Leg leg = (Leg) planElMidl;
                        if (leg.getMode().equals("av")) {
                            // get time and vNode index
                            double depTime = ((Leg) planElMidl).getDepartureTime();
                            int timeIndex = (int) Math.floor((depTime / dt));
                            Link linkFrom = network.getLinks().get(((Activity) planElMins).getLinkId());
                            Link linkTo = network.getLinks().get(((Activity) planElPlus).getLinkId());
                            int vNodeIndexFrom = virtualNetwork.getVirtualNode(linkFrom).getIndex();
                            int vNodeIndexTo = virtualNetwork.getVirtualNode(linkTo).getIndex();

                            // add customer/dt to arrival rate
                            lambda.set(s -> s.add(RealScalar.of(dt).reciprocal()), timeIndex, vNodeIndexFrom);
                            // old implementation, TODO remove
                            // Scalar val = lambda.Get(timeIndex, vNodeIndexFrom);
                            // Scalar valAdded = val.add(RealScalar.of(1.0 / (double) dt));
                            // lambda.set(valAdded, timeIndex, vNodeIndexFrom);

                            // add trips to pij (has to be normed later)
                            pij.set(s -> s.add(RealScalar.ONE), timeIndex, vNodeIndexFrom, vNodeIndexTo);

                        }
                    }
                }
            }
        }
        


        // norm pij such that it is row stochastic, i.e. sum(p_ij)_j = 1 for all i and all time indexes
        // count the total of the rows
        for (int t = 0; t < numberTimeSteps; ++t) {
            pij.set(TensorOperations.normToRowStochastic(pij.get(t)), t);
        }

        // compute pijPSF
        // pijPSF = row-stochastic normalized matrix derived from pij with pii = 0
        pijPSF = pij.copy();
        for (int t = 0; t < numberTimeSteps; ++t) {
            for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                pijPSF.set(RealScalar.ZERO, t, i, i);
            }
        }
        for (int t = 0; t < numberTimeSteps; ++t) {
            pijPSF.set(TensorOperations.normToRowStochastic(pijPSF.get(t)), t);
        }
        
        

        // compute lambdaPSF
        // lambda PSF = lambda - pii*lambda
        lambdaPSF = Array.zeros(numberTimeSteps, virtualNetwork.getvNodesCount());
        for (int t = 0; t < numberTimeSteps; ++t) {
            for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                Tensor labmdaold = lambda.get(t, i);
                Tensor pii = pij.get(t, i, i);
                // TODO remove old implementation
                // Tensor upd = labmdaold.subtract(labmdaold.multiply((Scalar) pii));
                // lambdaPSF.set(upd, t, i);
                lambdaPSF.set(labmdaold.subtract(labmdaold.multiply((Scalar) pii)), t, i);
            }
        }
        
        
        // compute lambdaPSFij
        // lambdaPSFij (i,j) = lambdaPSF (i) * pijPSF (i,j)
        lambdaPSFij = pijPSF.copy();
        for (int t = 0; t < numberTimeSteps; ++t) {
            for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                Tensor replace = lambdaPSFij.get(t,i).multiply(lambdaPSF.Get(t,i));
                lambdaPSFij.set(replace,t,i);
            }
        }
        

       
        
        


        // compute alphaij rates according to Pavone, Marco, Stephen L. Smith, and Emilio Frazzoli Daniela Rus. "Load balancing for mobility-on-demand
        // systems." (2011).
        LPVehicleRebalancing lpVehicleRebalancing = new LPVehicleRebalancing(virtualNetwork);
        for (int t = 0; t < numberTimeSteps; ++t) {

            // lambdas = [lbd_1, ... , lbd] row vector, where n nmbr. of vNodes
            Tensor lambdaT = lambdaPSF.get(t);

            // p_ij row-stochastic matrix (nxn) with transition probabilities from i to j
            Tensor pijT = pijPSF.get(t);

            // fill right-hand-side, i.e. rhs(i) = -lambda_i + sum_j * lambda_j p_ji
            Tensor dotprod = lambdaT.dot(pijT);

            // Tensor rhs = lambdaT.multiply(RealScalar.of(-1)).add(lambdaT.dot(pijT));
            Tensor rhs = lambdaT.subtract(dotprod);

            // solve the linear program with updated right-hand side
            Tensor rebalancingRate = lpVehicleRebalancing.solveUpdatedLP(rhs, GLPKConstants.GLP_FX);

            // ensure positivity of solution (small negative values possible due to solver
            // accuracy)

            rebalancingRate.flatten(-1).forEach(v -> GlobalAssert.that(v.Get().number().doubleValue() > -10E-12));
            // make negative values positive to ensure no problems in subsequent steps
            for(int i = 0; i<Dimensions.of(rebalancingRate).get(0);++i){
                for(int j = 0;j<Dimensions.of(rebalancingRate).get(1);++j){
                    if( Scalars.lessEquals(rebalancingRate.Get(i,j), RealScalar.ZERO)){
                        rebalancingRate.set(RealScalar.ZERO, i,j);                        
                    };
                }
            }            
            
            alphaijPSF.set(rebalancingRate, t);

        }

        checkConsistency();
        // TODO how to get dimensions of Tensor?
        System.out.println("successfully created lambda matrix of size " + lambda.length() + " x " + Transpose.of(lambda).length());
        System.out.println("successfully created pij matrix of size " + "?" + " x " + "?" + " x " + "?");
        System.out.println("successfully created alphaij matrix of size" + "?" + " x " + "?" + " x " + "?");

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

    public Tensor getLambdaforTime(int time) {
        int row = (int) Math.min(time / dt, numberTimeSteps - 1);
        return lambda.get(row).copy();
    }

    public Tensor getLambdaPSFforTime(int time) {
        int row = (int) Math.min(time / dt, numberTimeSteps - 1);
        return lambdaPSF.get(row).copy();
    }

    public Scalar getLambdaforTime(int time, int vNodeindex) {
        return getLambdaforTime(time).Get(vNodeindex);
    }

    public Tensor getNextNonZeroLambdaforTime(int time) {
        List<Integer> dim_lambda = Dimensions.of(lambda);
        int N = dim_lambda.get(1);
        Tensor nZlambda = Array.zeros(N);

        for (int i = 0; i < N; i++) {
            nZlambda.set(getNextNonZeroLambdaforTime(time, i), i);
        }
        return nZlambda;
    }

    public Scalar getNextNonZeroLambdaforTime(int time, int vNodeindex) {
        int row = (int) Math.min(time / dt, numberTimeSteps - 1);
        Scalar nZ_lambda = lambda.Get(row, vNodeindex);
        List<Integer> dim_lambda = Dimensions.of(lambda);

        while (nZ_lambda.number().doubleValue() == 0) {
            row++;
            if (!(row < dim_lambda.get(0))) {
                row = 0; // Cylcic Search for non-zero element
            }
            nZ_lambda = lambda.Get(row, vNodeindex);
        }
        return nZ_lambda;
    }

    public Scalar getpijforTime(int time, int from, int to) {
        int timestep = (int) Math.min(time / dt, numberTimeSteps - 1);
        return pij.Get(timestep, from, to);
    }

    public Tensor getpijPSFforTime(int time) {
        int timestep = (int) Math.min(time / dt, numberTimeSteps - 1);
        return pijPSF.get(timestep).copy();
    }
    
    public Tensor getlambdaijPSFforTime(int time) {
        int timestep = (int) Math.min(time / dt, numberTimeSteps - 1);
        return lambdaPSFij.get(timestep).copy();
    }
    

    public Scalar getpijPSFforTime(int time, int from, int to) {
        int timestep = (int) Math.min(time / dt, numberTimeSteps - 1);
        return pijPSF.Get(timestep, from, to);
    }

    public Tensor getAlphaijPSFforTime(int time) {
        int timestep = (int) Math.min(time / dt, numberTimeSteps - 1);
        return alphaijPSF.get(timestep).copy();
    }

    protected void fillSerializationInfo(VirtualNetwork<Link> virtualNetwork) {
        // check if TravelData object was created with the supplied virtualNetwork
        System.out.println("in network ID: " + virtualNetwork.getvNetworkID());
        System.out.println("inside network ID: " + virtualNetworkID);        
        GlobalAssert.that(virtualNetwork.getvNetworkID() == virtualNetworkID);
        this.virtualNetwork = virtualNetwork;
    }

    public long getVirtualNetworkID() {
        return virtualNetworkID;
    }

    public int getNumbertimeSteps() {
        GlobalAssert.that(dayduration % numberTimeSteps == 0);
        return numberTimeSteps;
    }
    
    public int getdt(){
        GlobalAssert.that(dayduration % numberTimeSteps == 0);
        return dayduration/numberTimeSteps;
    }



    /**
     * Perform consistency checks after completion of constructor operations.
     */
    public void checkConsistency() {
        // row-stochasticity of pij matrix
        for (int t = 0; t < numberTimeSteps; ++t) {
            for (int i = 0; i < virtualNetwork.getvNodesCount(); ++i) {
                Scalar sum = Total.of(pij.get(t, i)).Get();
                int sumInt = sum.number().intValue();
                GlobalAssert.that(sumInt == 1 || sumInt == 0);
            }
        }

    }
}








//@Deprecated
//public TravelData(VirtualNetwork virtualNetworkIn, File lambdaFile, File pijFile, File alphaijFile, long populationSize, int rebalancingPeriod)
//        throws JDOMException, IOException {
//    virtualNetwork = virtualNetworkIn;
//    virtualNetworkID = -1; // Must be wrong since unkknown with what virtualNetwork the XML was created.
//    this.populationSize = populationSize;
//    System.out.println("reading historic travel data");
//    {// arrival rates lambda
//        SAXBuilder builder = new SAXBuilder();
//        Document document = builder.build(lambdaFile);
//        Element rootNode = document.getRootElement();
//        List<Element> children = rootNode.getChildren();
//
//        // get number of time steps and time step size
//        numberTimeSteps = children.get(0).getChildren().size();
//        Attribute attribute = rootNode.getAttribute("dt");
//        dt = secfromStringTime(attribute.getValue());
//
//        // construct matrices based on xml
//        // the lambdas are normalized of their population size and time bin width
//        Scalar factor = RealScalar.of(populationSize * rebalancingPeriod);
//        lambda = Tensors.matrix((i, j) -> getLambdafromFile(i, j, rootNode).multiply(factor), numberTimeSteps, virtualNetwork.getvNodesCount());
//    }
//    {// transition probabilities pij
//        SAXBuilder builder = new SAXBuilder();
//        Document document = builder.build(pijFile);
//        Element rootNode = document.getRootElement();
//        // List<Element> children = rootNode.getChildren();
//
//        pij = Tensors.empty();
//        for (int k = 0; k < numberTimeSteps; ++k) {
//            final int timestep = k;
//            pij.append(Tensors.matrix((i, j) -> getpijfromFile(i, j, timestep, rootNode), virtualNetwork.getvNodesCount(),
//                    virtualNetwork.getvNodesCount()));
//        }
//    }
//    {// rebalancing rates alpha_ij
//        SAXBuilder builder = new SAXBuilder();
//        Document document = builder.build(alphaijFile);
//        Element rootNode = document.getRootElement();
//        // List<Element> children = rootNode.getChildren();
//
//        alphaijPSF = Tensors.empty();
//        for (int k = 0; k < numberTimeSteps; ++k) {
//            final int timestep = k;
//            alphaijPSF.append(Tensors.matrix((i, j) -> getalphaijfromFile(i, j, timestep, rootNode), virtualNetwork.getvNodesCount(),
//                    virtualNetwork.getvNodesCount()));
//        }
//    }
// @Deprecated
// private Scalar getLambdafromFile(int row, int col, Element rootNode) {
// List<Element> children = rootNode.getChildren();
//
// // get element with virtualNode col
// VirtualNode correctNode = virtualNetwork.getVirtualNode(col);
// Element vNodeelem = children.stream().filter(v ->
// v.getAttribute("id").getValue().toString().equals(correctNode.getId())).findFirst().get();
//
// // get element with number row
// Element timeatvNode = vNodeelem.getChildren().get(row);
//
// // return entry
// return RealScalar.of(Double.parseDouble(timeatvNode.getAttribute("lambda").getValue()));
// }
//
// @Deprecated
// private Scalar getpijfromFile(int from, int to, int timestep, Element rootNode) {
// List<Element> children = rootNode.getChildren();
//
// // get element with virtualLink col
// VirtualNode vNodeFrom = virtualNetwork.getVirtualNode(from);
// VirtualNode vNodeTo = virtualNetwork.getVirtualNode(to);
//
// Optional<Element> optional = children.stream().filter(v ->
// v.getAttribute("from").getValue().toString().equals(vNodeFrom.getId())
// && v.getAttribute("to").getValue().toString().equals(vNodeTo.getId())).findFirst();
//
// if (optional.isPresent()) {
// Element vLinkElem = optional.get();
// Element timeatvLink = vLinkElem.getChildren().get(timestep);
// return RealScalar.of(Double.parseDouble(timeatvLink.getAttribute("P_ij").getValue()));
// }
// GlobalAssert.that(false);
// return null; // <- this will never be returned since above GlobalAssert
// }
//
// @Deprecated
// private Scalar getalphaijfromFile(int from, int to, int timestep, Element rootNode) {
// List<Element> children = rootNode.getChildren();
//
// // get element with virtualLink col
// VirtualNode vNodeFrom = virtualNetwork.getVirtualNode(from);
// VirtualNode vNodeTo = virtualNetwork.getVirtualNode(to);
//
// Optional<Element> optional = children.stream().filter(v ->
// v.getAttribute("from").getValue().toString().equals(vNodeFrom.getId())
// && v.getAttribute("to").getValue().toString().equals(vNodeTo.getId())).findFirst();
//
// if (optional.isPresent()) {
// Element vLinkElem = optional.get();
// Element timeatvLink = vLinkElem.getChildren().get(timestep);
// return RealScalar.of(Double.parseDouble(timeatvLink.getAttribute("alpha_ij").getValue()));
// }
// GlobalAssert.that(false);
// return null; // <- this will never be returned since above GlobalAssert
// }

