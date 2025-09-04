package software.amazon.s3.analyticsaccelerator.io.physical.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.s3.analyticsaccelerator.request.GetRequest;
import software.amazon.s3.analyticsaccelerator.request.ObjectClient;
import software.amazon.s3.analyticsaccelerator.request.ObjectContent;
import software.amazon.s3.analyticsaccelerator.request.Range;
import software.amazon.s3.analyticsaccelerator.request.ReadMode;
import software.amazon.s3.analyticsaccelerator.request.Referrer;
import software.amazon.s3.analyticsaccelerator.util.ObjectKey;
import software.amazon.s3.analyticsaccelerator.util.OpenStreamInformation;
import software.amazon.s3.analyticsaccelerator.util.S3URI;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

public class StreamReaderV2 {

    ObjectClient objectClient;
    ValkeyClient valkeyClient;

    private static final Logger LOG = LoggerFactory.getLogger(StreamReaderV2 .class);

    public StreamReaderV2(
            ObjectClient objectClient,
            ValkeyClient valkeyClient
    ) {
        this.objectClient = objectClient;
        this.valkeyClient = valkeyClient;
    }


    public void read(byte[] buffer, int offset, int len, long pos, ObjectKey objectKey) throws IOException {

        long endPos = pos + len - 1;

        long cacheStartTime = System.nanoTime();

        Range range = new Range(pos, endPos);

//       byte[] b = valkeyClient.getObject(valkeyClient.buildCacheKey(objectKey.getS3URI().getKey(), range));
//
//       if (b != null) {
//           System.arraycopy(b, 0, buffer, 0, len);
//
//           long endTime = System.nanoTime();
//           long durationNanos = endTime - cacheStartTime;
//           long durationMillis = durationNanos / 1_000_000;
//
//           System.out.println("Cache operation took: " + durationMillis + " ms");
//
//          // LOG.debug("Cache retrieval Operation took {} for key {}", durationMillis, valkeyClient.buildCacheKey(objectKey.getS3URI().getKey(), range));
//
//           return;
//       }

        // LOG.debug("KEY NOT FOUND! {}, MAKING S3 GET REQUEST ", objectKey.getS3URI().getKey());

        System.out.println("KEY NOT FOUND " + objectKey.getS3URI().getKey());

        long s3GETtime = System.nanoTime();

       GetRequest getRequest =
                GetRequest.builder()
                        .s3Uri(objectKey.getS3URI())
                        .range(range)
                        .referrer(new Referrer(range.toHttpString(), ReadMode.READ_VECTORED))
                        .etag(objectKey.getEtag())
                        .build();

        ObjectContent objectContent = this.objectClient.getObject(getRequest, OpenStreamInformation.DEFAULT);

        InputStream inputStream = objectContent.getStream();
        int totalRead = 0;

        while (totalRead < len) {
            int bytesRead = inputStream.read(buffer, totalRead, len - totalRead);
            if (bytesRead == -1) {
                throw new EOFException("Premature EOF: expected " + len + " bytes, but got " + totalRead);
            }
            totalRead += bytesRead;
        }

        valkeyClient.putObject(buffer, objectKey.getS3URI().getKey(), range);

        inputStream.close();

        long endTime = System.nanoTime();
        long durationNanos = endTime - s3GETtime;
        long durationMillis = durationNanos / 1_000_000;


        System.out.println("S3 GET Operation took: " + durationMillis + " ms" + "for size: " + range.getLength() / (1024.0 * 1024.0));
      //  LOG.debug("S3 GET Operation took: " + durationMillis + " ms");
    }
}
