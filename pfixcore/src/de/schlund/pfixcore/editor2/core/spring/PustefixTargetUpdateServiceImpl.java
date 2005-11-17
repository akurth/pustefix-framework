/*
 * This file is part of PFIXCORE.
 *
 * PFIXCORE is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * PFIXCORE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PFIXCORE; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package de.schlund.pfixcore.editor2.core.spring;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import org.apache.log4j.Logger;

import de.schlund.pfixcore.lucefix.PfixReadjustment;
import de.schlund.pfixxml.targets.Target;
import de.schlund.pfixxml.targets.TargetGenerationException;

/**
 * Implementation of PageUpdateService using a Thread started upon construction.
 * This service generates all targets registered by using
 * {@link #registerTargetForInitialUpdate(Target)} in a background loop. After
 * the loop has completed the first time, there is a wait time of one second
 * between the generation of each target. Targets registered by using
 * {@link #registerTargetForUpdate(Target)} are generated with a higher priority
 * and without the wait time, but after being generated once, they are
 * automatically removed from the queue.
 * 
 * @author Sebastian Marsching <sebastian.marsching@1und1.de>
 */
public class PustefixTargetUpdateServiceImpl implements PustefixTargetUpdateService, Runnable {

    private ArrayList lowPriorityQueue;
    private ArrayList highPriorityQueue;
    private HashSet   targetList;
    private Object    lock;
    private boolean   firstRunDone;
    private boolean   waitingForRefill;
    private boolean   isEnabled        = false;
    private long      startupDelay     = 0;
    private long      highRunDelay     = 250;
    private long      firstRunDelay    = 250;
    private long      nthRunDelay      = 1000;
    private long      completeRunDelay = 600000;
    private Logger    LOG              = Logger.getLogger(this.getClass());
    
    public void setEnabled(boolean flag) {}

    public void setEnabledXXX(boolean flag) {

        LOG.debug("***** Target Updater currently enabled?: " + isEnabled);
        LOG.debug("***** New value: " + flag);

        this.isEnabled = flag;
        
        // Make sure sleeping thread is awakened
        // when service is enabled
        synchronized (this.lock) {
        	this.lock.notifyAll();
        }
    }

    public void setStartupDelay(long delay) {
        this.startupDelay = delay;
    }

    public void setHighRunDelay(long delay) {
        this.highRunDelay = delay;
    }

    public void setFirstRunDelay(long delay) {
        this.firstRunDelay = delay;
    }

    public void setNthRunDelay(long delay) {
        this.nthRunDelay = delay;
    }

    public void setCompleteRunDelay(long delay) {
        this.completeRunDelay = delay;
    }

    public PustefixTargetUpdateServiceImpl() {
        this.lowPriorityQueue  = new ArrayList();
        this.highPriorityQueue = new ArrayList();
        this.lock              = new Object();
        this.targetList        = new HashSet();
        this.firstRunDone      = false;
        this.waitingForRefill  = false;
    }

    public void init() {
        Thread thread = new Thread(this, "pustefix-target-update");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.setDaemon(true);
        thread.start();
    }

    public void registerTargetForUpdate(Target target) {
        if (target == null) {
            String msg = "Received null pointer as target!";
            LOG.warn(msg);
            return;
        }
        synchronized (this.lock) {
            LOG.debug("  + HighPrio target " + target.getFullName());
            this.highPriorityQueue.add(target);
            this.lock.notifyAll();
    	}
    }

    public void registerTargetForInitialUpdate(Target target) {
        if (target == null) {
            String msg = "Received null pointer as target!";
            LOG.warn(msg);
            return;
        }
        synchronized (this.lock) {
            if (!this.targetList.contains(target)) {
                // LOG.debug("  + LowPrio target " + target.getFullName());
                this.targetList.add(target);
                this.lowPriorityQueue.add(target);
                this.firstRunDone = false;
                this.lock.notifyAll();
            }
        } 
    }

