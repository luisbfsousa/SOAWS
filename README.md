start activemq
```
java   --add-opens java.base/java.lang=ALL-UNNAMED   --add-opens java.base/java.util=ALL-UNNAMED   --add-opens java.base/java.security=ALL-UNNAMED   --add-opens java.base/java.net=ALL-UNNAMED   --add-opens java.naming/javax.naming.spi=ALL-UNNAMED   --add-opens java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED   --add-opens java.base/java.util.concurrent=ALL-UNNAMED   --add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED   --add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED   --add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED   -Dactivemq.home=$HOME/apache-activemq-5.18.3   -Dactivemq.base=$HOME/apache-activemq-5.18.3   -Dactivemq.conf=$HOME/apache-activemq-5.18.3/conf   -Dactivemq.data=$HOME/apache-activemq-5.18.3/data   -jar $HOME/apache-activemq-5.18.3/bin/activemq.jar start &
```

in each service folder
```
mvn spring-boot:run
```

frontend
```
python3 -m http.server 3000
```


