package org.matsim.contrib.commercialTrafficApplications.jointDemand.commercialJob;

interface CommercialJobScoreCalculator {

    /**
     * @param timeDifference
     * @return a positive value for fulfilling the time window, a lower or negative value for not
     */
    double calcScore(double timeDifference);
}
