package net.onrc.openvirtex.elements.datapath.statistics;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

import net.onrc.openvirtex.core.OpenVirteXController;
import net.onrc.openvirtex.elements.OVXMap;
import net.onrc.openvirtex.elements.datapath.PhysicalFlowEntry;
import net.onrc.openvirtex.elements.datapath.PhysicalSwitch;
import net.onrc.openvirtex.elements.network.PhysicalNetwork;
import net.onrc.openvirtex.messages.statistics.OVXFlowStatisticsReply;

public class StatisticRecoveryHandler implements TimerTask{

	private HashedWheelTimer timer = null;
	
	Logger log = LogManager.getLogger(StatisticRecoveryHandler.class.getName());
	
	private Integer refreshInterval = 30;
	private boolean stopTimer = false;
	
	public StatisticRecoveryHandler(){
		this.timer = PhysicalNetwork.getTimer();
		this.refreshInterval = OpenVirteXController.getInstance().getStatsRefresh();
	}
	
	@Override
	public void run(Timeout timeout) throws Exception {
		log.info("Recovery Physical Statistic to Virtual Statistic...");
		recoverStatic();
		if(!this.stopTimer){
			timeout.getTimer().newTimeout(this, refreshInterval,  TimeUnit.SECONDS);
		}
	}
	
	public void start(){
		log.info("Starting Stats recovery thread...");
		timer.newTimeout(this, 1, TimeUnit.SECONDS);
	}
	
	public void stop(){
		log.info("Stopping Stats recovery thread...");
		this.stopTimer = true;
	}
	
	public void recoverStatic(){
		Set<PhysicalSwitch> pswList = PhysicalNetwork.getInstance().getSwitches();
		Set<Integer> tenantList = OVXMap.getInstance().listVirtualNetworks().keySet();
		List<OVXFlowStatisticsReply> stats;
		OVXFlowStatisticsReply reply;
		for(int tid : tenantList){
			for(PhysicalSwitch psw : pswList){
				stats = psw.getFlowStats(tid);
				for(OVXFlowStatisticsReply rep : stats){
					rep.getCookie();
					Set<PhysicalFlowEntry> setEntry = psw.getEntrytable().getFlowEntry();
					for(PhysicalFlowEntry fe : setEntry){
						fe.getCookieSet().contains(rep.getCookie());
					}
				}
				
			}
		}
	}

}
