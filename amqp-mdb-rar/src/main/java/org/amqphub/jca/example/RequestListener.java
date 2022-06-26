//package org.amqphub.jca.example;
//
//import javax.ejb.ActivationConfigProperty;
//import javax.ejb.MessageDriven;
//import javax.ejb.TransactionAttribute;
//import javax.ejb.TransactionAttributeType;
//import javax.inject.Inject;
//import javax.jms.JMSConnectionFactory;
//import javax.jms.JMSContext;
//import javax.jms.JMSException;
//import javax.jms.JMSProducer;
//import javax.jms.Message;
//import javax.jms.MessageListener;
//import javax.jms.Queue;
//import javax.jms.TextMessage;
//import static java.util.logging.Level.SEVERE;
//import static java.util.logging.Logger.getLogger;
//import java.util.logging.Logger;
//
//@MessageDriven(activationConfig = {
////        @ActivationConfigProperty(propertyName = "user", propertyValue = "example"),
////        @ActivationConfigProperty(propertyName = "password", propertyValue = "example"),
////        @ActivationConfigProperty(propertyName = "connectionFactory", propertyValue = "factory1"),
//        @ActivationConfigProperty(propertyName = "destination", propertyValue = "amqpMDBTestQueue"),
//        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue")
////        ,
////        @ActivationConfigProperty(propertyName = "jndiParameters", propertyValue = "java.naming.factory.initial=org.apache.qpid.jms.jndi.JmsInitialContextFactory;connectionFactory.factory1=amqp://localhost:5672;queue.queue1=example/requests"),
//    })
//@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
//public class RequestListener implements MessageListener {
//    private static final Logger log =  getLogger(RequestListener.class.getName());
//
//    @Inject
//    @JMSConnectionFactory("java:/amqp/ConnectionFactory")
//    private JMSContext jmsContext;
//
//    @Override
//    public void onMessage(Message message) {
//        log.info("Processing request message");
//
//        TextMessage request = (TextMessage) message;
//        String responseText;
//
//        try {
//            responseText = processRequest(request);
//        } catch (Exception e) {
//        	log.log(SEVERE,"Processing failed: %s", e.getMessage());
//            return;
//        }
//
//        Queue responses = jmsContext.createQueue("example/responses");
//        JMSProducer producer = jmsContext.createProducer();
//        TextMessage response = jmsContext.createTextMessage();
//
//        try {
//            response.setJMSCorrelationID(request.getJMSMessageID());
//            response.setText(responseText);
//        } catch (JMSException e) {
//            throw new RuntimeException(e);
//        }
//
//        producer.send(responses, response);
//
//        log.info("Sent response message");
//    }
//
//    private String processRequest(TextMessage request) throws Exception {
//        String text = request.getText();
//        return text.toUpperCase();
//    }
//}
