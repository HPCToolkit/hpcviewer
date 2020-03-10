package edu.rice.cs.hpc.remote.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;

import com.jcraft.jsch.JSchException;
import edu.rice.cs.hpc.data.experiment.InvalExperimentException;
import edu.rice.cs.hpc.data.trace.TraceName;
import edu.rice.cs.hpc.remote.tunnel.LocalTunneling;
import edu.rice.cs.hpc.remote.tunnel.RemoteUserInfo;

import edu.rice.cs.hpc.traceviewer.data.controller.SpaceTimeDataController;
import edu.rice.cs.hpc.traceviewer.data.db.AbstractDBOpener;
import edu.rice.cs.hpc.traceviewer.data.db.DatabaseAccessInfo;
import edu.rice.cs.hpc.traceviewer.data.db.DatabaseAccessInfo.DatabaseField;
import edu.rice.cs.hpc.traceviewer.data.util.Constants;
import edu.rice.cs.hpc.traceviewer.data.util.Debugger;

/**
 * Handles the protocol and commands to set up the session with the server.
 * 
 * For more information on message structure, see protocol documentation at the end of RemoteDataReceiver
 * 
 * @author Philip Taffet
 *
 */
public class RemoteDBOpener extends AbstractDBOpener 
{
	// -----------------
	// constants
	// -----------------
	
	private static final int PROTOCOL_VERSION = 0x00010001;
	private static final String LOCALHOST = "localhost";

	// -----------------
	// static variables
	// -----------------
	// TODO: static variables are discouraged in Eclipse since
	// 		 it isn't suitable for multiple instances of applications
	// -----------------

	static private Socket serverConnection = null;
	static private LocalTunneling tunnelMain, tunnelXML;
	static private RemoteUserInfo remoteUserInfo;

	// -----------------
	// object variables
	// -----------------
	
	private final DatabaseAccessInfo connectionInfo;

	private DataOutputStream sender;
	private DataInputStream receiver;


	/**************
	 * constructor
	 * 
	 * @param connectionInfo
	 */
	public RemoteDBOpener(DatabaseAccessInfo connectionInfo) {
		this.connectionInfo = connectionInfo;
	}

