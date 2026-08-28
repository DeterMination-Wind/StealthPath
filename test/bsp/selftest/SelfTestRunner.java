package bsp.selftest;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Standalone JUnit runner used by the Gradle {@code selfTest} task. The
 * Gradle Test Executor cannot start on this machine (its worker classpath
 * contains non-ASCII user directories and breaks JVM bootstrap), so tests
 * run in-process via JavaExec instead — same JUnit 4 classes and assertions.
 */
public final class SelfTestRunner{
    public static void main(String[] args){
        Class<?>[] suites = {
            bsp.core.threat.ThreatModelTest.class,
            bsp.core.threat.ThreatGridTest.class,
            bsp.core.path.CostModelTest.class,
            bsp.core.path.AStarTest.class,
            bsp.core.path.LiquidPolicyTest.class,
            bsp.core.path.PathPlannerTest.class,
            bsp.core.path.WaypointCompactorTest.class,
            bsp.core.cluster.ClusterSplitterTest.class,
            bsp.core.power.PowerClusterFinderTest.class,
            bsp.core.geo.ChatCoordinateParserTest.class,
            bsp.core.geo.GridUtilsTest.class,
        };

        Result result = JUnitCore.runClasses(suites);
        System.out.println("betterStealthPath self-test: "
            + result.getRunCount() + " tests, "
            + result.getFailureCount() + " failures, "
            + result.getRunTime() + " ms");
        for(Failure f : result.getFailures()){
            System.out.println("FAILED: " + f.getTestHeader());
            System.out.println("  " + f.getMessage());
            System.out.println("  at " + f.getTrace().split("\n")[0]);
        }
        if(!result.wasSuccessful()){
            System.exit(1);
        }
    }
}
