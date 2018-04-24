package testSuite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import adapterTests.CollectionAdapterTest;
import adapterTests.DistributionAdapterTest;
import adapterTests.FirstReloadAdapterTest;
import adapterTests.MainRunAdapterTest;
import adapterTests.SecondReloadAdapterTest;
import cascadingInfoTest.CascadingInfoTest;
import demandObjectTests.DemandObjectBuilderTest;
import lspCreationTests.CollectionLSPCreationTest;
import lspCreationTests.CompleteLSPCreationTest;
import lspMobsimTests.CollectionLSPMobsimTest;
import lspMobsimTests.CompleteLSPMobsimTest;
import lspMobsimTests.FirstReloadLSPMobsimTest;
import lspMobsimTests.MainRunLSPMobsimTest;
import lspMobsimTests.MainRunOnlyLSPMobsimTest;
import lspMobsimTests.SecondReloadLSPMobsimTest;
import lspPlanTests.CollectionLSPPlanTest;
import lspPlanTests.CompleteLSPPlanTest;
import lspReplanningTests.CollectionLSPReplanningTest;
import lspSchedulingTests.CollectionLSPSchedulingTest;
import lspSchedulingTests.CompleteLSPSchedulingTest;
import lspSchedulingTests.FirstReloadLSPSchedulingTest;
import lspSchedulingTests.MainRunLSPSchedulingTest;
import lspSchedulingTests.SecondReloadLSPSchedulingTest;
import lspScoringTests.CollectionLSPScoringTest;
import lspShipmentAssignmentTests.CollectionLSPShipmentAssigmentTest;
import lspShipmentAssignmentTests.CompleteLSPShipmentAssignerTest;
import lspShipmentTest.CollectionShipmentBuilderTest;
import lspShipmentTest.CompleteShipmentBuilderTest;
import lspShipmentTest.DistributionShipmentBuilderTest;
import requirementsCheckerTests.AssignerRequirementsTest;
import requirementsCheckerTests.TransferrerRequirementsTest;
import solutionElementTests.CollectionElementTest;
import solutionElementTests.DistributionElementTest;
import solutionElementTests.FirstReloadElementTest;
import solutionElementTests.MainRunElementTest;
import solutionElementTests.SecondReloadElementTest;
import solutionTests.CollectionSolutionTest;
import solutionTests.CompleteSolutionTest;
import testLSPWithCostTracker.CollectionTrackerTest;
import testMutualReplanning.MutualReplanningTest;
import testMutualreplanningWithOfferUpdate.MutualReplanningAndOfferUpdateTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	CollectionAdapterTest.class,
	DistributionAdapterTest.class,
	FirstReloadAdapterTest.class,
	MainRunAdapterTest.class,
	SecondReloadAdapterTest.class,
	CascadingInfoTest.class,
	DemandObjectBuilderTest.class,
	CompleteLSPCreationTest.class,
	CollectionLSPCreationTest.class,
	CollectionLSPMobsimTest.class,
	CompleteLSPMobsimTest.class,
	FirstReloadLSPMobsimTest.class,
	MainRunLSPMobsimTest.class,
	MainRunOnlyLSPMobsimTest.class,
	SecondReloadLSPMobsimTest.class,
	CompleteLSPPlanTest.class,
	CollectionLSPPlanTest.class,
	CollectionLSPReplanningTest.class,
	CollectionLSPSchedulingTest.class,
	CompleteLSPSchedulingTest.class,
	//FirstReloadLSPSchedulingTest.class,
	MainRunLSPSchedulingTest.class,
	SecondReloadLSPSchedulingTest.class,
	CollectionLSPScoringTest.class,
	CollectionShipmentBuilderTest.class,
	CompleteShipmentBuilderTest.class,
	DistributionShipmentBuilderTest.class,
	AssignerRequirementsTest.class,
	TransferrerRequirementsTest.class,
	CollectionElementTest.class,
	DistributionElementTest.class,
	FirstReloadElementTest.class,
	MainRunElementTest.class,
	SecondReloadElementTest.class,
	CompleteSolutionTest.class,
	CollectionSolutionTest.class,
	CollectionTrackerTest.class,
	MutualReplanningTest.class,
	MutualReplanningAndOfferUpdateTest.class,
	CollectionLSPShipmentAssigmentTest.class,
	CompleteLSPShipmentAssignerTest.class,
	
})

public class FTLabTestSuite {
	
}
