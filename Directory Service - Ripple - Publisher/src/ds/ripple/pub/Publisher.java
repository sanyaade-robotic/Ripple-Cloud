package ds.ripple.pub;
import java.io.IOException;
import java.util.Arrays;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import ds.ripple.common.PublisherRecord;
import ds.ripple.pub.exceptions.TopicNotRegisteredException;
import ds.ripple.pub.exceptions.URLAlreadyExistsException;
import ds.ripple.pub.exceptions.URLNotFoundException;
import ds.ripple.pub.exceptions.URLParsingException;
import ds.ripple.pub.exceptions.UpdateFailedException;
import ds.ripple.pub.util.MessageBuilder;

/**
 * Publisher class provides API for publisher in the publish-subscribe pattern.
 * The Publisher class uses ZeroMQ libraries to communicate with Directory Services
 * ("REQ" socket type) and to publish messages ("PUB" socket type).
 * @author Pawel Grzebala
 */
public class Publisher {
	private final static int REQUEST_PORT = 5555;
	
	private PublisherRecord mPublisherRecord = new PublisherRecord();
	private Context mContext;
	private Socket mPubSocket, mReqSocket;
	private String mdsURL;
	
	private int mPublisherID;
	private boolean isRegistered = false; 
	
	/**
	 * Creates a Publisher object.
	 * @param publisherURL String that contains URL of the publisher, ex: tcp://localhost
	 * @param dsURL String that contain URL of the Directory Services, ex: tcp://192.168.0.1
	 * @param topic Specifies a topic of this publisher
	 * @param publisherName Name of this publisher
	 */
	public Publisher(String publisherURL, String dsURL, String topic,
			String publisherName) {
		mdsURL = dsURL;
		mPublisherRecord.setPub_address(publisherURL);
		mPublisherRecord.setPub_name(publisherName);
		mPublisherRecord.setTopics(new String[] { topic });
	}

	/**
	 * Creates a Publisher object.
	 * @param publisherURL String that contains URL of the publisher, ex: tcp://localhost
	 * @param dsURL String that contain URL of the Directory Services, ex: tcp://192.168.0.1
	 * @param topic Specifies topics of this publisher
	 * @param publisherName Name of this publisher
	 */
	public Publisher(String publisherURL, String dsURL, String[] topic,
			String publisherName) {
		mPublisherRecord.setPub_address(publisherURL);
		mPublisherRecord.setPub_name(publisherName);
		mPublisherRecord.setTopics(topic);
	}

	/**
	 * Registers this publisher in the Director Services. This function also opens
	 * ZeroMQ PUB socket on a random port that will be used to publish messages. 
	 * @throws URLAlreadyExistsException
	 * @throws URLParsingException
	 */
	public void register() throws URLAlreadyExistsException, URLParsingException{
		mContext = ZMQ.context(1);
		mPubSocket = mContext.socket(ZMQ.PUB);
		int port = mPubSocket.bindToRandomPort(mPublisherRecord.getPub_address());
		mPublisherRecord.setPub_address(mdsURL + ":" + port);
		
		mReqSocket = mContext.socket(ZMQ.REQ);
		mReqSocket.connect(mdsURL + ":" + REQUEST_PORT);
		
		mReqSocket.send(MessageBuilder.getRegisterMsg(mPublisherRecord), 0);
		byte[] reply = mReqSocket.recv(0);
		
		try {
			processRegisterReply(""+ (int)MessageBuilder.deserialize(reply));
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Checks the response from Directory Services to registration request. 
	 * @param reply
	 * @throws URLAlreadyExistsException
	 * @throws URLParsingException
	 */
	private void processRegisterReply(String reply) throws URLAlreadyExistsException, URLParsingException {
		int replyCode = Integer.parseInt(reply);
		if (replyCode >= 0) {
			mPublisherID = replyCode;
			mPublisherRecord.setPub_Id(replyCode);
			isRegistered = true;
		} else if (replyCode == URLAlreadyExistsException.ERROR_CODE) {
			throw new URLAlreadyExistsException("Error code: " + replyCode + ". Given URL is already registered at Directory Services.");
		} else if (replyCode == URLParsingException.ERROR_CODE) {
			throw new URLParsingException("Error code: " + replyCode + ". Directory Services couldn't process given URL");
		}
	}
	
	/**
	 * Deregisters this publisher from the Directory Services list. Also closes the ZeroMQ
	 * PUB socket. Does nothing if this publisher was not registered.
	 * @throws URLNotFoundException
	 */
	public void deregister() throws URLNotFoundException{
		if (!isRegistered) {
			return;
		}
		mReqSocket.send(MessageBuilder.getDeregisterMsg(String.valueOf(mPublisherID)), 0);
		isRegistered = false;
		String reply = mReqSocket.recv(0).toString();
		
		processDeregisterReply(reply);
		
		mReqSocket.close();
		mPubSocket.close();
		mContext.term();
	}
	
	/**
	 * Checks the response from Directory Services to deregistration request.
	 * @param reply
	 * @throws URLNotFoundException
	 */
	private void processDeregisterReply(String reply) throws URLNotFoundException {
		if (reply.equals("OK")) {
			// everything went good, do nothing
		} else if (reply.equals(URLNotFoundException.ERROR_CODE)) {
			throw new URLNotFoundException("Given URL was not found at Directory Services.");
		}
	}
	
	/**
	 * Publishes specified message under specified topic.
	 * @param topic Must be in the list of topics of this publisher
	 * @param message Cannot be null
	 * @throws TopicNotRegisteredException
	 * @throws IOException 
	 */
	public void publish(String topic, String message) throws TopicNotRegisteredException, IOException {
		if (message == null) {
			throw new NullPointerException("Message cannot be null");
		}
		if (topic == null) {
			throw new NullPointerException("Topic cannot be null");
		}
		if (!Arrays.asList(mPublisherRecord.getTopics()).contains(topic)) {
			throw new TopicNotRegisteredException(topic + " was not registered.");
		}
		mPubSocket.sendMore(topic);
		mPubSocket.send(MessageBuilder.serialize(message), 0);
	}
	
	/**
	 * Updates the list of topics of this publisher. The old list will be overridden 
	 * by the new one. The method is silent on success, throws UpdateFailedException
	 * on failure. 
	 * @param topics
	 * @throws UpdateFailedException
	 */
	public void updateTopicList(String[] topics) throws UpdateFailedException {
		mPublisherRecord.setTopics(topics);
		mReqSocket.send(MessageBuilder.getUpdateMsg(mPublisherRecord), 0);
		byte[] reply = mReqSocket.recv(0);
		
		try {
			processUpdateReply((String)MessageBuilder.deserialize(reply));
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Internal method that will process update reply from Directory Services 
	 * server
	 * @param reply
	 * @throws UpdateFailedException
	 */
	private void processUpdateReply(String reply) throws UpdateFailedException {
		if (reply.equals("OK")) {
			return;
		} else if (reply.equals("" + UpdateFailedException.ERROR_CODE)) {
			throw new UpdateFailedException("Errpr code: " + reply + ". Update failed.");
		}
	}
	
	/**
	 * Returns the publisher's registered topics.
	 * @return
	 */
	public String[] getTopics() {
		return mPublisherRecord.getTopics();
	}
}