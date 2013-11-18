package config;

//the bean model representing a single server
public class ServerBean {
     private String host;
     private Integer port;
     
     public ServerBean(String host, Integer port) {
             this.host = host;
             this.port = port;
     }

     public String getHost() {
             return host;
     }

     public Integer getPort() {
             return port;
     }
}

