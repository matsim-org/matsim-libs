package sebhoerl;

import static org.junit.Assert.*;

import org.junit.Test;

import playground.sebhoerl.av.router.AVLinkSpeedMutator;

public class TestAVMutator {
    @Test
    public void test() {
        AVLinkSpeedMutator mutator = new AVLinkSpeedMutator("src/test/resources/sebhoerl/av_mutator.txt");
        System.out.println(mutator.mutateLinkSpeed(13.33, 19.0 * 3600));
    }
}
