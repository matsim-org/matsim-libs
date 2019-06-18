package commercialtraffic.scoring;

public interface DeliveryScoreCalculator {

    /**
     * @param timeDifference
     * @return a positive value for fulfilling the time window, a lower or negative value for not
     */
    double calcScore(double timeDifference);
}
