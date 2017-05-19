/*******************************************************************************
 *     NervousnetCoreServer - A Core Server template which is part of the Nervousnet project
 *     sensor data, text messages and more.
 *
 *     Copyright (C) 2015 ETH Zürich, COSS
 *
 *     This file is part of Nervousnet.
 *
 *     Nervousnet is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nervousnet is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nervousnet. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * 	 *******************************************************************************/
package ch.ethz.coss.nervousnet.core.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.ethz.coss.nervousnet.core.Configuration;
import ch.ethz.coss.nervousnet.core.PulseWebSocketServer;
import ch.ethz.coss.nervousnet.core.utils.Log;

public class PulseConcurrentServer implements Runnable {

	private int sport = 0;
	private int webSocketPort = 0;
	private ServerSocket ssocket = null;
	private PulseWebSocketServer pWebSocketServer = null;
	private boolean stopped = false;
	private Thread runThread = null;
	private ExecutorService threadPool;
	private ConcurrentSocketWorkerFactory factory;

	public PulseConcurrentServer(int port, PulseWebSocketServer pWebSocketServer, int numThreads,
			ConcurrentSocketWorkerFactory factory) {
		this.sport = port;
		this.pWebSocketServer = pWebSocketServer;
		this.threadPool = Executors.newFixedThreadPool(numThreads);
		this.factory = factory;
	}

	@Override
	public void run() {
		synchronized (this) {
			runThread = Thread.currentThread();
		}

		createSocket();
		// createWebSocketServer();
		// pWebSocketServer.start();

		while (!isStopped()) {
			boolean success = false;
			Socket csocket = null;
			try {

				csocket = ssocket.accept();
				success = true;
			} catch (IOException e) {
				if (isStopped()) {
					Log.getInstance().append(Log.FLAG_INFO, "Connection refused: server is closing");
				} else {
					Log.getInstance().append(Log.FLAG_ERROR, "Connection refused: error accepting");
				}
				success = false;
			} catch (Exception e) {
				e.printStackTrace();
				if (isStopped()) {
					Log.getInstance().append(Log.FLAG_INFO, "Connection refused: server is closing");
				} else {
					Log.getInstance().append(Log.FLAG_ERROR, "Connection refused: error accepting");
				}
				success = false;
			}
			if (success) {
				try {
					this.threadPool.execute(factory.createWorker(csocket, pWebSocketServer));
				} catch (Exception e) {
					Log.getInstance().append(Log.FLAG_ERROR, "Threadpool execution failure");
				}
			}
		}
		threadPool.shutdown();
		Log.getInstance().append(Log.FLAG_INFO, "Server threading pool is shut down");
	}

	private synchronized boolean isStopped() {
		return stopped;
	}

	public synchronized void stop() {
		stopped = true;
		try {
			ssocket.close();

		} catch (IOException e) {
			Log.getInstance().append(Log.FLAG_ERROR, "Can't close the server on port: " + String.valueOf(sport));
		}

	}

	private synchronized void createSocket() {
		try {
			ssocket = new ServerSocket(sport);

			Configuration.getConfig().setServerIP(InetAddress.getLocalHost().getHostAddress());
			
			
			Log.getInstance().append(Log.FLAG_INFO,"Socket port = " + sport);
			Log.getInstance().append(Log.FLAG_INFO,"Pulse Server started on ip: " + ssocket.getLocalSocketAddress() + " and port: "
					+ ssocket.getLocalPort());
	
		} catch (IOException e) {
			stopped = true;
			Log.getInstance().append(Log.FLAG_ERROR, "Can't open the server on port: " + String.valueOf(sport));
		}
	}

}