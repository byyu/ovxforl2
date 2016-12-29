/*******************************************************************************
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.onrc.openvirtex.messages;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.address.IPAddress;
import net.onrc.openvirtex.elements.datapath.FlowTable;
import net.onrc.openvirtex.elements.datapath.OVXFlowTable;
import net.onrc.openvirtex.elements.datapath.OVXSwitch;
import net.onrc.openvirtex.elements.datapath.PhysicalFlowEntry;
import net.onrc.openvirtex.elements.host.Host;
import net.onrc.openvirtex.elements.link.OVXLink;
import net.onrc.openvirtex.elements.link.OVXLinkUtils;
import net.onrc.openvirtex.elements.port.OVXPort;
import net.onrc.openvirtex.elements.sla.SLAHandler;
import net.onrc.openvirtex.elements.sla.SLAManager;
import net.onrc.openvirtex.exceptions.ActionVirtualizationDenied;
import net.onrc.openvirtex.exceptions.DroppedMessageException;
import net.onrc.openvirtex.exceptions.NetworkMappingException;
import net.onrc.openvirtex.exceptions.UnknownActionException;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.messages.actions.VirtualizableAction;
import net.onrc.openvirtex.packet.Ethernet;
import net.onrc.openvirtex.protocol.OVXMatch;
import net.onrc.openvirtex.util.MACAddress;
import net.onrc.openvirtex.util.OVXUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionDataLayerSource;
import org.openflow.protocol.action.OFActionType;

public class OVXFlowMod extends OFFlowMod implements Devirtualizable {


    private final Logger log = LogManager.getLogger(OVXFlowMod.class.getName());

    private OVXSwitch sw = null;
    private final List<OFAction> approvedActions = new LinkedList<OFAction>();

    private long ovxCookie = -1;

    long startTime, endTime;
    int sla_level;
    @Override
    public void devirtualize(final OVXSwitch sw) {
    	sla_level = 0;
    	startTime = System.nanoTime();
        /* Drop LLDP-matching messages sent by some applications */
        if (this.match.getDataLayerType() == Ethernet.TYPE_LLDP) {
            return;
        }
        this.hardTimeout = 300;
        this.sw = sw;
        FlowTable ft = this.sw.getFlowTable();

        int bufferId = OVXPacketOut.BUFFER_ID_NONE;
        if (sw.getFromBufferMap(this.bufferId) != null) {
            bufferId = sw.getFromBufferMap(this.bufferId).getBufferId();
        }
        final short inport = this.getMatch().getInputPort();

        /* let flow table process FlowMod, generate cookie as needed */
        boolean pflag = ft.handleFlowMods(this.clone());

        /* used by OFAction virtualization */
        OVXMatch ovxMatch = new OVXMatch(this.match);
        ovxCookie = ((OVXFlowTable) ft).getCookie(this, false);
        ovxMatch.setCookie(ovxCookie);
        this.setCookie(ovxMatch.getCookie());
        
        /*If match received by controller has only mac address, write ip address on match */
        if(ovxMatch.getNetworkSource()==0 || ovxMatch.getNetworkDestination()==0){
        	IPAddress srcIP = getHostIP(getHostbyMACAddress(ovxMatch.getDataLayerSource()));
        	IPAddress dstIP = getHostIP(getHostbyMACAddress(ovxMatch.getDataLayerDestination()));
        	if(srcIP != null && dstIP != null){
        		ovxMatch.setNetworkSource(srcIP.getIp());
        		ovxMatch.setNetworkDestination(dstIP.getIp());
        	}
        }
        
        Integer flowId;
		try {
			flowId = sw
			        .getMap()
			        .getVirtualNetwork(sw.getTenantId())
			        .getFlowManager()
			        .getFlowId(this.match.getDataLayerSource(),
			                this.match.getDataLayerDestination());
			SLAHandler slaHandler = SLAHandler.getInstance();
			if(this.match.getTransportDestination()!=0 && this.match.getTransportSource()!=0){
				this.sla_level = slaHandler.processSLA(sw.getTenantId(), sw.getSwitchId(), flowId, this.match.getTransportSource(), this.match.getTransportDestination(), this.match);
			}else{
				this.sla_level = slaHandler.processSLA(sw.getTenantId(), sw.getSwitchId(), flowId, this.match);
			}
			
			this.log.info("Setting sla Level : {}\n", this.sla_level);
		} catch (NetworkMappingException e) {
            log.warn(
                    "OVXFlowMod. Error retrieving the network with id {} for flowMod {}. Dropping packet...",
                    this.sw.getTenantId(), this);
        } catch (DroppedMessageException e) {
            log.warn(
                    "OVXFlowMod. Error retrieving flowId in network with id {} for flowMod {}. Dropping packet...",
                    this.sw.getTenantId(), this);
        }
        

    	for (final OFAction act : this.getActions()) {		
    		try {
    			((VirtualizableAction) act).virtualize(sw,
    					this.approvedActions, ovxMatch);
    		} catch (final ActionVirtualizationDenied e) {
    			this.log.warn("Action {} could not be virtualized; error: {}",
    					act, e.getMessage());
    			ft.deleteFlowMod(ovxCookie);
    			sw.sendMsg(OVXMessageUtil.makeError(e.getErrorCode(), this), sw);
    			return;
    		} catch (final DroppedMessageException e) {
    			this.log.warn("Dropping flowmod {}", this);
    			ft.deleteFlowMod(ovxCookie);
    			// TODO perhaps send error message to controller
    			return;
    		}
    	}

    	if(sla_level == SLAManager.isolation){
    		List<OFAction> outActions = new LinkedList<OFAction>();
    		for(OFAction act : this.approvedActions){
    			if(act.getType() == OFActionType.OUTPUT){
    				outActions.add(act);
    			}
    		}
    		this.approvedActions.clear();
    		this.approvedActions.addAll(outActions);
    	}
    		

        final OVXPort ovxInPort = sw.getPort(inport);
        this.setBufferId(bufferId);

        if (ovxInPort == null) {
        	if (this.match.getWildcardObj().isWildcarded(Flag.IN_PORT)) {
        		/* expand match to all ports */
        		for (OVXPort iport : sw.getPorts().values()) {
        			int wcard = this.match.getWildcards()
        					& (~OFMatch.OFPFW_IN_PORT);
        			this.match.setWildcards(wcard);
        			prepAndSendSouth(iport, pflag);
        		}
        	} else {
        		this.log.error(
        				"Unknown virtual port id {}; dropping flowmod {}",
        				inport, this);
        		sw.sendMsg(OVXMessageUtil.makeErrorMsg(
        				OFFlowModFailedCode.OFPFMFC_EPERM, this), sw);
        		return;
        	}
        } else {
        	prepAndSendSouth(ovxInPort, pflag);
        }

        /* Measure processing flowmod time */
        if(match.getDataLayerType()==Ethernet.TYPE_IPV4){
        	endTime = System.nanoTime();
        	long elapseTime = endTime - startTime;
        	this.log.info("FlowMod processing Time :\t{}", elapseTime);
        }
    }

    private void prepAndSendSouth(OVXPort inPort, boolean pflag) {
        if (!inPort.isActive()) {
            log.warn("Virtual network {}: port {} on switch {} is down.",
                    sw.getTenantId(), inPort.getPortNumber(),
                    sw.getSwitchName());
            return;
        }
        this.getMatch().setInputPort(inPort.getPhysicalPortNumber());
        OVXMessageUtil.translateXid(this, inPort);
        
        //byyu
        PhysicalFlowEntry phyFlowEntry  = inPort.getPhysicalPort().getParentSwitch().getEntrytable();

        boolean isedgeOut=true;
        boolean duflag=false;
        
        //this.idleTimeout = 5;

        try {
        	if (inPort.isEdge()) {
        		if(sla_level != SLAManager.isolation){
        			//byyu
        			if(this.match.getNetworkProtocol()==0){
        				match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
        						& (~OFMatch.OFPFW_DL_SRC)
        						& (~OFMatch.OFPFW_DL_DST)
        						& (~OFMatch.OFPFW_DL_TYPE)
        						& (~OFMatch.OFPFW_NW_DST_MASK)
        						& (~OFMatch.OFPFW_NW_SRC_MASK));
        			}else{
        				match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
        						& (~OFMatch.OFPFW_DL_SRC)
        						& (~OFMatch.OFPFW_DL_DST)
        						& (~OFMatch.OFPFW_DL_TYPE)
        						& (~OFMatch.OFPFW_NW_DST_MASK)
        						& (~OFMatch.OFPFW_NW_SRC_MASK)
        						& (~OFMatch.OFPFW_TP_DST)
        						& (~OFMatch.OFPFW_TP_SRC)
        						& (~OFMatch.OFPFW_NW_PROTO));
        			}
        		}else{
        			this.approvedActions.add(0, new OFActionDataLayerSource(MACAddress.valueOf(sw.getTenantId()).toBytes()));
        			SLAManager slaManager = new SLAManager();
        			slaManager.SLArewriteMatch(this.match, sla_level);
        		}
        	} else {
        		//                IPMapper.rewriteMatch(sw.getTenantId(), this.match);
        		// TODO: Verify why we have two send points... and if this is
        		// the right place for the match rewriting
        		if (inPort != null
        				&& inPort.isLink()
        				&& (!this.match.getWildcardObj().isWildcarded(
        						Flag.DL_DST) || !this.match.getWildcardObj()
        						.isWildcarded(Flag.DL_SRC))) {
        			// rewrite the OFMatch with the values of the link
        			OVXPort dstPort = sw.getMap()
        					.getVirtualNetwork(sw.getTenantId())
        					.getNeighborPort(inPort);
        			OVXLink link = sw.getMap()
        					.getVirtualNetwork(sw.getTenantId())
        					.getLink(dstPort, inPort);
        			if (inPort != null && link != null) {
        				Integer flowId = sw
        						.getMap()
        						.getVirtualNetwork(sw.getTenantId())
        						.getFlowManager()
        						.getFlowId(this.match.getDataLayerSource(),
        								this.match.getDataLayerDestination());
        				OVXLinkUtils lUtils = new OVXLinkUtils(
        						sw.getTenantId(), link.getLinkId(), flowId, link.getSrcSwitch());

        				if(sla_level != SLAManager.isolation){
        					lUtils.rewriteMatch(this.getMatch());
        				}else{
        					this.match.setDataLayerSource(MACAddress.valueOf(sw.getTenantId()).toBytes());
        				}

        				//byyu
        				isedgeOut = isEdgeOutport();
        				if(isedgeOut){
        					this.setPriority((short)(this.priority+5));
        					match.setWildcards((OFMatch.OFPFW_ALL) & (~OFMatch.OFPFW_IN_PORT)
        							& (~OFMatch.OFPFW_DL_SRC)
        							& (~OFMatch.OFPFW_DL_DST)
        							& (~OFMatch.OFPFW_DL_TYPE)
        							& (~OFMatch.OFPFW_NW_DST_MASK)
        							& (~OFMatch.OFPFW_NW_SRC_MASK));
        					if(sla_level == SLAManager.isolation){
        						this.match.setDataLayerSource(MACAddress.valueOf(sw.getTenantId()).toBytes());
        					}
        				}else{
        					SLAManager slaManager = new SLAManager();
        					this.log.info("\n{}\n{}",this.match.toString(),this.match.getWildcards());
        					slaManager.SLArewriteMatch(this.match, sla_level);
        					this.setPriority(slaManager.SLAresettingPriority(this.priority, sla_level));
        					this.log.info("\n{}\n{}",this.match.toString(),this.match.getWildcards());
        				}
        			}
        		}
        	}
        	
        } catch (NetworkMappingException e) {
        	log.warn(
        			"OVXFlowMod. Error retrieving the network with id {} for flowMod {}. Dropping packet...",
        			this.sw.getTenantId(), this);
        } catch (DroppedMessageException e) {
        	log.warn(
        			"OVXFlowMod. Error retrieving flowId in network with id {} for flowMod {}. Dropping packet...",
        			this.sw.getTenantId(), this);
        }

        this.computeLength();

        if(!isedgeOut){
        	if((sla_level==SLAManager.Hop_isolation) || (sla_level==SLAManager.Hop_no_isolation)){
        		duflag = phyFlowEntry.checkduplicate(this);
        		this.log.info("DuFlag is {}\n\n", duflag);
        	}else{
        		duflag = false;
        		this.log.info("SLA level is not need to aggregation...");
        	}
        }

        this.log.info("duflag and pflag is {}, {}", duflag, pflag);

        //byyu
        if(!duflag && pflag){
        	
        	this.flags |= OFFlowMod.OFPFF_SEND_FLOW_REM;
        	sw.sendSouth(this, inPort);
        }
    }

    private void computeLength() {
        this.setActions(this.approvedActions);
        this.setLengthU(OVXFlowMod.MINIMUM_LENGTH);
        for (final OFAction act : this.approvedActions) {
            this.setLengthU(this.getLengthU() + act.getLengthU());
        }
    }

