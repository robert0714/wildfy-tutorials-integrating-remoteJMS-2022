# Wildfly 26 integration with remote ActiveMQ
* reference: [JBoss eap 6.4 integration with remote ActiveMQ](https://www.youtube.com/watch?v=m52fYYB4fA4)
* ActiveMQ Artemis [installation guide](http://www.mastertheboss.com/jboss-frameworks/activemq/getting-started-with-activemq-artemis/) 
* ActiveMQ Artemis [docker-compose](http://www.mastertheboss.com/jboss-frameworks/activemq/how-to-run-artemis-mq-as-docker-image/)
* we can see the below diagram   
  ![4EHRxgl4Uf.png](/images/diagram01.png)
    * To modify the file  ``${ARTEMIS_HOME_DOMAIN}/etc/broker.xml`` , adjust element ``address``  https://youtu.be/m52fYYB4fA4?t=270  
      * Add the queue named 'HelloworldQ' :
        ```xml
        <addresses>
         <address name="DLQ">
            <anycast>
               <queue name="DLQ" />
            </anycast>
         </address>
         <address name="ExpiryQueue">
            <anycast>
               <queue name="ExpiryQueue" />
            </anycast>
         </address>
         <address name="HelloworldQ">
            <anycast>
               <queue name="HelloworldQ" />
            </anycast>
         </address>        
        </addresses>
        ```
    * adjust the configuration (location: ``${ARTEMIS_HOME_DOMAIN}/etc/jolokia-access.xml``) comment ``<allow-origin>*://localhost*</allow-origin>`` https://youtu.be/m52fYYB4fA4?t=300  
    * adjust the configuration (location: ``${ARTEMIS_HOME_DOMAIN}/etc/bootstrap.xml``):   ``<web bind="http://localhost:8161" path="web"`` -> ``<web bind="http://0.0.0.0:8161" path="web"`` https://youtu.be/m52fYYB4fA4?t=379
    * publisher.jar
        * Publisher :Java class to send messages to ActiveMQ
        ```java
        //JBoss eap 6.4 integration with ActiveMQ and consuming messages remotely.
        //Java class to send messages to ActiveMQ
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
                this.brokerUrl = "tcp://192.168.56.102:61616";
                this.username = "jbossamq";
                this.password = "jbossamq123";
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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        ```
        pom.xml  
        ```xml
        <dependencies>
            <dependency>
                <groupId>org.apache.activemq</groupId>
                <artifactId>activemq-client</artifactId>
                <version>5.15.15</version>
            </dependency>
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>${slf4j.version}</version>
            </dependency>
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-simple</artifactId>
                <version>${slf4j.version}</version>
            </dependency>
        </dependencies>
        <build>
            <finalName>publisher</finalName>
        </build>    
        ```
        * HelloWorldQueueMDB :java class to consume the messages
        ```java
        //JBoss eap 6.4 integration with ActiveMQ and consuming messages remotely.
        package org.jboss.as.quickstarts.mdb;
        import java.util.logging.Logger;
        import javax.ejb.ActivationConfigProperty;
        import javax.ejb.MessageDriven;
        import javax.jms.JMSException;
        import javax.jms.Message;
        import javax.jms.MessageListener;
        import javax.jms.TextMessage;
        import org.jboss.ejb3.annotation.ResourceAdapter;

        @MessageDriven(name = "HelloworldQ", activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "HelloworldQ"),
                @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge") })
        @ResourceAdapter(value="activemq-ra.rar")
        public class HelloWorldQueueMDB implements MessageListener {
            private final static Logger LOGGER = Logger.getLogger(HelloWorldQueueMDB.class.toString());
            /**
            * @see MessageListener#onMessage(Message)
            */
            public void onMessage(Message rcvMessage) {
                TextMessage msg = null;
                try {
                    if (rcvMessage instanceof TextMessage) {
                        msg = (TextMessage) rcvMessage;
                        LOGGER.info("Received Message from queue: " + msg.getText());
                    } else {
                        LOGGER.warning("Message of wrong type: " + rcvMessage.getClass().getName());
                    }
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ```
    * Adjust standalone-full.xml in jboss eap / wildfly，
      * Find the element `<resource-adapter-ref resource-adapter-name="${ejb.resource-adapter-name:activemq-ra.rar}"/>`[to process](https://youtu.be/m52fYYB4fA4?t=580)  
        ```xml
            <mdb>
                <resource-adapter-ref resource-adapter-name="${ejb.resource-adapter-name:activemq-ra.rar}"/>
                <bean-instance-pool-ref pool-name="mdb-strict-max-pool"/>
            </mdb>
        ```
        to 
        ```xml
            <mdb>
                <resource-adapter-ref resource-adapter-name="activemq-rar.rar"/>
                <bean-instance-pool-ref pool-name="mdb-strict-max-pool"/>
            </mdb>
        ```
    * Download ActiveMQ Resource Adapter community version( Only rehat version is able to support 2-pc transaction ,but the community version does not . ) [ActiveMQ :: RAR](https://mvnrepository.com/artifact/org.apache.activemq/activemq-rar/5.17.1)(5.16.x ->jdk8 , 5.17.x ->jdk11).
       * The reason to replace it because that Jboss EAP/Wildfly's  activemq-rar (2.1x)is too old. 
       * Redhat [official document](https://access.redhat.com/documentation/en-us/red_hat_amq/6.3/html/integrating_with_jboss_enterprise_application_platform/deployrar-installrar#doc-wrapper) step 2 metioned that they use the version 5.x(ActiveMQ JMS Resource Adapter-JMS1.1 )  
       * video: https://youtu.be/m52fYYB4fA4?t=636   
       * Copy to{JBOSS_HOME}/standalone/deployments/
       * [Other document](http://www.mastertheboss.com/jbossas/jboss-jms/integrate-activemq-with-wildfly/)
    * We can see the Redhat [official document](https://access.redhat.com/documentation/en-us/red_hat_amq/6.3/html/integrating_with_jboss_enterprise_application_platform/deployrar-installrar#doc-wrapper) step 5. action `<subsystem xmlns="urn:jboss:domain:resource-adapters:6.1"/>`in the configuration to [add the element](https://youtu.be/m52fYYB4fA4?t=735):
      ```xml
        <subsystem xmlns="urn:jboss:domain:resource-adapters:6.1">
            <resource-adapters>
                <resource-adapter id="activemq-rar.rar">
                    <archive>
                        activemq-rar.rar
                    </archive>
                    <config-property name="ServerUrl">
                        tcp://192.168.56.102:61616?jms.rmIdFromConnectionId=true
                    </config-property>
                    <config-property name="UserName">
                        jbossamq
                    </config-property>
                    <config-property name="Password">
                        jbossamq123
                    </config-property>
                </resource-adapter> 
            </resource-adapters>
        </subsystem> 
      ```
#  Bootable Wildfly with a Basic MDB via RAR to an external AMQ 
[reference](./demo-mdb-rar/README.md)
