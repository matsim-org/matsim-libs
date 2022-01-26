/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import java.util.Objects;

class HbefaRoadVehicleCategoryKey {
    private HbefaVehicleCategory hbefaVehicleCategory;
    private String hbefaRoadCategory;

    public HbefaRoadVehicleCategoryKey(){
    }

    public HbefaRoadVehicleCategoryKey(HbefaWarmEmissionFactorKey key) {
        this.hbefaVehicleCategory = key.getVehicleCategory();
        this.hbefaRoadCategory = key.getRoadCategory();
    }

    public HbefaVehicleCategory getHbefaVehicleCategory() {
        return hbefaVehicleCategory;
    }

    public String getHbefaRoadCategory() {
        return hbefaRoadCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HbefaRoadVehicleCategoryKey that = (HbefaRoadVehicleCategoryKey) o;
        return hbefaVehicleCategory.equals(that.hbefaVehicleCategory) &&
                Objects.equals(hbefaRoadCategory, that.hbefaRoadCategory);
    }

    @Override
    public int hashCode() {

        return Objects.hash(hbefaVehicleCategory, hbefaRoadCategory);
    }

    @Override
    public String toString() {
        return this.hbefaVehicleCategory + "---" + this.hbefaRoadCategory;
    }
}
