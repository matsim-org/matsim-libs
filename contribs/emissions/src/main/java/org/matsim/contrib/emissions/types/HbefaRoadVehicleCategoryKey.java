package org.matsim.contrib.emissions.types;

import java.util.Objects;

public class HbefaRoadVehicleCategoryKey {
    private HbefaVehicleCategory hbefaVehicleCategory;
    private String hbefaRoadCategory;

    public HbefaRoadVehicleCategoryKey(){
    }

    public HbefaRoadVehicleCategoryKey(HbefaWarmEmissionFactorKey key) {
        this.hbefaVehicleCategory = key.getHbefaVehicleCategory();
        this.hbefaRoadCategory = key.getHbefaRoadCategory();
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
}
