package org.amqphub.jca.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import java.util.logging.Logger;

@Singleton
@ApplicationPath("/api")
@Path("/")
public class ExampleApplication extends Application {
    private static final Logger log = Logger.getLogger(ExampleApplication.class.getName());

    
    private String jmsVersion;
    
    @Resource(lookup = "java:/jms/QpidJMSXA")
    private ConnectionFactory cf ;
    
    @Inject
    @JMSConnectionFactory( "java:/jms/QpidJMSXA")
    private JMSContext jmsContext;

    @Resource(lookup = "java:/queue/example/requests")
    private Queue queue;

    
    BlockingQueue<String> responses = new LinkedBlockingQueue<>();
    
    
    @PostConstruct
    protected void postConstruct() { 
		try (Connection connection = cf.createConnection()) {
			final ConnectionMetaData cmd = connection.getMetaData();
			log.info("------------------------------");
			if (cmd != null) { 
				jmsVersion = cmd.getJMSVersion();
				log.info("jms version: "+jmsVersion);
			}
		} catch (JMSException ex) {
			log.log(SEVERE, "Exception sending msg", ex);
		} finally {
			log.info("------------end--------------");
		}
    }

    @POST
    @Path("/send-request")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public synchronized String sendRequest(String text) {
        log.info("Sending request message");

        try { 
             
            return sendMsg(text) + "\n";
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    @Path("/receive-response")
    @Produces(MediaType.TEXT_PLAIN)
    public String receiveResponse() {
        log.info("Receiving response message");

        String response;

        try {
            response = responses.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        return response + "\n";
    }
    protected String sendMsg(final String text) throws JMSException {
    	final JMSContext jmsContext = getJMSContext() ;
    	String result =null;
    	switch(jmsVersion) {
    	case "1.1":
    		result =sendMsgByJMS11(text);
    		break;
    	default:    		  
    		 final JMSProducer producer = jmsContext.createProducer();
    		 final TextMessage message = jmsContext.createTextMessage();
    		  message.setText(text);
    		 log.info("jms version: "+jmsVersion);
    		 producer.send(queue, message);
    		 result = message.getJMSMessageID();
    		break;    	
    	}
    	return result;
    }
    protected String sendMsgByJMS11(final String text) {
		return sendMsgByJMS11(cf, s -> createTextMessage(s, text));
	}
    //https://docs.payara.fish/community/docs/documentation/user-guides/mdb-in-payara-micro.html
    protected String sendMsgByJMS11(final  ConnectionFactory cf,final Function<Session, Message> message) {
    	try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            Message data = message.apply(session);
            session.createProducer(queue)
                   .send(data);
            return data.getJMSMessageID();
        }catch (JMSException ex) {
        	log.log(SEVERE, "Exception sending msg", ex);
        }
    	return null;
    }
    public TextMessage createTextMessage(Session session, String message) {
        try {
            return session.createTextMessage(message);
        }
        catch(JMSException e) {
            throw new IllegalStateException(e);
        }
    }
    /**
     * Because using wildfly-jar-maven-plugin ,we discover that  injecting JMSContext would be null.<br/>
     * we use JMS 1.1(ConnectionFactory) to get JMSContext.<br/>
     * Sending messages to ActiveMQ can be done via the JMS API. At the moment, ActiveMQ 5.x only supports the JMS 1.1 API.
     * **/
	protected JMSContext getJMSContext() {
		if (this.jmsContext != null) {
			return this.jmsContext;
		} else if(cf!=null) {
			return cf.createContext();
		}else {
			log.info("Both ConnectionFactory & JMSContext are null.");
			return null;
		}
	}
}
