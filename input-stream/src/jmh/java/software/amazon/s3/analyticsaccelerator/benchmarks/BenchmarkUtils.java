/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.s3.analyticsaccelerator.benchmarks;

import static software.amazon.s3.analyticsaccelerator.util.Constants.ONE_KB;

import java.util.List;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.s3.analyticsaccelerator.access.StreamRead;
import software.amazon.s3.analyticsaccelerator.access.StreamReadPattern;

/** Utils class for methods to be used in micro benchmarks. */
public class BenchmarkUtils {

  private static final int PARQUET_FOOTER_SIZE = 10 * ONE_KB;

  /**
   * Gets a list of keys to be read from a bucket.
   *
   * @param s3Client client to use to make the list request.
   * @param bucket bucket to list keys from.
   * @param prefix list prefix
   * @param maxKeys number of keys to read
   * @return List<S3Object>
   */
  public static List<S3Object> getKeys(
      S3Client s3Client, String bucket, String prefix, int maxKeys) {
    ListObjectsV2Request.Builder requestBuilder =
        ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).maxKeys(maxKeys);

    ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

    System.out.println("\nObjects fetched: " + response.contents().size());

    return response.contents();
  }

  /**
   * Constructs a parquet like read pattern. This will first issue two reads for the footer, and
   * then a number of reads of various sizes which represent a column read.
   *
   * @param objectSize The size of object to read
   * @return Stream read pattern
   */
  public static StreamReadPattern getQuasiParquetColumnChunkPattern(long objectSize) {
    return StreamReadPattern.builder()
        .streamRead(
            StreamRead.builder()
                .start(percent(objectSize, 10))
                .length(percent(objectSize, 4))
                .build())
        .streamRead(
            StreamRead.builder()
                .start(percent(objectSize, 25))
                .length(percent(objectSize, 5))
                .build())
        .streamRead(
            StreamRead.builder()
                .start(percent(objectSize, 40))
                .length(percent(objectSize, 8))
                .build())
        .streamRead(
            StreamRead.builder()
                .start(percent(objectSize, 80))
                .length(percent(objectSize, 6))
                .build())
        .build();
  }

  private static long percent(long size, int percent) {
    return (size / 100) * percent;
  }

  /**
   * Stream read for the last 8 bytes of the object.
   *
   * @param objectSize size of the object
   * @return StreamRead
   */
  public static StreamRead getMagicBytesRead(long objectSize) {
    return StreamRead.builder().start(objectSize - 1 - 8).length(8).build();
  }

  /**
   * Stream for the last 10KB to replicate a typical footer read.
   *
   * @param objectSize size of the object
   * @return StreamRead
   */
  public static StreamRead getFooterRead(long objectSize) {
    return StreamRead.builder()
        .start(objectSize - 1 - PARQUET_FOOTER_SIZE)
        .length(PARQUET_FOOTER_SIZE)
        .build();
  }
}
