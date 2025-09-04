package software.amazon.s3.analyticsaccelerator.io.physical.reader;

import glide.api.GlideClusterClient;
import glide.api.models.configuration.GlideClusterClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.s3.analyticsaccelerator.S3SeekableInputStreamFactory;
import software.amazon.s3.analyticsaccelerator.request.Range;

import java.util.Base64;

public class ValkeyClient {
        GlideClusterClient client;

    private static final Logger LOG = LoggerFactory.getLogger(ValkeyClient .class);

        public ValkeyClient() {
            System.out.println("Connecting to Valkey Glide...");

            try {
                // Configure the Glide Client
                GlideClusterClientConfiguration config = GlideClusterClientConfiguration.builder()
                        .address(NodeAddress.builder()
                                .host("")
                                .port(6379)
                                .build())
                        .requestTimeout(3000)
                        .useTLS(true)
                        .build();

                this.client = GlideClusterClient.createClient(config).get();
            } catch (Exception e) {
                System.out.println("OOPS!");
            }
        }


    public void putObject(byte[] buf, String key, Range range) {
        String s = Base64.getEncoder().encodeToString(buf);

        String cacheKey = buildCacheKey(key, range);

        this.client.set(cacheKey, s);

        System.out.println("PUTTING DATA FOR: " + cacheKey + "LENGTH: " + buf.length);
    }

        public byte[] getObject(String key) {
            String s  = this.client.get(key).join();

            if (s == null || s.isEmpty()) {
                return null;
            }

            LOG.info("Found key {} in the cache!", key);

            byte[] x =  Base64.getDecoder().decode(s);
            return x;
        }

        public String buildCacheKey(String key, Range range) {
            return String.format("%s-%d-%d", key, range.getStart(), range.getEnd());
        }
}
