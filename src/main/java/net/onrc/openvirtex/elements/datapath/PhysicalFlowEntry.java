package net.onrc.openvirtex.elements.datapath;

import java.util.Arrays;
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
		this.log.info("Entry size is {}", entry.size());
	}
	
	public void removeEntry(OVXMatch match, OVXActionOutput action){
		EntryPair newEntity = new EntryPair(match, action);
		for(EntryPair entity : entry){
			if(entity.equals(newEntity)){
//				this.log.info("This entity is removed");
				entry.remove(entity);
			}
		}
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
//			log.info("Compare two wildcard \n{}\n", oldMatch.getWildcards(), newWcd);
			if(Arrays.equals(oldMatch.getDataLayerDestination(), match.getDataLayerDestination())
					&& Arrays.equals(oldMatch.getDataLayerSource(), match.getDataLayerSource())){
//				log.info("Compare two Mac\n{}\t{}\n{}\t{}\n", oldMatch.getDataLayerDestination(), oldMatch.getDataLayerSource(),match.getDataLayerDestination(), match.getDataLayerSource());
//				log.info("\n{}\n{}",Arrays.equals(oldMatch.getDataLayerDestination(), match.getDataLayerDestination()),Arrays.equals(oldMatch.getDataLayerSource(), match.getDataLayerSource()));
				if(outport == oldoutport){
					if(oldMatch.getWildcards() == newWcd){
						log.info("All condition is equal\n{}\n{}\t{}\n{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport);
//						entity.incCount();
						return true;
					}else{
						log.info("Need to change wcd");
						match.setWildcards(newWcd & (~OFMatch.OFPFW_NW_DST_ALL) 
													& (~OFMatch.OFPFW_NW_SRC_ALL) 
													& (~OFMatch.OFPFW_DL_TYPE));
						fm.setMatch(match);
						entry.add(new EntryPair(match, outaction));
//						log.info("All condition is equal but action is't equal\n{}\n{}\t{}\n{}\t{}",newWcd, match.getDataLayerSource(),match.getDataLayerDestination(), outport, oldoutport);
						return false;
					}
				}
			}
		}
		
		entry.add(new EntryPair(match, outaction));
		return false;
	}
}

