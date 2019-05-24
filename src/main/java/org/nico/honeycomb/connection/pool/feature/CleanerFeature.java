package org.nico.honeycomb.connection.pool.feature;

import java.sql.SQLException;
import java.util.concurrent.LinkedBlockingDeque;

import org.nico.honeycomb.connection.HoneycombConnection;
import org.nico.honeycomb.connection.pool.HoneycombConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CleanerFeature extends AbstractFeature{

    private Logger logger = LoggerFactory.getLogger(CleanerFeature.class);

    public CleanerFeature(boolean enable, long interval) {
       super(enable, interval);
    }

    @Override
    public void doing(HoneycombConnectionPool pool) {
        Thread t = new Thread() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(interval);
                        
                        LinkedBlockingDeque<HoneycombConnection> idleQueue = pool.getIdleQueue();
                        synchronized (idleQueue) {
                            logger.debug("Cleaner Model To Start {}", idleQueue.size());
                            idleQueue.stream().filter(c -> { return c.idleTime() > pool.getConfig().getMaxIdleTime(); }).forEach(c -> {
                                try {
                                    if(! c.isClosedActive() && c.idle()) {
                                        c.closeActive();
                                        pool.freeze(c);
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                } 
                            });
                            logger.debug("Cleaner Model To Finished {}", idleQueue.size());
                        }
                    }catch(Throwable e) {
                        logger.error("Cleaner happended error", e);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

}