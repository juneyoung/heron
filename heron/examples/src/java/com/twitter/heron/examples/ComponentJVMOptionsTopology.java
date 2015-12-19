package com.twitter.heron.examples;

import java.util.Map;

import com.twitter.heron.api.Config;
import com.twitter.heron.api.HeronSubmitter;
import com.twitter.heron.api.bolt.BaseRichBolt;
import com.twitter.heron.api.bolt.OutputCollector;
import com.twitter.heron.api.metric.GlobalMetrics;
import com.twitter.heron.api.topology.OutputFieldsDeclarer;
import com.twitter.heron.api.topology.TopologyBuilder;
import com.twitter.heron.api.topology.TopologyContext;
import com.twitter.heron.api.tuple.Tuple;

/**
 * This is a basic example of a Storm topology.
 */
public class ComponentJVMOptionsTopology {
  public static class ExclamationBolt extends BaseRichBolt {
    OutputCollector _collector;
    long nItems;
    long startTime;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
      _collector = collector;
      nItems = 0;
      startTime = System.currentTimeMillis();
    }

    @Override
    public void execute(Tuple tuple) {
      if (++nItems % 100000 == 0) {
        long latency = System.currentTimeMillis() - startTime;
        System.out.println("Done " + nItems + " in " + latency);
        GlobalMetrics.incr("selected_items");
      }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
  }

  public static void main(String[] args) throws Exception {
    TopologyBuilder builder = new TopologyBuilder();

    builder.setSpout("word", new TestWordSpout(), 2);
    builder.setBolt("exclaim1", new ExclamationBolt(), 2)
        .shuffleGrouping("word");

    Config conf = new Config();
    conf.setDebug(true);
    conf.setMaxSpoutPending(10);
    conf.setComponentRam("word", 500 * 1024 * 1024);
    conf.setComponentRam("exclaim1", 1024 * 1024 * 1024);
    // TOPOLOGY_WORKER_CHILDOPTS will be a global one
    conf.put(Config.TOPOLOGY_WORKER_CHILDOPTS, "-XX:+HeapDumpOnOutOfMemoryError");
    // For each component, both the global and if any the component one will be appended.
    // And the component one will take precedence
    conf.setComponentJvmOptions("word", "-XX:NewSize=300m");
    conf.setComponentJvmOptions("exclaim1", "-XX:NewSize=800m");

    if (args != null && args.length > 0) {
      conf.setNumStmgrs(1);
      HeronSubmitter.submitTopology(args[0], conf, builder.createTopology());
    } else {
      // TODO:- This is not yet supported
      System.out.println("Local mode not yet supported");
      System.exit(1);
    }
  }
}