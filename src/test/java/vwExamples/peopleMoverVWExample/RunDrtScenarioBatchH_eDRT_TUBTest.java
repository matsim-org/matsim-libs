package vwExamples.peopleMoverVWExample;

import org.junit.Test;

import vwExamples.peoplemoverVWExample.RunDrtScenarioBatchH_eDRT_TUB;

import java.io.IOException;

public class RunDrtScenarioBatchH_eDRT_TUBTest {

    @Test
    public void run() throws IOException {

        RunDrtScenarioBatchH_eDRT_TUB.run(100, 0, "input/vwintegrationtest");
    }
}