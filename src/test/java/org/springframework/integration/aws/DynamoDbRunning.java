/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aws;

import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;

/**
 * @author Artem Bilan
 *
 * @since 1.1
 */
public final class DynamoDbRunning extends TestWatcher {

	private static Log logger = LogFactory.getLog(DynamoDbRunning.class);

	// Static so that we only test once on failure: speeds up test suite
	private static Map<Integer, Boolean> dynamoDbOnline = new HashMap<>();

	private final int port;

	private AmazonDynamoDBAsync amazonDynamoDB;

	private DynamoDbRunning(int port) {
		this.port = port;
		dynamoDbOnline.put(port, true);
	}

	public AmazonDynamoDBAsync getDynamoDB() {
		return this.amazonDynamoDB;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		assumeTrue(dynamoDbOnline.get(this.port));

		String url = "http://localhost:" + this.port;

		this.amazonDynamoDB = AmazonDynamoDBAsyncClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("", "")))
				.withClientConfiguration(
						new ClientConfiguration()
								.withMaxErrorRetry(0)
								.withConnectionTimeout(1000))
				.withEndpointConfiguration(
						new AwsClientBuilder.EndpointConfiguration(url, Regions.DEFAULT_REGION.getName()))
				.build();

		try {
			this.amazonDynamoDB.listTables();
		}
		catch (SdkClientException e) {
			logger.warn("Tests not running because no DynamoDb on " + url, e);
			assumeNoException(e);
		}
		return super.apply(base, description);
	}


	public static DynamoDbRunning isRunning(int port) {
		return new DynamoDbRunning(port);
	}

}
