package org.matsim.freight.logistics;

import org.matsim.freight.carriers.CarrierConstants;

public abstract class LSPConstants extends CarrierConstants {

  public static final String CAPACITY_NEED_FIXED = "capacityNeedFixed";
  public static final String CAPACITY_NEED_LINEAR = "capacityNeedLinear";
  public static final String CHAIN_ID = "chainId";
  public static final String ELEMENT = "element";
  public static final String END_TIME = "endTime";
  public static final String FIXED_COST = "fixedCost";
  public static final String HUB = "hub";
  public static final String LOCATION = "location";
  public static final String LOGISTIC_CHAIN = "logisticChain";
  public static final String LOGISTIC_CHAINS = "logisticChains";
  public static final String LOGISTIC_CHAIN_ELEMENT = "logisticChainElement";
  public static final String LSP = "lsp";
  public static final String LSPS = "lsps";
  public static final String LSP_PLAN = "LspPlan";
  public static final String LSP_PLANS = "LspPlans";
  public static final String RESOURCES = "resources";
  public static final String RESOURCE_ID = "resourceId";
  public static final String SCHEDULER = "scheduler";
  public static final String SHIPMENT_PLAN = "shipmentPlan";
  public static final String SHIPMENT_PLANS = "shipmentPlans";
  public static final String START_TIME = "startTime";
  public static final String TYPE = "type";
  //Shipment activity types
  public static final String HANDLING = "HANDLING"; //"Handle" sounds a bit unclear in the German translation. Therefor we are breaking the unofficial naming convention here. KMT/KN jan'25
  public static final String LOAD = "LOAD";
  public static final String TRANSPORT = "TRANSPORT";
  public static final String UNLOAD = "UNLOAD";
}
