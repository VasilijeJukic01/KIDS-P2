package com.kids.app.servent;

import com.kids.app.AppConfig;
import com.kids.app.CausalBroadcast;
import com.kids.app.snapshot_bitcake.snapshot_collector.NullSnapshotCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollector;
import com.kids.app.snapshot_bitcake.snapshot_collector.SnapshotCollectorWorker;
import com.kids.app.snapshot_bitcake.SnapshotType;
import com.kids.cli.CLIParser;
import com.kids.servent.SimpleServentListener;
import com.kids.servent.message.util.FifoSendWorker;
import com.kids.servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the procedure for starting a single servent
 *
 * @author bmilojkovic
 */
public class ServentMain {

	/**
	 * Command line arguments are:
	 * 0 - path to servent list file
	 * 1 - this servent's id
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			AppConfig.timestampedErrorPrint("Please provide servent list file and id of this servent.");
		}
		
		int serventId = -1;
		int portNumber;
		
		String serventListFile = args[0];
		AppConfig.readConfig(serventListFile);
		
		try {
			serventId = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Second argument should be an int. Exiting...");
			System.exit(0);
		}
		
		if (serventId >= AppConfig.getServentCount()) {
			AppConfig.timestampedErrorPrint("Invalid servent id provided");
			System.exit(0);
		}
		
		AppConfig.myServentInfo = AppConfig.getInfoById(serventId);
		
		try {
			portNumber = AppConfig.myServentInfo.listenerPort();
			
			if (portNumber < 1000 || portNumber > 2000) {
				throw new NumberFormatException();
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Port number should be in range 1000-2000. Exiting...");
			System.exit(0);
		}
		
		MessageUtil.initializePendingMessages();
		
		AppConfig.timestampedStandardPrint("Starting servent " + AppConfig.myServentInfo);
		
		SnapshotCollector snapshotCollector;
		
		if (AppConfig.SNAPSHOT_TYPE == SnapshotType.NONE) {
			snapshotCollector = new NullSnapshotCollector();
		} else {
			snapshotCollector = new SnapshotCollectorWorker(AppConfig.SNAPSHOT_TYPE);
		}

		CausalBroadcast.getInstance().injectSnapshotCollector(snapshotCollector);

		Thread snapshotCollectorThread = new Thread(snapshotCollector);
		snapshotCollectorThread.start();
		
		SimpleServentListener simpleListener = new SimpleServentListener(snapshotCollector);
		Thread listenerThread = new Thread(simpleListener);
		listenerThread.start();

		List<FifoSendWorker> senderWorkers = new ArrayList<>();
		if (AppConfig.IS_FIFO) {
			for (Integer neighbor : AppConfig.myServentInfo.neighbors()) {
				FifoSendWorker senderWorker = new FifoSendWorker(neighbor);

				Thread senderThread = new Thread(senderWorker);
				senderThread.start();

				senderWorkers.add(senderWorker);
			}

		}
		
		CLIParser cliParser = new CLIParser(simpleListener, snapshotCollector, senderWorkers);
		Thread cliThread = new Thread(cliParser);
		cliThread.start();
		
	}
}
