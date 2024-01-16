package org.matsim.contrib.drt.extension.operations.shifts;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.operations.shifts.io.DrtShiftsWriter;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecificationImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftsIOTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final String TESTSHIFTSINPUT  = "testShifts.xml";
	private static final String TESTXMLOUTPUT  = "testShiftsOut.xml";

	private final Id<DrtShift> sid11 = Id.create("11", DrtShift.class);
	private final Id<DrtShift> sid22 = Id.create("22", DrtShift.class);
	private final Id<DrtShift> sid33 = Id.create("33", DrtShift.class);
	private final Id<OperationFacility> oid1 = Id.create("op1", OperationFacility.class);
	private final Id<OperationFacility> oid2 = Id.create("op2", OperationFacility.class);


	@Test
	void testBasicReaderWriter() {

		DrtShiftsSpecification shiftsSpecification = new DrtShiftsSpecificationImpl();

		DrtShiftsReader reader = new DrtShiftsReader(shiftsSpecification);
		reader.readFile(utils.getPackageInputDirectory() + TESTSHIFTSINPUT);
		checkContent(shiftsSpecification);

		DrtShiftsWriter writer = new DrtShiftsWriter(shiftsSpecification);
		String outfilename = utils.getOutputDirectory() +  TESTXMLOUTPUT;
		writer.writeFile(outfilename);

		File outFile = new File(outfilename);
		assertTrue(outFile.exists());

		//read it again to check if the same is read as at the very first beginning of test
		shiftsSpecification = new DrtShiftsSpecificationImpl();
		reader = new DrtShiftsReader(shiftsSpecification);
		reader.readFile(outfilename);
		checkContent(shiftsSpecification);
	}

	private void checkContent(DrtShiftsSpecification shiftsSpecification) {
		assertEquals(3, shiftsSpecification.getShiftSpecifications().size());

		DrtShiftSpecification shiftSpecification1 = shiftsSpecification.getShiftSpecifications().get(sid11);
		assertNotNull(shiftSpecification1);
		assertEquals(sid11, shiftSpecification1.getId());
		assertEquals(14400., shiftSpecification1.getStartTime(), 0);
		assertEquals(45000., shiftSpecification1.getEndTime(), 0);
		assertTrue(shiftSpecification1.getOperationFacilityId().isPresent());
		assertEquals(oid1, shiftSpecification1.getOperationFacilityId().get());
		assertEquals(1800., shiftSpecification1.getBreak().orElseThrow().getDuration(), 0);
		assertEquals(28800., shiftSpecification1.getBreak().orElseThrow().getEarliestBreakStartTime(), 0);
		assertEquals(32400., shiftSpecification1.getBreak().orElseThrow().getLatestBreakEndTime(), 0);

		DrtShiftSpecification shiftSpecification2 = shiftsSpecification.getShiftSpecifications().get(sid22);
		assertNotNull(shiftSpecification2);
		assertEquals(sid22, shiftSpecification2.getId());
		assertEquals(18400., shiftSpecification2.getStartTime(), 0);
		assertEquals(49000., shiftSpecification2.getEndTime(), 0);
		assertTrue(shiftSpecification2.getOperationFacilityId().isPresent());
		assertEquals(oid2, shiftSpecification2.getOperationFacilityId().get());
		assertEquals(3600., shiftSpecification2.getBreak().orElseThrow().getDuration(), 0);
		assertEquals(29200., shiftSpecification2.getBreak().orElseThrow().getEarliestBreakStartTime(), 0);
		assertEquals(32800., shiftSpecification2.getBreak().orElseThrow().getLatestBreakEndTime(), 0);

		DrtShiftSpecification shiftSpecification3 = shiftsSpecification.getShiftSpecifications().get(sid33);
		assertNotNull(shiftSpecification3);
		assertEquals(sid33, shiftSpecification3.getId());
		assertEquals(22400., shiftSpecification3.getStartTime(), 0);
		assertEquals(53000., shiftSpecification3.getEndTime(), 0);
		assertFalse(shiftSpecification3.getOperationFacilityId().isPresent());
		assertEquals(Optional.empty(), shiftSpecification3.getOperationFacilityId());
		assertTrue(shiftSpecification3.getBreak().isEmpty());
	}
}
