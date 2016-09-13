package net.onrc.openvirtex.elements.datapath;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards.Flag;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;

public class PhysicalFlowEntry {
	
	private static Logger log = LogManager.getLogger(PhysicalFlowEntry.class
            .getName());
	private Set<EntryPair> entry = new HashSet<EntryPair>();
	OVXSwitch sw;
	PhysicalSwitch physw;
	public PhysicalFlowEntry(){
		
	}
	public PhysicalFlowEntry(PhysicalSwitch sw){
		this.physw = sw;
	}
	public PhysicalFlowEntry(OVXSwitch sw){
		this.sw = sw;
	}
	
	public void addEntry(OVXMatch match, OVXActionOutput action){
		EntryPair entity = new EntryPair(match, action, match.getCookie());
		entry.add(entity);
	}
	
	public List<Long> removeEntry(OVXMatch match, OVXActionOutput action){
		EntryPair newEntity = new EntryPair(match, action, match.getCookie());
		for(EntryPair entity : entry){
			if(entity.equals(newEntity)){
				List<Long> cookieList = entity.getCookieSet();
				entry.remove(entity);
				return cookieList;
			}
		}
		
		return null;
	}

	
	public boolean checkduplicate(OVXFlowMod fm){
		log.info("Start checking duplicate");
		OVXMatch match = new OVXMatch(fm.getMatch());
		match.setCookie(fm.getCookie());
		int newWcd = match.getWildcards();
		OVXActionOutput outaction = null;
		short outport=0;
		for(OFAction action : fm.getActions()){
			if(action.getType()==OFActionType.OUTPUT){
	    		outaction = (OVXActionOutput) action;
	    		outport = outaction.getPort();
			}
		}
		
		if(outport==0){
			return false;
		}
		OVXMatch oldMatch;
		short oldoutport;
		for(EntryPair entity : entry){
			oldMatch = entity.getMatch();
			oldoutport = entity.getAction().getPort();
			log.info("Compare condition : \nold : {}\t{}\nnew : {}\t{}", oldMatch.toString(), oldoutport, match.toString(), outport);
			if(Arrays.equals(oldMatch.getDataLayerDestination(), match.getDataLayerDestination())
					&& (oldMatch.getInputPort() == match.getInputPort())){

				if(outport == oldoutport){
					log.info("compare wildcard : \nold : {}\nnew : {}",oldMatch.getWildcards(), newWcd);
					if(oldMatch.getWildcards() == newWcd){
						log.info("compare info : \n{}\nold : {}\nnew : {}", oldMatch.getWildcardObj().isWildcarded(Flag.NW_DST), oldMatch.getNetworkDestination(), match.getNetworkDestination());
						if(!oldMatch.getWildcardObj().isWildcarded(Flag.NW_DST)){
							if(oldMatch.getNetworkDestination()==match.getNetworkDestination()){
								entity.addCookie(match.getCookie());
								return true;
							}else{
								return false;
							}
						}else{
							log.info("All condition is equal\n{}\n{}\t{}\n{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport);
							entity.addCookie(match.getCookie());
							return true;
						}
					}else{
						return false;
					}
				}else{
					log.info("Need to change wcd");
					match.setWildcards(newWcd & (~OFMatch.OFPFW_NW_DST_ALL)
												& (~OFMatch.OFPFW_NW_SRC_ALL)
												& (~OFMatch.OFPFW_DL_TYPE));
					short prio = fm.getPriority();
					fm.setPriority(++prio);
					fm.setMatch(match);

					addEntry(match,outaction);
					log.info("All condition is equal but action is't equal\n{}\n{}\t{}\n{}\t{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport, oldoutport);
					return false;
				}
			}
		}
		
		addEntry(match,outaction);
		return false;
	}
}

