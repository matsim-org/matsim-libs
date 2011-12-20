package org.matsim.contrib.freight.trade;

import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierContract;
import org.matsim.contrib.freight.carrier.CarrierOffer;

public class CarrierTradingAgentImpl implements CarrierTradingAgent {

    private static Logger logger;

    private Carrier carrier;

    private CostMemory costTable;

    private MarginalCostOfContractCalculator marginalCostOfContractCalculator;

    private Id id;

    @Override
    public void reset() {

    }

    @Override
    public void informOfferRejected(CarrierOffer offer) {
        logger.info("i am " + offer.getId() + " and my offer was rejected ;)). offer: " + offer.getPrice());
    }

    @Override
    public void informOfferAccepted(CarrierContract contract) {
        logger.info("i am " + contract.getOffer().getId() + " and my offer was accepted :)). offer: " + contract.getOffer().getPrice());
    }


    @Override
    public void informTSPContractAccepted(CarrierContract contract) {
        logger.info("i am " + contract.getOffer().getId() + " and this is my new contract :)). buyer: " + contract.getBuyer() + "; seller: " + contract.getSeller());
        carrier.getContracts().add(contract);
        carrier.getNewContracts().add(contract);
    }

    @Override
    public void informTSPContractCanceled(CarrierContract contract) {
        logger.info("i am " + contract.getOffer().getId() + " and my contract was canceled ;)). offer: " + contract.getOffer().getPrice());
        carrier.getContracts().remove(contract);
        carrier.getExpiredContracts().add(contract);
    }

    @Override
    public CarrierOffer requestOffer(Id from, Id to, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery) {
        CarrierOffer offer = new CarrierOffer();
        if(costTable.getCost(from, to, shipmentSize) != null) {
            offer.setPrice(Math.round(costTable.getCost(from, to, shipmentSize)));
        } else {
            double capacity = carrier.getCarrierCapabilities().getCarrierVehicles().iterator().next().getCapacity();
            double loadFactor = 0.5;
            double beeLineDistance = marginalCostOfContractCalculator.getBeeLineDistance(carrier.getDepotLinkId(), from, to);
            double costs = beeLineDistance * (shipmentSize/Math.max(shipmentSize, capacity*loadFactor));
            offer.setPrice(Math.round(costs));
        }
        offer.setId(id);
        return offer;
    }

    @Override
    public Id getId() {
        return id;
    }

}
