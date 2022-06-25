/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fhw;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Function;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Logger.getLogger;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * A simple servlet 3 as client that sends several messages to a queue or a topic.
 * </p>
 *
 * <p>
 * The servlet is registered and mapped to /HelloWorldMDBServletClient using the {@linkplain WebServlet
 * @HttpServlet}.
 * </p>
 *
 * @author Serge Pagop (spagop@redhat.com)
 *
 */
@WebServlet("/HelloWorldMDBServletClient")
public class HelloWorldMDBServletClient extends HttpServlet {

	private static final Logger logger = getLogger(HelloWorldMDBServletClient.class.getName());
	
    private static final long serialVersionUID = -8314035702649252239L;

    private static final int MSG_COUNT = 5;

    @Inject
    @JMSConnectionFactory("java:/amq/ConnectionFactory")
    private JMSContext context;
    
    private String jmsVersion;
    
    @Resource(lookup = "java:/amq/ConnectionFactory")
    private ConnectionFactory cf ; 

    @Resource(lookup = "java:/queue/simpleMDBTestQueue")
    private Queue queue;

    @PostConstruct
    protected void postConstruct() {
		try (Connection connection = cf.createConnection()) {
			final ConnectionMetaData cmd = connection.getMetaData();
			logger.info("------------------------------");
			if (cmd != null) { 
				jmsVersion = cmd.getJMSVersion();
				logger.info("jms version: "+jmsVersion);
			}
		} catch (JMSException ex) {
			logger.log(SEVERE, "Exception sending msg", ex);
		} finally {
			logger.info("------------end--------------");
		}
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.write("<h1>Quickstart: Example demonstrates the use of <strong>JMS 2.0</strong> and <strong>EJB 3.2 Message-Driven Bean</strong> in JBoss EAP.</h1>");
        try {
            out.write("<p>Sending messages to <em>" + queue + "</em></p>");
            out.write("<h2>The following messages will be sent to the destination:</h2>");
            for (int i = 0; i < MSG_COUNT; i++) {
                String text = "This is message " + (i + 1);
                sendMsg(text);
                out.write("Message (" + i + "): " + text + "</br>");
            }
            out.write("<p><i>Go to your JBoss EAP server console or server log to see the result of messages processing.</i></p>");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    protected void sendMsg(final String text) {
    	switch(jmsVersion) {
    	case "1.1":
    		sendMsgByJMS11(text);
    		break;
    	default:
    		 final JMSProducer producer = getJMSContext().createProducer();
    		 logger.info("jms version: "+jmsVersion);
    		 producer.send(queue, text);
    		break;    	
    	}
    }
    protected void sendMsgByJMS11(final String text) {
		sendMsgByJMS11(cf, s -> createTextMessage(s, text));
	}
    //https://docs.payara.fish/community/docs/documentation/user-guides/mdb-in-payara-micro.html
    protected void sendMsgByJMS11(final  ConnectionFactory cf,final Function<Session, Message> message) {
    	try (Connection connection = cf.createConnection()) {
            Session session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            session.createProducer(queue)
                   .send(message.apply(session));
        }catch (JMSException ex) {
            logger.log(SEVERE, "Exception sending msg", ex);
        }
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
		if (this.context != null) {
			return this.context;
		} else if(cf!=null) {
			return cf.createContext();
		}else {
			logger.info("Both ConnectionFactory & JMSContext are null.");
			return null;
		}
	}
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
