package net.onrc.openvirtex.elements.datapath;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openflow.protocol.OFMatch;
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
	
	public PhysicalFlowEntry(OVXSwitch sw){
		this.sw = sw;
	}
	
	public void addEntry(OVXMatch match, OVXActionOutput action){
		EntryPair entity = new EntryPair(match, action);
		entry.add(entity);
	}
	
	public void removeEntry(OVXMatch match, OVXActionOutput action){
		EntryPair entity = new EntryPair(match,action);
		entry.remove(entity);
	}
	
	public boolean checkduplicate(OVXFlowMod fm){
		log.info("Start checking duplicate");
		
		OVXMatch match = new OVXMatch(fm.getMatch());
		int newWcd = match.getWildcards();
		log.info("this match is : {}",match.toString());
		OVXActionOutput outaction = null;
		short outport=0;
		for(OFAction action : fm.getActions()){
			if(action.getType()==OFActionType.OUTPUT){
	    		outaction = (OVXActionOutput) action;
	    		outport = outaction.getPort();
			}
		}
		log.info("This flowmod output is {}\n\n", outport);
		
		if(outport==0){
			return false;
		}
		OVXMatch oldMatch;
		short oldoutport;
		for(EntryPair entity : entry){
			oldMatch = entity.getMatch();
			oldoutport = entity.getAction().getPort();
			log.info("Compare two wildcard \n{}\n", oldMatch.getWildcards(), newWcd);
			if(oldMatch.getWildcards() == newWcd){
				log.info("Compare two Mac\n{}\t{}\n{}\t{}\n", oldMatch.getDataLayerDestination(), oldMatch.getDataLayerSource(),match.getDataLayerDestination(), match.getDataLayerSource());
				if(oldMatch.getDataLayerDestination().equals(match.getDataLayerDestination())
						&& oldMatch.getDataLayerSource().equals(match.getDataLayerSource())){
					if(outport == oldoutport){
						log.info("All condition is equal\n{}\n{}\t{}\n{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport);
						return true;
					}else{
						match.setWildcards(newWcd & (~OFMatch.OFPFW_NW_DST_ALL) 
													& (~OFMatch.OFPFW_NW_SRC_ALL) 
													& (~OFMatch.OFPFW_DL_TYPE));
						fm.setMatch(match);
						entry.add(new EntryPair(match, outaction));
						log.info("All condition is equal but action is't equal\n{}\n{}\t{}\n{}\t{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport, oldoutport);
						return false;
					}
				}
			}
		}
		
		entry.add(new EntryPair(match, outaction));
		return false;
	}
}

