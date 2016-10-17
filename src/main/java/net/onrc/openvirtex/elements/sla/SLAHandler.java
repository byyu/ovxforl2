package net.onrc.openvirtex.elements.sla;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.openflow.protocol.OFMatch;

public final class SLAHandler {
    
    private static AtomicReference<SLAHandler> SLAInstance = new AtomicReference<>();

	private final Map<Integer, Integer> tenantSLAMap;
    private final Map<Long, Integer> switchSLAMap;
    private final Map<Integer, Integer> flowSLAMap;
    
    private SLAHandler(){
    	this.tenantSLAMap = new HashMap<Integer, Integer>();
    	this.switchSLAMap = new HashMap<Long, Integer>();
    	this.flowSLAMap = new HashMap<Integer, Integer>();
    }
    
    public void setTenantSLA(int tenantId, int sla){
    	this.tenantSLAMap.put(tenantId, sla);
    }
    
    public void setSwitchSLA(long switchId, int sla){
    	this.switchSLAMap.put(switchId, sla);
    }
    
    public void setFlowSLA(int flowId, int sla){
    	this.flowSLAMap.put(flowId, sla);
    }
    
    public void removeTenantSLA(int tenantId){
    	this.tenantSLAMap.remove(tenantId);
    }
    
    public void removeSwitchSLA(int switchId){
    	this.switchSLAMap.remove(switchId);
    }
    
    public void removeFlowSLA(int flowId){
    	this.flowSLAMap.remove(flowId);
    }
    
    public void processSLA(int tenantId, long switchId, int flowId, OFMatch ofmatch){
    	Integer tenantSLA, switchSLA, flowSLA;
    	tenantSLA = tenantSLAMap.get(tenantId);
    	switchSLA = switchSLAMap.get(switchId);
    	flowSLA = flowSLAMap.get(flowId);
    	
    	SLAManager slaManager = new SLAManager();
    	
    	if(tenantSLA == null && switchSLA == null && flowSLA == null){
    		return ;
    	}
    	
    	if(switchSLA !=null){
    		slaManager.SLArewriteMatch(ofmatch, switchSLA);
    		return;
    	}
    	
    	if(flowSLA != null){
        	slaManager.SLArewriteMatch(ofmatch, flowSLA);
        	return;
    	}
    	if(tenantSLA != null){
        	slaManager.SLArewriteMatch(ofmatch, tenantSLA);
        	return;
    	}

    	
    	
    }
    public static SLAHandler getInstance() {
        SLAHandler.SLAInstance.compareAndSet(null, new SLAHandler());
        return SLAHandler.SLAInstance.get();
    }
    
}
