# Install ActiveMQ Artemis on Kubernetes/OpenShift
Finally, we may proceed to the ActiveMQ Artemis installation. Firstly, let’s install the required CRDs. You may find all the YAML manifests inside the operator repository on GitHub.
```
$ git clone https://github.com/artemiscloud/activemq-artemis-operator.git
$ cd activemq-artemis-operator
$ git chekout v1.0.4
```
## To deploy an operator watching single namespace (operator's namespace)
1. Deploy all the crds
```
kubectl create -f ./deploy/crds
```

2. Deploy operator

You need to deploy all the yamls from this dir except *cluster_role.yaml* and *cluster_role_binding.yaml*:
```
kubectl create -f ./deploy/service_account.yaml
kubectl create -f ./deploy/role.yaml
kubectl create -f ./deploy/role_binding.yaml
kubectl create -f ./deploy/election_role.yaml
kubectl create -f ./deploy/election_role_binding.yaml
kubectl create -f ./deploy/operator_config.yaml
kubectl create -f ./deploy/operator.yaml
```

3.Create a cluster.

In order to create a cluster, we have to create the ActiveMQArtemis object. It contains a number of brokers being a part of the cluster (1). We should also set the accessor, to expose the AMQP/all port  outside of every single broker pod (2). Of course, we will also expose the management console (3).

```yaml
apiVersion: broker.amq.io/v1beta1
kind: ActiveMQArtemis
metadata:
  name: ex-aao
spec:
  deploymentPlan:
    size: 3 # (1)
    image: placeholder
    messageMigration: true
    resources:
      limits:
        cpu: "500m"
        memory: "1024Mi"
      requests:
        cpu: "250m"
        memory: "512Mi"
  acceptors: # (2)  https://access.redhat.com/documentation/it-it/red_hat_amq_broker/7.10/pdf/deploying_amq_broker_on_openshift/red_hat_amq_broker-7.10-deploying_amq_broker_on_openshift-en-us.pdf
    - name: amqp
      protocols: amqp
      port: 5672
      connectionsAllowed: 5
    - name: all-acceptors
      protocols: all
      port: 61616
      connectionsAllowed: 100
  console: # (3)
    expose: true
```
ps. You can refer the [official document](https://github.com/artemiscloud/activemq-artemis-operator/blob/main/docs/getting-started/quick-start.md#draining-messages-on-scale-down).

Once the ``ActiveMQArtemis`` is created, and the operator starts the deployment process. It creates the ``StatefulSet`` object:`
```shell
$ kubectl get statefulset
NAME        READY   AGE
ex-aao-ss   3/3     1m
```

It starts all three pods with brokers sequentially:
```shell
$ kubectl get pod -l application=ex-aao-app
NAME          READY   STATUS    RESTARTS    AGE
ex-aao-ss-0   1/1     Running   0           5m
ex-aao-ss-1   1/1     Running   0           3m
ex-aao-ss-2   1/1     Running   0           1m
```

Let’s display a list of ``Services`` created by the operator. There is a single ``Service`` per broker for exposing the AMQP port (``ex-aao-amqp-*``) and web console (``ex-aao-wsconsj-*``):
```shell
$ kubectl get svc
NAME                  TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)              AGE
ex-aao-amqp-0-svc     ClusterIP   10.102.137.85   <none>        5672/TCP             5m40s
ex-aao-amqp-1-svc     ClusterIP   10.107.188.11   <none>        5672/TCP             5m40s
ex-aao-amqp-2-svc     ClusterIP   10.102.177.8    <none>        5672/TCP             5m40s
ex-aao-hdls-svc       ClusterIP   None            <none>        8161/TCP,61616/TCP   5m40s
ex-aao-ping-svc       ClusterIP   None            <none>        8888/TCP             5m40s
ex-aao-wconsj-0-svc   ClusterIP   10.101.88.50    <none>        8161/TCP             5m40s
ex-aao-wconsj-1-svc   ClusterIP   10.102.68.243   <none>        8161/TCP             5m40s
ex-aao-wconsj-2-svc   ClusterIP   10.103.56.48    <none>        8161/TCP             5m40s
kubernetes            ClusterIP   10.96.0.1       <none>        443/TCP              12m

```

If you use minikube ..
```shell
$ kubectl get ing
NAME                      CLASS    HOSTS   ADDRESS   PORTS   AGE
ex-aao-wconsj-0-svc-ing   <none>   *                 80      6m6s
ex-aao-wconsj-1-svc-ing   <none>   *                 80      6m6s
ex-aao-wconsj-2-svc-ing   <none>   *                 80      6m6s

$ kubectl edit ing ex-aao-wconsj-0-svc-ing
ingress.networking.k8s.io/ex-aao-wconsj-0-svc-ing edited

