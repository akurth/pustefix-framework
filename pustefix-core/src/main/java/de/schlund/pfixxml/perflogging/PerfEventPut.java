/*
 * Created on 30.05.2005
 *
 */
package de.schlund.pfixxml.perflogging;


import org.apache.log4j.Logger;

/**
 * @author jh
 * 
 */
class PerfEventPut {
    private static PerfEventPut instance = new PerfEventPut();
    private final static Logger LOG = Logger.getLogger(PerfEventPut.class);
    
    private BoundedBufferWrapper bBuffer;
    
    private PerfEventPut() {

    }
    
    static PerfEventPut getInstance() {
        return instance;
    }

    void setBuffer(BoundedBufferWrapper b) {
        bBuffer = b;
    }
    
    
    void logPerf(PerfEvent pe) {
        
        try {
            LOG.info("Putting ("+pe+") into buffer. Buffersize: "+bBuffer.size());
            boolean ok = bBuffer.offer(pe);
            LOG.info("Putting succeeded: "+ok);
          
        } catch (InterruptedException e) {
            LOG.warn(e);
        }
    }

   

  
}