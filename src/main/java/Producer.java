import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;


public class Producer {
    final KafkaProducer<String, String> mProducer;
    final Logger mLogger = LoggerFactory.getLogger(Producer.class);

    public Producer(String bootstrapServer) {
        Properties props = produceProps(bootstrapServer);
        this.mProducer = new KafkaProducer<>(props);
        mLogger.info("Producer initialized");
    }

    private Properties produceProps(String bootstrapServer) {
        String serializer = StringSerializer.class.getName();
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, serializer);
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer);
        return props;
    }
    public void put(String topic,  String value) throws ExecutionException, InterruptedException {
//        mLogger.info("Put value: " + value);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);

        mProducer.send(record, (recordMetadata, e) -> {
            if (e != null) {
                mLogger.error("Error while producing", e);
                return;
            }

//            mLogger.info("Received new meta. \n" +
//                    "Topic: " + recordMetadata.topic() + "\n" +
//                    "Partition: " + recordMetadata.partition() + "\n" +
//                    "Offset: " + recordMetadata.offset() + "\n" +
//                    "TImestamp: " + recordMetadata.timestamp());
        });
    }

    public void close() {
        mLogger.info("Closing Producer's connection");
        mProducer.close();

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        int[] msgSizes = new int[]{10,50,100,200,500};
        int[] msgNums = new int[]{1000,10000,20000,50000,100000};
        String[] difPartition = new String[]{"test3"};
        for(String s: difPartition)
            ProducerTest(50000,200,s);
    }
    public static void ProducerTest(int numOfRecords, int sizeOfRecords,String topic){ //kafka test regarding message num and message size
        String server = "127.0.0.1:9091,127.0.0.1:9092,127.0.0.1:9093";
        Producer p = new Producer(server);
        Long runTime =0l;
        for (int i = 0; i < numOfRecords; i++) //send 1GB data
        {
            try {
                String val = Integer.toString(i);
                while(val.length()<200)val += '0';
                Long startTime = System.currentTimeMillis();
                p.put(topic,val);
                Long endTime = System.currentTimeMillis();
                runTime += endTime-startTime;
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("Throughput for producer is : "+(double)sizeOfRecords*numOfRecords*1000/(runTime*1024*1024)+" mb/s");
        System.out.printf("\nThroughput for producer is " + numOfRecords*1000/runTime +" record per second");
        p.close();
    }
}