$ kubectl get ing
NAME                      CLASS    HOSTS              ADDRESS   PORTS   AGE
ex-aao-wconsj-0-svc-ing   <none>   one.activemq.com             80      14m
ex-aao-wconsj-1-svc-ing   <none>   *                            80      14m
ex-aao-wconsj-2-svc-ing   <none>   *                            80      14m

$ kubectl describe ing  ex-aao-wconsj-0-svc-ing
Name:             ex-aao-wconsj-0-svc-ing
Labels:           ActiveMQArtemis=ex-aao
                  application=ex-aao-app
                  statefulset.kubernetes.io/pod-name=ex-aao-ss-0
Namespace:        default
Address:
Ingress Class:    <none>
Default backend:  <default>
Rules:
  Host              Path  Backends
  ----              ----  --------
  one.activemq.com
                    /   ex-aao-wconsj-0-svc:wconsj-0 (172.17.0.6:8161)
Annotations:        <none>
Events:             <none>


$ minikube addons enable ingress
* ingress is an addon maintained by Kubernetes. For any concerns contact minikube on GitHub.
You can view the list of minikube maintainers at: https://github.com/kubernetes/minikube/blob/master/OWNERS
  - Using image k8s.gcr.io/ingress-nginx/controller:v1.2.1
  - Using image k8s.gcr.io/ingress-nginx/kube-webhook-certgen:v1.1.1
  - Using image k8s.gcr.io/ingress-nginx/kube-webhook-certgen:v1.1.1
* Verifying ingress addon...
* The 'ingress' addon is enabled

$ minikube addons enable ingress-dns
* ingress-dns is an addon maintained by Google. For any concerns contact minikube on GitHub.
You can view the list of minikube maintainers at: https://github.com/kubernetes/minikube/blob/master/OWNERS
  - Using image gcr.io/k8s-minikube/minikube-ingress-dns:0.0.2
* The 'ingress-dns' addon is enabled

$ kubectl get ing
NAME                      CLASS    HOSTS              ADDRESS          PORTS   AGE
ex-aao-wconsj-0-svc-ing   <none>   one.activemq.com   192.168.59.142   80      22m
ex-aao-wconsj-1-svc-ing   <none>   *                  192.168.59.142   80      22m
ex-aao-wconsj-2-svc-ing   <none>   *                  192.168.59.142   80      22m

```

After creating ingresses we would have to add the following line in ``/etc/hosts``.
```
192.168.59.142    one.activemq.com two.activemq.com three.activemq.com
```
Now, we access the management console, for example for the third broker under the following URL http://one.activemq.com/console.

Once the broker is ready, we may define a test queue. The name of that queue is test-1.
```
apiVersion: broker.amq.io/v1beta1
kind: ActiveMQArtemisAddress
metadata:
  name: address-1
spec:
  addressName: address-1
  queueName: test-1
  routingType: anycast
```

The first way to distribute the connections is through the dedicated Kubernetes Service. We don’t have to leverage the services created automatically by the operator. We can create our own Service that load balances between all available pods with brokers.

slb.yaml
```yaml
kind: Service
apiVersion: v1
metadata:
  name: ex-aao-lb
spec:
  ports:
    - name: amqp
      protocol: TCP
      port: 5672
    - name: all-acceptors
      protocol: TCP
      port: 61616
  type: ClusterIP
  selector:
    application: ex-aao-app
```
# Related Referemce
* ActiveMQ Artemis Operator
  *  [Community Documentation](https://artemiscloud.io/docs/tutorials/using_operator/)
     * [Using JMS or Jakarta Messaging] (https://activemq.apache.org/components/artemis/documentation/latest/using-jms.html)
  *  [Redhat Documentation - Chapter 4. Configuring Operator-based broker deployments](https://access.redhat.com/documentation/en-us/red_hat_amq/2020.q4/html-single/deploying_amq_broker_on_openshift/index#assembly-br-configuring-operator-based-deployments_broker-ocp)
* Use cases: 
  *  AMQP: [ActiveMQ Artemis with Spring Boot on Kubernetes](https://piotrminkowski.com/2022/07/26/activemq-artemis-with-spring-boot-on-kubernetes/)
  *  AMQP: [Getting started with JMS and ActiveMQ on Kubernetes](https://github.com/ssorj/hello-world-jms-kubernetes)
     * [DevNation talk video](https://www.youtube.com/watch?v=mkqVxWZfGfI)
     * [DevNation talk slides](https://docs.google.com/presentation/d/1kOsWwLcJWZGoCF8O_NPUB0jkAre9LMhE2VETnafxcMw/edit?usp=sharing)
     * [Igor Brodewicz's Tech Blog - Running ActiveMQ Artemis on Kubernetes step-by-step ](https://brodewicz.tech/2020/05/running-activemq-artemis-on-kubernetes-step-by-step)
   
  
