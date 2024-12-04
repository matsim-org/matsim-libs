/*
  *********************************************************************** *
  * project: org.matsim.*
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  * copyright       :  (C) 2024 by the members listed in the COPYING,       *
  *                   LICENSE and WARRANTY file.                            *
  * email           : info at matsim dot org                                *
  *                                                                         *
  * *********************************************************************** *
  *                                                                         *
  *   This program is free software; you can redistribute it and/or modify  *
  *   it under the terms of the GNU General Public License as published by  *
  *   the Free Software Foundation; either version 2 of the License, or     *
  *   (at your option) any later version.                                   *
  *   See also COPYING, LICENSE and WARRANTY file                           *
  *                                                                         *
  * ***********************************************************************
 */

package org.matsim.freight.logistics.examples.initialPlans;

import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;

/**
 * @author Kai Martins-Turner (kturner)
 */
class MyCarrierScorer implements CarrierScoringFunctionFactory {

  public ScoringFunction createScoringFunction(Carrier carrier) {
    SumScoringFunction sf = new SumScoringFunction();
    TakeJspritScore takeJspritScore = new TakeJspritScore(carrier);
    sf.addScoringFunction(takeJspritScore);
    return sf;
  }

  private class TakeJspritScore implements SumScoringFunction.BasicScoring {

    private final Carrier carrier;

    public TakeJspritScore(Carrier carrier) {
      super();
      this.carrier = carrier;
    }

    @Override
    public void finish() {}

    @Override
    public double getScore() {
      if (carrier.getSelectedPlan().getScore() != null) {
        return carrier.getSelectedPlan().getScore();
      }
      return Double.NEGATIVE_INFINITY;
    }
  }
}
