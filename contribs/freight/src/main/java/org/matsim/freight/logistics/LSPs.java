/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;

public class LSPs {

  private final Map<Id<LSP>, LSP> lsps = new LinkedHashMap<>();

  public LSPs(Collection<LSP> lsps) {
    makeMap(lsps);
  }

  private void makeMap(Collection<LSP> lsps) {
    for (LSP c : lsps) {
      this.lsps.put(c.getId(), c);
    }
  }

  public Map<Id<LSP>, LSP> getLSPs() {
    return lsps;
  }
}
