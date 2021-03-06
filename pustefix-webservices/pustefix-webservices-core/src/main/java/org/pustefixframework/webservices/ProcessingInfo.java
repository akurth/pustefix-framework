/*
 * This file is part of Pustefix.
 *
 * Pustefix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Pustefix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Pustefix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.pustefixframework.webservices;


public class ProcessingInfo {

    String service;
    String method;
    long startTime;
    
    long invocTime=-1;
    
    long procTime=-1;
    
    public ProcessingInfo(String service,String method) {
        this.service=service;
        this.method=method;
    }
    
    public void setService(String service) {
        this.service=service;
    }
    
    public String getService() {
        return service;
    }
    
    public void setMethod(String method) {
        this.method=method;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setStartTime(long startTime) {
        this.startTime=startTime;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void startInvocation() {
        invocTime=System.currentTimeMillis();
    }
    public void endInvocation() {
        invocTime=System.currentTimeMillis()-invocTime;
    }
    
    public long getInvocationTime() {
        return invocTime;
    }
    
    public void startProcessing() {
        procTime=System.currentTimeMillis();
    }
    public void endProcessing() {
        procTime=System.currentTimeMillis()-procTime;
    }

    public long getProcessingTime() {
        return procTime;
    }
    
}
