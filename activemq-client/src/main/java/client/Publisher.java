package client;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
public class Publisher
{    
    private Connection connection;
    private Session session;
    private Queue queue;
    private MessageProducer sender;
    private ActiveMQConnectionFactory connectionFactory;

    private String brokerUrl;
    private String username;
    private String password;
    private String queueName;

    public Publisher(){
        this.brokerUrl = "tcp://192.168.18.30:61616";
        this.username = "quarks";
        this.password = "quarks";
        this.queueName = "HelloworldQ";
        connectionFactory = new ActiveMQConnectionFactory(this.username, this.password, this.brokerUrl);
    }
    public void initQueueConnection() throws Exception {
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        queue = session.createQueue(queueName);
        sender =  session.createProducer(queue);
    }
    public void sendMsgToQueue(String serviceInput) throws Exception{
        initQueueConnection();  
        TextMessage requestTextMessage = session.createTextMessage(serviceInput);
        sender.send(requestTextMessage);
        sender.close();
        session.close();
        connection.close();
    }

    public static void main(String[] args) {
        Publisher pub = new Publisher();
        try {
            pub.sendMsgToQueue("helloworld");
        } catch (Exception e) { 
            e.printStackTrace();
        }finally {
        	System.out.println("exit...");
        }
    }
}