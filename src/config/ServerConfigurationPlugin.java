package config;

import java.util.ArrayList;
import java.util.List;

import com.google.code.jconfig.reader.hierarchical.IHierarchicalReader;
import com.google.code.jconfig.reader.plugins.IConfigurationPlugin;

//the plugin class
public class ServerConfigurationPlugin implements IConfigurationPlugin<List<ServerBean>> {

   public List<ServerBean> readConfiguration(IHierarchicalReader reader) {
           
           IHierarchicalReader serversNode = reader.getChildren().get(0);
           List<ServerBean> servers = new ArrayList<ServerBean>();
           for (IHierarchicalReader child : serversNode.getChildren()) {
                   servers.add(new ServerBean(child.getAttributeValue("name"), Integer.parseInt(child.getAttributeValue("port"))));
           }
           
           return servers;
   }
}