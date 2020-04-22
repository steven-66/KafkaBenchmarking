import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class Consumer {
    //Variables

    private  final Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
    private final String mBootstrapServer;
    private final String mGroupId;
    private final String mTopic;
    // constructor
    public Consumer(String mBootstrapServer, String mGroupId, String mTopic){
        this.mBootstrapServer = mBootstrapServer;
        this.mGroupId = mGroupId;
        this.mTopic = mTopic;
    }
    public void run(int partitionNum){
        for(int i=0;i<partitionNum;i++){
            ConsumeThread c1 = new ConsumeThread(new TopicPartition(mTopic,i),mBootstrapServer,mGroupId);
            new Thread(c1).start();
        }

    }




    public static void main(String[] args) {
        String server = "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093";
        String topic = "test2";
        String groupId = "kafka-bench";
        String[] difPartition = new String[]{"test","test3","test2","test4","test5","test6"};
        int[] msgNums = new int[]{1000, 10000, 20000, 50000, 100000};
        //test for different num of message
//        for (int i : msgNums){
//            ConsumerTest(server, topic, groupId, i,3);
//        }
        //test for different partition num
        int i = 1;
        for(String s:difPartition){
            ConsumerTest(server, s, groupId, 50000,i);
            i++;
        }
    }
    public static void ConsumerTest(String server, String topic, String groupId, int numOfMessages,int partitionNum){
        ConsumeThread.numOfMessages = numOfMessages;
        ConsumeThread.testTime = 0;
        new Consumer(server, groupId, topic).run(partitionNum);
        while (ConsumeThread.numOfMessages > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("Throughput for consumer is: " + (double) numOfMessages * 200 * 1000 / (ConsumeThread.testTime * 1024 * 1024) + " MB/s\n");
        System.out.printf("latency for consumer is: " + (double) numOfMessages * 1000 / ConsumeThread.testTime + " s");
    }
}
class ConsumeThread implements Runnable {
    static int  numOfMessages;
    static long testTime;
    private TopicPartition partition;
    private KafkaConsumer<String, String> mConsumer;
    private Logger mLogger = LoggerFactory.getLogger(Consumer.class.getName());
    public ConsumeThread(TopicPartition partition,  String boostStrapServer, String groupId){
        this.partition = partition;
        mConsumer = new KafkaConsumer<String, String>(consumerProps(boostStrapServer,groupId));
        mConsumer.assign(Collections.singleton(partition));
        mConsumer.seekToBeginning(Collections.singleton(partition));
    }
    private Properties consumerProps(String bootstrapServer, String groupId){
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return properties;
    }
    boolean onReading = true;
    @Override
    public void run() {

        while (onReading) {
            synchronized (ConsumeThread.class) {
                if(numOfMessages<=0){
                    onReading = false;
                    break;
                }
                long startTime = System.currentTimeMillis();
                ConsumerRecords<String, String> records = mConsumer.poll(Duration.ofMillis(100));
                long endTime = System.currentTimeMillis();
                testTime += endTime-startTime;
                for (ConsumerRecord<String, String> record : records) {
                    numOfMessages--;
//                    mLogger.info("Key: " + record.key() + ", Value: " + record.value());
//                    mLogger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    if(numOfMessages<=0){
                        onReading = false;
                        break;
                    }
                }
            }
            Thread.yield();
        }
    }
}
