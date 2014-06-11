package ds.ripple.server;

import java.io.IOException;
import java.util.Arrays;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import ds.ripple.common.PublisherRecord;
import ds.ripple.pub.util.DirectoryPublisher;
import ds.ripple.pub.util.MessageBuilder;

public class HWServer {
	private int id;
	private String name, reply;
	private Directory dir = new Directory();
	private DirectoryPublisher dirPub;
	
	private Context context;
	private Socket responder;
	
	private Thread server;
	
	private boolean isRunning = false;
	
	private static final byte PUBLISHER_REGISTRATION = 0x01;
	private static final byte PUBLISHER_DEREGISTRATION = 0x02;
	private static final byte PUBLISHER_INFO_UPDATE = 0x04;
	private static final byte OBSERVER_MAP_REQUEST = 0x03;
	
	public HWServer() {
		context = ZMQ.context(1);
		responder = context.socket(ZMQ.REP);
		responder.bind("tcp://*:5555");
		dirPub = new DirectoryPublisher(context, dir);
	}
	
	public void start() {
		isRunning = true;
		server = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (isRunning) {
					byte[] request = responder.recv(0);
					byte[] requestPayload = Arrays.copyOfRange(request, 1, request.length);
					byte requestHeader = request[0];
					switch (requestHeader) {
						case PUBLISHER_REGISTRATION:
							int pubId = dir.pubisherRegistration(requestPayload);
							try {
								responder.send(MessageBuilder.buildMsg(pubId), 0);
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println("New publisher registered!");
							if (pubId != Directory.ERROR_URL_ALREADY_EXISTS || pubId != Directory.ERROR_PUBLISHER_PARSING_ERROR) {
								dirPub.publish();
							}
							break;
						case PUBLISHER_DEREGISTRATION:
							String deregReplyCode = dir.publisherDeregistration(requestPayload);
							try {
								responder.send(MessageBuilder.buildMsg(reply), 0);
							} catch (IOException e) {
								e.printStackTrace();
							}
							if (deregReplyCode.equals(Directory.DEREGISTRATION_OK)) {
								dirPub.publish();
							}
							System.out.println("A publisher deregistered!");
							break;
						case PUBLISHER_INFO_UPDATE:
							String updateReplyCode = dir.updatePublisherInfo(requestPayload);
							try {
								responder.send(MessageBuilder.buildMsg(updateReplyCode), 0);
								dirPub.publish();
							} catch (IOException e) {
								e.printStackTrace();
							}
							break;
						case OBSERVER_MAP_REQUEST:
							try {
								responder.send(MessageBuilder.buildMsg(dir.getDirectoryList()), 0);
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println("Map was sent as a response to request!");
							break;
						default:
							// handle unrecognized commands somehow
							break;
					}
				}
				
				responder.close();
				context.term();
			}
		});
		server.start();
	}
	
	public void stop() {
		responder.close();
		isRunning = false;
	}
}