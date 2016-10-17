package net.onrc.openvirtex.elements.sla;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.openflow.protocol.OFMatch;

public final class SLAHandler {
    
    private static AtomicReference<SLAHandler> SLAInstance = new AtomicReference<>();

	private final Map<Integer, Integer> tenantSLAMap;
    private final Map<Integer, Integer> flowSLAMap;
    private final Map<Integer, HashMap<Long, Integer>> tenantSwitchSLAMap;
    
    private SLAHandler(){
    	this.tenantSLAMap = new HashMap<Integer, Integer>();
    	this.tenantSwitchSLAMap = new HashMap<Integer, HashMap<Long, Integer>>();
    	this.flowSLAMap = new HashMap<Integer, Integer>();
    }
    
    public void setTenantSLA(int tenantId, int sla){
    	this.tenantSLAMap.put(tenantId, sla);
    }
    
    public void setSwitchSLA(int tenantId, long switchId, int sla){
    	HashMap<Long, Integer> switchSLAMap = this.tenantSwitchSLAMap.get(tenantId);
    	if(switchSLAMap == null){
    		switchSLAMap = new HashMap<Long, Integer>();
    	}
    	switchSLAMap.put(switchId, sla);
    	this.tenantSwitchSLAMap.put(tenantId, switchSLAMap);
    }
    
    public void setFlowSLA(int flowId, int sla){
    	this.flowSLAMap.put(flowId, sla);
    }
    
    public void removeTenantSLA(int tenantId){
    	this.tenantSLAMap.remove(tenantId);
    }
    
    public void removeSwitchSLA(int tenantId, int switchId){
    	HashMap<Long, Integer> switchSLAMap = this.tenantSwitchSLAMap.get(tenantId);
    	switchSLAMap.remove(switchId);
    	this.tenantSwitchSLAMap.put(tenantId, switchSLAMap);
    }
    
    public void removeFlowSLA(int flowId){
    	this.flowSLAMap.remove(flowId);
    }
    
    public void processSLA(int tenantId, long switchId, int flowId, OFMatch ofmatch){
    	Integer tenantSLA, switchSLA, flowSLA;
    	tenantSLA = this.tenantSLAMap.get(tenantId);
    	switchSLA = this.tenantSwitchSLAMap.get(tenantId).get(switchId);
    	flowSLA = this.flowSLAMap.get(flowId);
    	
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
