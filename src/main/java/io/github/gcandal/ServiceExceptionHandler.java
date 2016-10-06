package io.github.gcandal;

import java.util.logging.Logger;

public class ServiceExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Logger LOGGER = Logger.getLogger(ServiceManager.class.getName());
    public void uncaughtException(Thread t, Throwable e) {
        if(!(t instanceof Service)) {
            return;
        }
        Service service = (Service) t;
        LOGGER.severe("Service[" + service.id + "]: " + e);
    }
}
