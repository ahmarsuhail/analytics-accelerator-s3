package software.amazon.s3.analyticsaccelerator.io.physical.reader;

import glide.api.GlideClient;
import glide.api.GlideClusterClient;
import glide.api.models.configuration.AdvancedGlideClusterClientConfiguration;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.GlideClusterClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.ReadFrom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.s3.analyticsaccelerator.S3SeekableInputStreamFactory;
import software.amazon.s3.analyticsaccelerator.request.Range;

import java.util.Base64;
import java.util.List;

public class ValkeyClient {
        GlideClient client;

    private static final Logger LOG = LoggerFactory.getLogger(ValkeyClient .class);

        public ValkeyClient() {
            System.out.println("Connecting to Valkey Glide...");

            try {
                // Configure the Glide Client
                GlideClientConfiguration config = GlideClientConfiguration.builder()
                        .address(NodeAddress.builder()
                                .host("")
                                .port(6379)
                                .build())
                        .requestTimeout(5000)
                        .useTLS(true)
                        .build();

                this.client = GlideClient.createClient(config).get();
            } catch (Exception e) {
                System.out.println("OOPS!");
            }
        }


    public void putObject(byte[] buf, String key, Range range) {
        String s = Base64.getEncoder().encodeToString(buf);

        String cacheKey = buildCacheKey(key, range);

        this.client.set(cacheKey, s).join();

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
