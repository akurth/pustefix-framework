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
 *
 */

package de.schlund.pfixcore.webservice.monitor;

/**
 * @author mleidig@schlund.de
 */
public class MonitorRecord {
	
	String service;
	String protocol;
    long startTime;
    long endTime;
    String reqMsg;
    String resMsg;
    
    public MonitorRecord() {
        
    }
    
    public String getService() {
    	return service;
    }
    
    public void setService(String service) {
    	this.service=service;
    }
    
    public String getProtocol() {
    	return protocol;
    }
    
    public void setProtocol(String protocol) {
    	this.protocol=protocol;
    }
    
    public long getStartTime() {
    	return startTime;
    }
    
    public void setStartTime(long startTime) {
    	this.startTime=startTime;
    }
    
    public long getEndTime() {
    	return endTime;
    }
    
    public void setEndTime(long endTime) {
    	this.endTime=endTime;
    }
    
    public long getTime() {
    	return endTime-startTime;
    }
    
    public String getRequestMessage() {
    	return reqMsg;
    }
    
    public void setRequestMessage(String reqMsg) {
    	this.reqMsg=reqMsg;
    }
    
    public String getResponseMessage() {
    	return resMsg;
    }
    
    public void setResponseMessage(String resMsg) {
    	this.resMsg=resMsg;
    }
    
}
