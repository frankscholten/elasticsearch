package org.apache.mesos.elasticsearch.systemtest;

import org.apache.log4j.Logger;
import org.apache.mesos.mini.MesosCluster;
import org.apache.mesos.mini.mesos.MesosClusterConfig;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Main app to run Mesos Elasticsearch with Mini Mesos.
 */
@SuppressWarnings({"PMD.AvoidUsingHardCodedIP"})
public class Main {

    public static final Logger LOGGER = Logger.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        MesosClusterConfig config = MesosClusterConfig.builder()
                .numberOfSlaves(3)
                .privateRegistryPort(15000) // Currently you have to choose an available port by yourself
                .slaveResources(new String[]{"ports(*):[9200-9200,9300-9300]", "ports(*):[9201-9201,9301-9301]", "ports(*):[9202-9202,9302-9302]"})
                .build();

        MesosCluster cluster = new MesosCluster(config);
        final AtomicReference<ElasticsearchSchedulerContainer> schedulerReference = new AtomicReference<>(null);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (schedulerReference.get() != null) {
                    schedulerReference.get().remove();
                }
                cluster.stop();
            }
        });
        cluster.start();
        cluster.injectImage("mesos/elasticsearch-executor");

        LOGGER.info("Starting scheduler");

        ElasticsearchSchedulerContainer scheduler = new ElasticsearchSchedulerContainer(config.dockerClient, cluster.getMesosContainer().getIpAddress());
        schedulerReference.set(scheduler);

        scheduler.start();

        LOGGER.info("Scheduler started at http://" + scheduler.getIpAddress() + ":8080");

        LOGGER.info("Type CTRL-C to quit");
        while (true) {
            Thread.sleep(1000);
        }
    }

}
