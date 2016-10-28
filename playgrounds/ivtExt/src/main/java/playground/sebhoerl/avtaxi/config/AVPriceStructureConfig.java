package playground.sebhoerl.avtaxi.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVPriceStructureConfig extends ReflectiveConfigGroup {
    static final String PER_KM = "pricePerKm";
    static final String PER_MIN = "pricePerMin";
    static final String PER_TRIP = "pricePerTrip";
    static final String DAILY_SUBSCRIPTION_FEE = "dailySubscriptionFee";

    static final String TEMPORAL_BILLING_INTERVAL = "temporalBillingInterval";
    static final String SPATIAL_BILLING_INTERVAL = "spatialBillingInterval";

    private double pricePerKm = 0.0;
    private double pricePerMin = 0.0;
    private double pricePerTrip = 0.0;
    private double dailySubscriptionFee = 0.0;

    private double temporalBillingInterval = 1.0;
    private double spatialBillingInterval = 1.0;

    public AVPriceStructureConfig() {
        super("price_structure");
    }

    public static AVPriceStructureConfig createDefault() {
        return new AVPriceStructureConfig();
    }

    @StringGetter(PER_KM)
    public double getPricePerKm() {
        return pricePerKm;
    }

    @StringSetter(PER_KM)
    public void setPricePerKm(double pricePerKm) {
        this.pricePerKm = pricePerKm;
    }

    @StringGetter(PER_MIN)
    public double getPricePerMin() {
        return pricePerMin;
    }

    @StringSetter(PER_MIN)
    public void setPerMIn(double pricePerMin) {
        this.pricePerMin = pricePerMin;
    }

    @StringGetter(PER_TRIP)
    public double getPricePerTrip() {
        return pricePerTrip;
    }

    @StringSetter(PER_TRIP)
    public void setPricePerTrip(double pricePerTrip) {
        this.pricePerTrip = pricePerTrip;
    }

    @StringGetter(DAILY_SUBSCRIPTION_FEE)
    public double getDailySubscriptionFee() {
        return dailySubscriptionFee;
    }

    @StringSetter(DAILY_SUBSCRIPTION_FEE)
    public void setDailySubscriptionFee(double dailySubscriptionFee) {
        this.dailySubscriptionFee = dailySubscriptionFee;
    }

    @StringGetter(TEMPORAL_BILLING_INTERVAL)
    public double getTemporalBillingInterval() {
        return temporalBillingInterval;
    }

    @StringSetter(TEMPORAL_BILLING_INTERVAL)
    public void setTemporalBillingInterval(double temporalBillingInterval) {
        this.temporalBillingInterval = temporalBillingInterval;
    }

    @StringGetter(SPATIAL_BILLING_INTERVAL)
    public double getSpatialBillingInterval() {
        return spatialBillingInterval;
    }

    @StringSetter(SPATIAL_BILLING_INTERVAL)
    public void setSpatialBillingInterval(double spatialBillingInterval) {
        this.spatialBillingInterval = spatialBillingInterval;
    }
}
