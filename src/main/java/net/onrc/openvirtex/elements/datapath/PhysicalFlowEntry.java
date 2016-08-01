package net.onrc.openvirtex.elements.datapath;

import java.util.HashSet;
import java.util.Set;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionType;

import net.onrc.openvirtex.messages.OVXFlowMod;
import net.onrc.openvirtex.messages.actions.OVXActionOutput;
import net.onrc.openvirtex.protocol.OVXMatch;

public class PhysicalFlowEntry {
	
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
		
	}
	public boolean checkduplicate(OVXFlowMod fm){
		OVXMatch match = (OVXMatch) fm.getMatch();
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
			
			if(oldMatch.getWildcards() == newWcd){
				if(oldMatch.getDataLayerDestination() == match.getDataLayerDestination()
						&& oldMatch.getDataLayerSource() == match.getDataLayerSource()){
					if(outport == oldoutport){
						return true;
					}else{
						match.setWildcards(newWcd & (~OFMatch.OFPFW_NW_DST_ALL) 
													& (~OFMatch.OFPFW_NW_SRC_ALL) 
													& (~OFMatch.OFPFW_DL_TYPE));
						fm.setMatch(match);
						return false;
					}
				}
			}
		}
		
		entry.add(new EntryPair(match, outaction));
		return false;
	}
}

