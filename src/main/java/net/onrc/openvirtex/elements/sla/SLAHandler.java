package net.onrc.openvirtex.elements.sla;

import java.util.HashMap;
import java.util.LinkedList;
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
    private final Map<Integer, HashMap<LinkedList<Integer>, Integer>> tenantServiceSLAMap;
    private final Map<Integer, HashMap<LinkedList<Integer>, Integer>> tenantFlowServiceSLAMap;
    
    private SLAHandler(){
    	this.tenantSLAMap = new HashMap<Integer, Integer>();
    	this.tenantSwitchSLAMap = new HashMap<Integer, HashMap<Long, Integer>>();
    	this.tenantFlowSLAMap = new HashMap<Integer, HashMap<Integer, Integer>>();
    	this.tenantServiceSLAMap = new HashMap<Integer, HashMap<LinkedList<Integer>, Integer>>();
    	this.tenantFlowServiceSLAMap = new HashMap<Integer, HashMap<LinkedList<Integer>, Integer>>();
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
    
    public void setServiceSLA(int tenantId, int srcPort, int dstPort, int sla){
    	this.log.info("Set Service SLA tenantid : {}, srcPort : {}, dstPort : {}, SLA_level : {}", tenantId, srcPort, dstPort, sla);
    	LinkedList<Integer> dualPort = new LinkedList<Integer>();
    	dualPort.add(srcPort);
    	dualPort.add(dstPort);
    	
    	HashMap<LinkedList<Integer>, Integer> serviceSLAMap = this.tenantServiceSLAMap.get(tenantId);
    	if(serviceSLAMap == null){
    		serviceSLAMap = new HashMap<LinkedList<Integer>, Integer>();
    	}
    	serviceSLAMap.put(dualPort, sla);
    	
    	LinkedList<Integer> reverseDualPort = new LinkedList<Integer>();
    	reverseDualPort.add(dstPort);
    	reverseDualPort.add(srcPort);
    	serviceSLAMap.put(reverseDualPort, sla);
    	this.tenantServiceSLAMap.put(tenantId, serviceSLAMap);
    }
    
    public void setFlowServiceSLA(int tenantId, int flowId, int srcPort, int dstPort, int sla){
    	this.log.info("Set Flow Service SLA tenantid : {}, flowId : {}, srcPort : {}, dstPort : {}, SLA_level : {}", tenantId, flowId, srcPort, dstPort, sla);
    	LinkedList<Integer> flowAndDualPort = new LinkedList<Integer>();
    	flowAndDualPort.add(flowId);
    	flowAndDualPort.add(srcPort);
    	flowAndDualPort.add(dstPort);
    	
    	HashMap<LinkedList<Integer>, Integer> serviceSLAMap = this.tenantFlowServiceSLAMap.get(tenantId);
    	if(serviceSLAMap == null){
    		serviceSLAMap = new HashMap<LinkedList<Integer>, Integer>();
    	}
    	serviceSLAMap.put(flowAndDualPort, sla);
    	
    	LinkedList<Integer> reverseflowAndDualPort = new LinkedList<Integer>();
    	reverseflowAndDualPort.add(flowId);
    	reverseflowAndDualPort.add(dstPort);
    	reverseflowAndDualPort.add(srcPort);
    	serviceSLAMap.put(reverseflowAndDualPort, sla);
    	this.tenantFlowServiceSLAMap.put(tenantId, serviceSLAMap);
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
    
    public int processSLA(int tenantId, long switchId, int flowId, OFMatch ofmatch){
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
    	
//    	SLAManager slaManager = new SLAManager();

    	if(!istenantSLA && !isSwitchSLA && !isflowSLA){
    		ofmatch.setWildcards((~OFMatch.OFPFW_IN_PORT) & (~OFMatch.OFPFW_DL_DST));
    		return 0;
    	}
    	
    	if(isSwitchSLA){
    		switchSLA = this.tenantSwitchSLAMap.get(tenantId).get(switchId);
//    		slaManager.SLArewriteMatch(ofmatch, switchSLA);
    		this.log.info("switchSLA setting.. tenantId : {}, switchId : {}, sla_level : {}", tenantId, switchId, switchSLA);
    		return switchSLA;
    	}
    	
    	if(isflowSLA){
    		flowSLA = this.tenantFlowSLAMap.get(tenantId).get(flowId);
//        	slaManager.SLArewriteMatch(ofmatch, flowSLA);
        	this.log.info("flowSLA setting.. flowId : {}, sla_level : {}", flowId, flowSLA);
        	return flowSLA;
    	}
    	
    	if(istenantSLA){
        	tenantSLA = this.tenantSLAMap.get(tenantId);
        	this.log.info("\norigin wildcard : {}", ofmatch.getWildcards());
//        	slaManager.SLArewriteMatch(ofmatch, tenantSLA);
        	this.log.info("\nprocessed wildcard : {}", ofmatch.getWildcards());
        	this.log.info("tenantSLA setting.. tenantId : {}, sla_level : {}", tenantId, tenantSLA);
        	return tenantSLA;
    	}	
    	
    	return 0;
    }
    
    public int processSLA(int tenantId, long switchId, int flowId, int srcPort, int dstPort, OFMatch ofmatch){
    	Integer tenantSLA, switchSLA, flowSLA, serviceSLA, flowServiceSLA;
    	this.log.info("processSLA.. tenantId : {}, switchId : {}, flowId : {}, srcPort : {}, dstPort : {}", tenantId, switchId, flowId, srcPort, dstPort);
    	boolean istenantSLA, isSwitchSLA, isflowSLA, isServiceSLA, isflowServiceSLA;
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
    	LinkedList<Integer> dualPort = new LinkedList<Integer>();
    	dualPort.add(srcPort);
    	dualPort.add(dstPort);
    	if(this.tenantServiceSLAMap.containsKey(tenantId)){
    		isServiceSLA = this.tenantServiceSLAMap.get(tenantId).containsKey(dualPort);
    	}else{
    		isServiceSLA = false;
    	}
    	
    	LinkedList<Integer> flowAndDualPort = new LinkedList<Integer>();
    	flowAndDualPort.add(flowId);
    	flowAndDualPort.add(srcPort);
    	flowAndDualPort.add(dstPort);
    	
    	if(this.tenantFlowServiceSLAMap.containsKey(tenantId)){
    		isflowServiceSLA = this.tenantFlowServiceSLAMap.get(tenantId).containsKey(flowAndDualPort);
    	}else{
    		isflowServiceSLA = false;
    	}
//    	SLAManager slaManager = new SLAManager();
    	
    	if(!istenantSLA && !isSwitchSLA && !isflowSLA && !isServiceSLA && !isflowServiceSLA){
    		ofmatch.setWildcards((~OFMatch.OFPFW_IN_PORT) & (~OFMatch.OFPFW_DL_DST));
    		return 0;
    	}
    	
    	if(isflowServiceSLA){
    		flowServiceSLA = this.tenantFlowServiceSLAMap.get(tenantId).get(flowAndDualPort);
    		this.log.info("flowServiceSLA setting.. tenantId : {}, flowId : {}, srcPort : {}, dstPort : {}, sla_level : {}", tenantId, flowId, srcPort, dstPort , flowServiceSLA);
    		return flowServiceSLA;
    	}
    	
    	if(isSwitchSLA){
    		switchSLA = this.tenantSwitchSLAMap.get(tenantId).get(switchId);
//    		slaManager.SLArewriteMatch(ofmatch, switchSLA);
    		this.log.info("switchSLA setting.. tenantId : {}, switchId : {}, sla_level : {}", tenantId, switchId, switchSLA);
    		return switchSLA;
    	}
    	
    	if(isflowSLA){
    		flowSLA = this.tenantFlowSLAMap.get(tenantId).get(flowId);
//        	slaManager.SLArewriteMatch(ofmatch, flowSLA);
        	this.log.info("flowSLA setting.. flowId : {}, sla_level : {}", flowId, flowSLA);
        	return flowSLA;
    	}
    	
    	if(isServiceSLA){
    		serviceSLA = this.tenantServiceSLAMap.get(tenantId).get(dualPort);
//			slaManager.SLArewriteMatch(ofmatch, serviceSLA);
        	this.log.info("flowSLA setting.. srcPort : {}, dstPort: {}, sla_level : {}", dualPort.get(0), dualPort.get(1), serviceSLA);
        	return serviceSLA;
    	}
    	
    	if(istenantSLA){
        	tenantSLA = this.tenantSLAMap.get(tenantId);
        	this.log.info("\norigin wildcard : {}", ofmatch.getWildcards());
//        	slaManager.SLArewriteMatch(ofmatch, tenantSLA);
        	this.log.info("\nprocessed wildcard : {}", ofmatch.getWildcards());
        	this.log.info("tenantSLA setting.. tenantId : {}, sla_level : {}", tenantId, tenantSLA);
        	return tenantSLA;
    	}
    	
    	return 0;
    }
    
    public static SLAHandler getInstance() {
        SLAHandler.SLAInstance.compareAndSet(null, new SLAHandler());
        return SLAHandler.SLAInstance.get();
    }
    
}