//    private void prependRewriteActions() {
//        if (!this.match.getWildcardObj().isWildcarded(Flag.NW_SRC)) {
//            final OVXActionNetworkLayerSource srcAct = new OVXActionNetworkLayerSource();
//            srcAct.setNetworkAddress(IPMapper.getPhysicalIp(sw.getTenantId(),
//                    this.match.getNetworkSource()));
//            this.approvedActions.add(0, srcAct);
//        }
//
//        if (!this.match.getWildcardObj().isWildcarded(Flag.NW_DST)) {
//            final OVXActionNetworkLayerDestination dstAct = new OVXActionNetworkLayerDestination();
//            dstAct.setNetworkAddress(IPMapper.getPhysicalIp(sw.getTenantId(),
//                    this.match.getNetworkDestination()));
//            this.approvedActions.add(0, dstAct);
//        }
//    }

    /**
     * @param flagbit
     *            The OFFlowMod flag
     * @return true if the flag is set
     */
    public boolean hasFlag(short flagbit) {
        return (this.flags & flagbit) == flagbit;
    }

    public OVXFlowMod clone() {
        OVXFlowMod flowMod = null;
        try {
            flowMod = (OVXFlowMod) super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Error cloning flowMod: {}", this);
        }
        return flowMod;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (this.match != null) {
            map.put("match", new OVXMatch(match).toMap());
        }
        LinkedList<Map<String, Object>> actions = new LinkedList<Map<String, Object>>();
        for (OFAction act : this.actions) {
            try {
                actions.add(OVXUtil.actionToMap(act));
            } catch (UnknownActionException e) {
                log.warn("Ignoring action {} because {}", act, e.getMessage());
            }
        }
        map.put("actionsList", actions);
        map.put("priority", String.valueOf(this.priority));
        return map;
    }

    public void setVirtualCookie() {
        long tmp = this.ovxCookie;
        this.ovxCookie = this.cookie;
        this.cookie = tmp;
    }

    //byyu
    private boolean isEdgeOutport(){
    	OVXPort outPort;
    	
		short outport = 0;
		if(this.getActions().size()==0){
			return false;
		}
	    
		for(final OFAction act : this.getActions()){
	    	if(act.getType()==OFActionType.OUTPUT){
	    		OVXActionOutput outact = (OVXActionOutput) act;
	    		outport = outact.getPort();
	    	}
	    }
		outPort = this.sw.getPort(outport);

		if(outPort.isEdge())
			return true;
		else
			return false;
    }
    
    //byyu
    private IPAddress getHostIP(Host host){
    	if(host!=null)
    		return host.getIp();
    	else
    		return null;
    }
    
    //byyu
    private Host getHostbyMACAddress(byte[] mac){
    	OVXMap map = OVXMap.getInstance();
    	
    	try {
			return map.getVirtualNetwork(sw.getTenantId()).getHost(MACAddress.valueOf(mac));
		} catch (NetworkMappingException e) {
			log.error(e);
		}
    	return null;
    }

}