	// --------------------------------------------------------------------------------------
	// override methods
	// --------------------------------------------------------------------------------------
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.db.AbstractDBOpener#openDBAndCreateSTDC(org.eclipse.ui.IWorkbenchWindow, 
	 * org.eclipse.jface.action.IStatusLineManager)
	 */
	public SpaceTimeDataController openDBAndCreateSTDC(
			IWorkbenchWindow window, IProgressMonitor statusMgr) 
			throws InvalExperimentException, Exception 
	{

		// --------------------------------------------------------------
		// step 1 : create a SSH tunnel for the main port if necessary
		// --------------------------------------------------------------
		
		int port = Integer.parseInt(connectionInfo.getField(DatabaseField.ServerPort));
		boolean use_tunnel = connectionInfo.isTunnelEnabled();
		String host = connectionInfo.getField(DatabaseField.ServerName);
		
		int num_attempts = 3;
		
		{
			if  (use_tunnel) {
				// we need to setup the SSH tunnel
				tunnelMain = createSSHTunnel(window, tunnelMain, port);
				host = LOCALHOST;
			}
			
			// --------------------------------------------------------------
			// step 2 : initial contact to the server.
			//			if there's no reply or I/O error, we quit
			// --------------------------------------------------------------
			
		    connectToServer(window, host, port);

			// --------------------------------------------------------------
			// step 3 : send OPEN information including the database to open
			// --------------------------------------------------------------
			
			if (sendOpenDB(connectionInfo.getField(DatabaseField.DatabasePath)))
			{
				// communication has been done successfully
				num_attempts   	 = 0;
			} else 
			{	// problem with the communication
				// try to reset the connection if we can solve this by resetting the channel
				remoteUserInfo 	 = null;
				tunnelMain     	 = null;
				serverConnection = null;
				
				num_attempts--;
			}
		} while (num_attempts > 0);

		// --------------------------------------------------------------
		// step 4 : Blocking waiting the reply from the server. 
		//			A better way is to wait a
		// --------------------------------------------------------------
		
		int traceCount;
		int messageTag = RemoteDataRetriever.waitAndReadInt(receiver);
		int xmlMessagePortNumber;
		int compressionType;
		TraceName[] valuesX;

		if (messageTag == Constants.DB_OK)// DBOK
		{
			xmlMessagePortNumber  = receiver.readInt();
			traceCount = receiver.readInt();
			compressionType = receiver.readInt();
			valuesX = formatTraceNames(traceCount);
			
		} else 
		{
			//If the message is not a DBOK, it must be a NODB 
			//Right now, the error code isn't used, but it is there for the future
			int errorCode = receiver.readInt();
			String errorMessage="The server could not find traces in the directory:\n"
                + connectionInfo.getDatabasePath() + "\nPlease select a directory that contains traces.\nError code: " + errorCode ;
			throw new IOException(errorMessage);
		}
		
		// --------------------------------------------------------------
		// step 5 : create a SSH tunnel for XML port if necessary
		// --------------------------------------------------------------
		
		Debugger.printDebug(2, "About to connect to socket "+ xmlMessagePortNumber + " at "+ System.nanoTime());
		
		if (use_tunnel &&  (port != xmlMessagePortNumber)) {
			// only create SSH tunnel if the XML socket has different port number
			tunnelXML = createSSHTunnel(window, tunnelXML, xmlMessagePortNumber);			
		}
		
		statusMgr.setTaskName("Receiving XML stream");
		
		InputStream xmlStream = getXmlStream(host, port, xmlMessagePortNumber);
		
		if (xmlStream == null) {//null if getting it failed
			String errorMessage="Error communicating with server:\nCould not receive XML stream. \nPlease try again.";
			throw new IOException(errorMessage);
		}

		// --------------------------------------------------------------
		// step 6 : prepare communication stream fir sending & receiving 
		// --------------------------------------------------------------
		
		RemoteDataRetriever dataRetriever = new RemoteDataRetriever(serverConnection,
				 window.getShell(), compressionType);
		
		SpaceTimeDataControllerRemote stData = new SpaceTimeDataControllerRemote(dataRetriever, window, 
				xmlStream, connectionInfo.getDatabasePath() + " on " + host, traceCount, valuesX, sender);

		sendInfoPacket(sender, stData);
		
		return stData;	
	}
	

	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.traceviewer.db.AbstractDBOpener#end()
	 */
	public void end() {
		try {
			// closing I/O and network connection
			sender.close();
			receiver.close();
			
			serverConnection.close();
			if (tunnelMain != null) {
				try {
					tunnelMain.disconnect();
				} catch (JSchException e) {
					System.err.println("Warning: Cannot close the SSH tunnel !");
					e.printStackTrace();
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// --------------------------------------------------------------------------------------
	// private methods
	// --------------------------------------------------------------------------------------
	
	/*****
	 * a wrapper for tunneling() function to throw an IOException
	 * 
	 * @param window : the reference of the current workbench window
	 * @param tunnel : the local tunnel, if the tunnel is null, we'll create a new one. Otherwise,
	 * 					just use this argument. The caller needs to assign with the return tunnel
	 * @param port   : the local and the remote port
	 *  
	 * @throws JSchException 
	 */
	private LocalTunneling createSSHTunnel(IWorkbenchWindow window, LocalTunneling tunnel, int port) 
			throws JSchException
	{
		final String sshTunnelUsername = connectionInfo.getField(DatabaseField.SSH_TunnelUsername);
		final String sshTunnelHostname = connectionInfo.getField(DatabaseField.SSH_TunnelHostname);
		final String serverName		   = connectionInfo.getField(DatabaseField.ServerName);
		if (tunnel == null)
		{
			if (remoteUserInfo == null)
			{
				remoteUserInfo = new RemoteUserInfo(window.getShell());
			}
			remoteUserInfo.setInfo(	sshTunnelUsername, 
					sshTunnelHostname, port);
			tunnel = new LocalTunneling(remoteUserInfo);
		}
		
		tunnel.connect(sshTunnelUsername, sshTunnelHostname, serverName, port);
		
		System.out.println("tunnel: " + tunnel);
		return tunnel;
	}
	

	
	/******
	 * 
	 * @param traceCount
	 * @return
	 * @throws IOException
	 */
	private TraceName[] formatTraceNames(int traceCount) throws IOException {
		TraceName[] names  = new TraceName[traceCount];
		for (int i = 0; i < names.length; i++) {
			int processID = receiver.readInt();
			int threadID = receiver.readShort();
			names[i] = new TraceName(processID, threadID);
		}
		return names;
	}

	
	/***************
	 * Get XML data from the server
	 * 
	 * @param serverURL
	 * @param port
	 * @param xmlMessagePortNumber
	 * 
	 * @return XML data in zipped format
	 * 
	 * @throws IOException
	 */
	private GZIPInputStream getXmlStream(String serverURL, int port, int xmlMessagePortNumber)
			throws IOException {

		byte[] compressedXMLMessage;
		DataInputStream dxmlReader;
		if (xmlMessagePortNumber == port)
		{
			dxmlReader = receiver;

		}
		else
		{
			Socket xmlConnection = new Socket();
			SocketAddress xmlAddress = new InetSocketAddress(serverURL, xmlMessagePortNumber);
			xmlConnection.connect(xmlAddress, 1000);
			BufferedInputStream buf = new BufferedInputStream(xmlConnection.getInputStream());
			dxmlReader = new DataInputStream(buf);
		}

		int exml = dxmlReader.readInt();
		if (exml != Constants.XML_HEADER) 
		{
			Debugger.printDebug(0,"Expected XML Message (" + Constants.XML_HEADER
					+ ")  on data socket, got " + exml);
			return null;
		}
		int size = dxmlReader.readInt();
		
		compressedXMLMessage = new byte[size];
		int numRead = 0;
		while (numRead < size)
		{
			numRead += dxmlReader.read(compressedXMLMessage, numRead, size- numRead);
		}
		GZIPInputStream xmlStream = new GZIPInputStream(new 
				ByteArrayInputStream(compressedXMLMessage));
		return xmlStream;
	}

	
	/*****************
	 * Try to connect to a remote server
	 * 
	 * @param window
	 * @param serverURL
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void connectToServer(IWorkbenchWindow window, String serverURL, int port) 
			throws UnknownHostException, IOException 
	{
		if (serverConnection != null && !serverConnection.isClosed()) 
		{
			InetSocketAddress addr = new InetSocketAddress(serverURL, port);
			SocketAddress sockAddr = serverConnection.getRemoteSocketAddress();
			
			if (sockAddr.equals(addr)) 
			{
				//Connecting to same server, don't do anything.
				initDataIOStream();
				return;
			} else {
				//Connecting to a different server
				//TraceDatabase.removeInstance(window);
			}
		}
		serverConnection = new Socket(serverURL, port);
		initDataIOStream();
		System.out.println("connect: " + serverConnection.getRemoteSocketAddress());
	}
	
	
	private void initDataIOStream() 
			throws IOException
	{
		sender = new DataOutputStream(new BufferedOutputStream(
				serverConnection.getOutputStream()));
		receiver = new DataInputStream(new BufferedInputStream(
				serverConnection.getInputStream()));
	}

	/*******
	 * sending info message to the server
	 * 
	 * @param _sender
	 * @param stData
	 * @throws IOException
	 *******/
	private void sendInfoPacket(DataOutputStream _sender,
			SpaceTimeDataControllerRemote stData) throws IOException {

		// The server
		// needs information (min & max time, etc.) that can only be gotten (as
		// far as I know) from the
		// XML processing that happens in the SpaceTimeDataController
		// constructor, so we construct it, get what we need, then pass in the
		// RemoteDataRetriever as soon as possible.

		/*
		 * Then: overallMinTime (long) overallMaxTime (long) headerSize (int)
		 */
		sender.writeInt(Constants.INFO);
		sender.writeLong(stData.getMinBegTime());
		sender.writeLong(stData.getMaxEndTime());
		sender.writeInt(stData.getHeaderSize());
		sender.flush();

	}

	
	/*******
	 * sending a message to the server to open a database
	 * 
	 * @param serverPathToDB
	 * 
	 * @throws 	SocketException when fail to send via SSH tunnel
	 * 			IOException for general cases
	 *******/
	private boolean sendOpenDB(String serverPathToDB) 
			throws IOException, SocketException 
			{
		sender.writeInt(Constants.OPEN);
		sender.writeInt(PROTOCOL_VERSION);
		int len = serverPathToDB.length();
		sender.writeShort(len);
		for (int i = 0; i < len; i++) {
			int charVal = serverPathToDB.charAt(i);
			if (charVal > 0xFF)
				System.out.println("Path to database cannot contain special characters");
			sender.writeByte(charVal);
		}
		
		try {
			// TODO Warning this flush may cause @see SocketException broken pipe
			sender.flush();
		} catch (SocketException e) {
			return false;
		}

		Debugger.printDebug(0,"Open database message sent");
		
		return true;
	}	
}


