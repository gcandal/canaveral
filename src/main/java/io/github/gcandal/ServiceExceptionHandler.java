package io.github.gcandal;

/**
 * Used to log error messages issued by
 * uncaught Exceptions in {@link Service}s
 * with a custom format.
 */
class ServiceExceptionHandler implements Thread.UncaughtExceptionHandler {
    public void uncaughtException(Thread t, Throwable e) {
        if(!(t instanceof Service)) {
            return;
        }
        Service service = (Service) t;
        log(service, e.toString());
    }

    /**
     * Print a message prepended by the Service's ID
     * and Thread name.
     * @param service The service that triggered the exception.
     * @param message The message being printed.
     */
    private void log(Service service, String message) {
        System.err.println("Service[" + service.id + "]" + service.getName() + ": " + message);
    }
}
