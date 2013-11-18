package config;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.code.jconfig.listener.IConfigurationChangeListener;

public class ServerConfigurationContainer implements IConfigurationChangeListener {

    private List<ServerBean> configuration;
    // a simple and brutal lock avoiding concurrency between different thread consumers of this container properties
    private AtomicBoolean suspend = new AtomicBoolean(true);
    private static ServerConfigurationContainer instance;
    
    public ServerConfigurationContainer() { }
    
    public static synchronized ServerConfigurationContainer getInstance() {
            if(instance == null) {
                    instance = new ServerConfigurationContainer();
            }
            
            return instance;
    }
    
    public List<ServerBean> getServers() {
            while(suspend.get());
            return configuration;
    }
    
    // more useful methods
    
    @Override
    public <T> void loadConfiguration(T configuration) {
            while(suspend.compareAndSet(false, true));

            List<ServerBean> newConfiguration = (List<ServerBean>)configuration;
            this.configuration = newConfiguration;
            suspend.compareAndSet(true, false);
    }
}