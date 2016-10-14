package net.onrc.openvirtex.elements.sla;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.Wildcards;
import org.openflow.protocol.Wildcards.Flag;

public class SLAManager {
	final public static int Hop_no_isolation = 0;
	final public static int Hop_isolation = 1;
	final public static int Host_isolation = 2;
	final public static int Service_isolation = 3;
	
	
	public void SLArewriteMatch(OFMatch ofmatch, int sla_level){
		OFMatch match = ofmatch;
		Wildcards wcd = match.getWildcardObj();
		switch(sla_level){
			case Hop_no_isolation :
				
			case Hop_isolation :
				if(!wcd.isWildcarded(Flag.DL_SRC)){
					ofmatch.setWildcards(wcd.getInt() & (~OFMatch.OFPFW_DL_SRC));
				}
				
			case Host_isolation :
				ofmatch.setWildcards((~OFMatch.OFPFW_DL_SRC) & (~OFMatch.OFPFW_NW_DST_MASK)
						& (~OFMatch.OFPFW_NW_SRC_MASK) & (~OFMatch.OFPFW_DL_TYPE));
				
			case Service_isolation :
				ofmatch.setWildcards((~OFMatch.OFPFW_DL_SRC) & (~OFMatch.OFPFW_DL_TYPE)
						& (~OFMatch.OFPFW_NW_DST_MASK) & (~OFMatch.OFPFW_NW_SRC_MASK) & (~OFMatch.OFPFW_TP_DST)
						& (~OFMatch.OFPFW_TP_SRC));
		}
			
	}
}
