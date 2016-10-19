package net.onrc.openvirtex.elements.sla;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;

public final class SLAHandler {
	Logger log = LogManager.getLogger(SLAHandler.class.getName());
	
    private static AtomicReference<SLAHandler> SLAInstance = new AtomicReference<>();

	private final Map<Integer, Integer> tenantSLAMap;
	private final Map<Integer, HashMap<Integer, Integer>> tenantFlowSLAMap;
    private final Map<Integer, HashMap<Long, Integer>> tenantSwitchSLAMap;
    
    private SLAHandler(){
    	this.tenantSLAMap = new HashMap<Integer, Integer>();
    	this.tenantSwitchSLAMap = new HashMap<Integer, HashMap<Long, Integer>>();
    	this.tenantFlowSLAMap = new HashMap<Integer, HashMap<Integer, Integer>>();
    }
    
    public void setTenantSLA(int tenantId, int sla){
    	this.log.info("Set tenant SLA id : {}, sla_level : {}", tenantId, sla);
    	this.tenantSLAMap.put(tenantId, sla);
    }
    
    public void setSwitchSLA(int tenantId, long switchId, int sla){
    	this.log.info("Set Switch SLA tenantid : {}, switchId : {}, SLA_level : {}", tenantId, switchId, sla );
    	HashMap<Long, Integer> switchSLAMap = this.tenantSwitchSLAMap.get(tenantId);
    	if(switchSLAMap == null){
    		switchSLAMap = new HashMap<Long, Integer>();
    	}
    	switchSLAMap.put(switchId, sla);
    	this.tenantSwitchSLAMap.put(tenantId, switchSLAMap);
    }
    
    public void setFlowSLA(int tenantId, int flowId, int sla){
    	this.log.info("Set flow SLA flowId : {}, SLA_level : {}", flowId, sla);
    	HashMap<Integer, Integer> flowSLAMap = this.tenantFlowSLAMap.get(tenantId);
    	if(flowSLAMap ==null){
    		flowSLAMap = new HashMap<Integer, Integer>();
    	}
    	flowSLAMap.put(flowId, sla);
    	this.tenantFlowSLAMap.put(tenantId, flowSLAMap);
    }
    
    public void removeTenantSLA(int tenantId){
    	this.tenantSLAMap.remove(tenantId);
    }
    
    public void removeSwitchSLA(int tenantId, int switchId){
    	HashMap<Long, Integer> switchSLAMap = this.tenantSwitchSLAMap.get(tenantId);
    	switchSLAMap.remove(switchId);
    	this.tenantSwitchSLAMap.put(tenantId, switchSLAMap);
    }
    
    public void removeFlowSLA(int tenantId, int flowId){
    	HashMap<Integer, Integer> flowSLAMap = this.tenantFlowSLAMap.get(tenantId);
    	flowSLAMap.remove(flowId);
    	this.tenantFlowSLAMap.put(tenantId, flowSLAMap);
    }
    
    public void processSLA(int tenantId, long switchId, int flowId, OFMatch ofmatch){
    	Integer tenantSLA, switchSLA, flowSLA;
    	this.log.info("processSLA.. tenantId : {}, switchId : {}, flowId : {}", tenantId, switchId, flowId);
    	boolean istenantSLA, isSwitchSLA, isflowSLA;
    	istenantSLA = this.tenantSLAMap.containsKey(tenantId);
    	if(this.tenantSwitchSLAMap.containsKey(tenantId)){
    		isSwitchSLA = this.tenantSwitchSLAMap.get(tenantId).containsKey(switchId);
    	}else{
    		isSwitchSLA = false;
    	}
    	if(this.tenantFlowSLAMap.containsKey(tenantId)){
    		isflowSLA = this.tenantFlowSLAMap.get(tenantId).containsKey(flowId);
    	}else{
    		isflowSLA = false;
    	}
    	
    	SLAManager slaManager = new SLAManager();
    	
    	if(!istenantSLA & !isSwitchSLA & !isflowSLA){
    		ofmatch.setWildcards((~OFMatch.OFPFW_IN_PORT) & (~OFMatch.OFPFW_DL_DST));
    		return ;
    	}
    	
    	if(isSwitchSLA){
    		switchSLA = this.tenantSwitchSLAMap.get(tenantId).get(switchId);
    		slaManager.SLArewriteMatch(ofmatch, switchSLA);
    		this.log.info("switchSLA setting.. tenantId : {}, switchId : {}, sla_level : {}", tenantId, switchId, switchSLA);
    		return;
    	}
    	
    	if(isflowSLA){
    		flowSLA = this.tenantFlowSLAMap.get(tenantId).get(flowId);
        	slaManager.SLArewriteMatch(ofmatch, flowSLA);
        	this.log.info("flowSLA setting.. flowId : {}, sla_level : {}", flowId, flowSLA);
        	return;
    	}
    	if(istenantSLA){
        	tenantSLA = this.tenantSLAMap.get(tenantId);
        	this.log.info("\norigin wildcard : {}", ofmatch.getWildcards());
        	slaManager.SLArewriteMatch(ofmatch, tenantSLA);
        	this.log.info("\nprocessed wildcard : {}", ofmatch.getWildcards());
        	this.log.info("tenantSLA setting.. tenantId : {}, sla_level : {}", tenantId, tenantSLA);
        	return;
    	}	
    }
    
    public static SLAHandler getInstance() {
        SLAHandler.SLAInstance.compareAndSet(null, new SLAHandler());
        return SLAHandler.SLAInstance.get();
    }
    
}
