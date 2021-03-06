import io.confluent.examples.streams.IntegrationTestUtils2;
import io.confluent.examples.streams.kafka.EmbeddedSingleNodeKafkaCluster;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.processor.*;
import org.apache.kafka.test.TestUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.*;

public class PartitionTimestampSyncTest {
  @ClassRule
  public static final EmbeddedSingleNodeKafkaCluster CLUSTER = new EmbeddedSingleNodeKafkaCluster();

  private static final String inputTopic = "input-topic";
  private static final String outputTopic = "output-topic";

  @BeforeClass
  public static void startKafkaCluster() throws Exception {
    CLUSTER.createTopic(inputTopic, 2, 1);
    CLUSTER.createTopic(outputTopic);
  }

  @Test
  public void shouldCountUsersPerRegion() throws Exception {
    // Step 1: Configure and start the processor topology.

    Topology topology = new Topology();
    StreamPartitioner<String, Integer> partitioner = (String k, Integer v, int numPartitions)-> 0;
    topology.addSource("SOURCE", inputTopic)
        .addProcessor("PROCESS", () -> new MyProcessor(), "SOURCE")
        .addSink("SINK", outputTopic, new StringSerializer(), new IntegerSerializer(), partitioner, "PROCESS");

    KafkaStreams streams = new KafkaStreams(topology, getStreamProperties());
    streams.setUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
      System.err.println(throwable);
    });
    streams.start();


    // Step 2: Publish
    int MINUTE = 10;
    // 파티션0
    List<ProducerRecord<String, String>> records = new ArrayList<>();
    for(long l = 0; l < MINUTE*60; l++)
      records.add(new ProducerRecord<>(inputTopic, 0, l*1000, "A", "A"));
    IntegrationTestUtils2.produceRecordsSynchronously(records, getProducerProperties(), new StringSerializer(), new StringSerializer());

    // 파티션1
    records = new ArrayList<>();
    for(long l = 0; l < MINUTE*60; l++)
      records.add(new ProducerRecord<>(inputTopic, 1, l*1000, "B", "B"));
    IntegrationTestUtils2.produceRecordsSynchronously(records, getProducerProperties(), new StringSerializer(), new StringSerializer());

    //
    List<ConsumerRecord<String, Integer>> results = IntegrationTestUtils2.waitUntilMinKeyValueRecordsReceived(
        getConsumerProperties(),
        outputTopic,
        20,
        new StringDeserializer(),
        new IntegerDeserializer(),
        10*1000);
    streams.close();

    // outputTopic에서 Record가 timestamp로 정렬되어 있기를 기대하지만, 실제로는 그렇지 않다.
    // 1개의 Thread에서 2개의 task가 실행되고
    // task는 서로 연관없이 병렬로 실행되기 때문에, timestamp가 꼬일 수 있다.
    for(ConsumerRecord<String, Integer> rec : results)
      System.out.println(rec);
  }

  private Properties getStreamProperties() {
    Properties streamsConfiguration = new Properties();
    streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    // The commit interval for flushing records to state stores and downstream must be lower than
    // this integration test's timeout (30 secs) to ensure we observe the expected processing results.
    streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
    streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // Use a temporary directory for storing state, which will be automatically removed after the test.
    streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getAbsolutePath());
    return streamsConfiguration;
  }

  private Properties getConsumerProperties() {
    Properties consumerConfig = new Properties();
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "user-regions-lambda-integration-test-standard-consumer");
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
    return consumerConfig;
  }

  private Properties getProducerProperties() {
    Properties userRegionsProducerConfig = new Properties();
    userRegionsProducerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
    userRegionsProducerConfig.put(ProducerConfig.ACKS_CONFIG, "all");
    userRegionsProducerConfig.put(ProducerConfig.RETRIES_CONFIG, 0);
    userRegionsProducerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    userRegionsProducerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return userRegionsProducerConfig;
  }

}

class MyProcessor implements Processor<String, String>{

  ProcessorContext context;
  Map<String, Integer> map = new HashMap<>();

  @Override
  public void init(ProcessorContext context) {
    this.context = context;
    this.context.schedule(60*1000, PunctuationType.STREAM_TIME, this::flush);
  }

  public void flush(long timestamp){
    System.out.println(Thread.currentThread().getName() + " " + context.taskId() + " " + timestamp);
    for(String key : map.keySet())
      context.forward(key, map.get(key));

    context.commit();
    map = new HashMap<>();
  }

  @Override
  public void process(String key, String value) {
    Integer oldValue = map.get(value);
    if (oldValue == null)
      map.put(value, 1);
    else
      map.put(value, oldValue + 1);

  }

  @Override
  public void punctuate(long timestamp) {

  }

  @Override
  public void close() {

  }
}
