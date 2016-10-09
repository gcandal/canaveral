package io.github.gcandal;


import org.junit.Test;

/**
 * Tests the {@link Service} class.
 */
public class ServiceTest {
    /**
     * Checks if {@link Service} verifies the timeout
     * value's validity.
     */
    @Test(expected = RuntimeException.class)
    public void testCyclicServiceManager() {
        Service service = new SleepingService("id");
        service.setTimeout(-1);
    }
}