    public void run() {
        // Wait for delay
        LOG.debug("*** Starting updater thread ***");
        if (this.startupDelay > 0) {
            long startTime = System.currentTimeMillis();
            long currentTime = System.currentTimeMillis();
            ;
            do {
                long waitTime = this.startupDelay + startTime - currentTime;
                Object waitLock = new Object();
                synchronized (waitLock) {
                    try {
                        waitLock.wait(waitTime);
                    } catch (InterruptedException e) {
                        // Ignore interruption
                    }
                }
                currentTime = System.currentTimeMillis();
            } while (currentTime < (startTime + this.startupDelay));
        }

        while (true) {
            LOG.info("*** Starting updater loop ***");
            ArrayList lowCopy;
            ArrayList highCopy;
            synchronized (this.lock) {
                lowCopy = (ArrayList) this.lowPriorityQueue.clone();
                highCopy = (ArrayList) this.highPriorityQueue.clone();
                this.highPriorityQueue.clear();
            }
            LOG.debug("*** Starting HighPrio loop");
            while (!highCopy.isEmpty()) {
                Target target = (Target) highCopy.get(0);
                try {
                    LOG.debug("  * Generating HighPrio target " + target.getFullName());
                    target.getValue();
                } catch (TargetGenerationException e) {
                    LOG.warn("*** Exception generating HP " + target.getFullName() + ": " + e.getMessage());
                }
                highCopy.remove(0);
                synchronized(this.lock) {
                    try {
                        this.lock.wait(this.highRunDelay);
                    } catch (InterruptedException e) {
                        LOG.debug("*** Interrupted while waiting in HighPrio loop");
                        // Ignore interruption and continue
                    }
                }
            }
            LOG.debug("*** End of HighPrio loop");

            // Do automatic regeneration only if enabled
            if (this.isEnabled) {
                // System.out.println("*** in low loop ***");
                LOG.debug("*** Starting LowPrio loop");
                while (!lowCopy.isEmpty()) {
                    Target target = (Target) lowCopy.get(0);
                    boolean needsUpdate;
                    try {
                        needsUpdate = target.needsUpdate();
                    } catch (Exception e) {
                        // Remove target from queue without generating it
                        LOG.warn("*** Exception checking LP " + target.getFullName() + ": " + e.getMessage());
                        lowCopy.remove(0);
                        synchronized (this.lock) {
                            this.lowPriorityQueue.remove(0);
                        }
                        continue;
                    }
                    try {
                        if (needsUpdate) {
                            LOG.debug("  * Generating LowPrio target " + target.getFullName());
                            target.getValue();
                        }
                    } catch (TargetGenerationException e) {
                        LOG.warn("*** Exception generating LP " + target.getFullName() + ": " + e.getMessage());
                    }
                    lowCopy.remove(0);
                    synchronized (this.lock) {
                        this.lowPriorityQueue.remove(0);
                        if (!this.highPriorityQueue.isEmpty()) {
                            LOG.debug("*** Leaving LP loop for doing HP targets");
                            break;
                        }

                        if (needsUpdate) {
                            // If a target has been generated, wait for some time.
                            long delay = nthRunDelay;
                            if (!this.firstRunDone) {
                                delay = firstRunDelay;
                            }
                            if (delay > 0) {
                                try {
                                    this.lock.wait(delay);
                                } catch (InterruptedException e) {
                                    LOG.debug("*** Interrupted while waiting after generating target in LowPrio loop");
                                }
                            }
                        }
                    }
                }
                LOG.debug("*** End of LowPrio loop");
            }

            synchronized (this.lock) {
                if (this.isEnabled) {
                    if (this.lowPriorityQueue.isEmpty() && !waitingForRefill) {
                        this.firstRunDone = true;

                        // All low priority targets (usually all targets)
                        // have been updated, so trigger regeneration of
                        // search index
                        PfixReadjustment.getInstance().readjust();

                        // Delay refill of low priority queue
                        // in order to keep down system load
                        this.waitingForRefill = true;
                        Runnable refillTool = new Runnable() {
                                public void run() {
                                    try {
                                        if (completeRunDelay > 0) {
                                            Thread.sleep(completeRunDelay);
                                        }
                                    } catch (InterruptedException e) {
                                        // Ignore
                                    }
                                    synchronized (lock) {
                                        LOG.debug("##### Adding target list to LowPrio queue");
                                        lowPriorityQueue.addAll(targetList);
                                        waitingForRefill = false;
                                        lock.notifyAll();
                                    }
                                }
                            };
                        Thread toolThread = new Thread(refillTool, "target-update-refill");
                        toolThread.start();
                    }
                }

                if (this.highPriorityQueue.isEmpty() && (!this.isEnabled || this.lowPriorityQueue.isEmpty())) {
                    try {
                        LOG.debug("*** Going to Sleep...\n");
                        this.lock.wait();
                    } catch (InterruptedException e) {
                        LOG.debug("*** Interrupted while sleeping...");
                        // Ignore exception
                    }
                } else {
                    LOG.debug("*** HP or LP queue still not empty - continue directly");
                }
            }
        }
    }
}
