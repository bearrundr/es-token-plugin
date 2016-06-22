/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.rest.action.trainnaivebayes;

import org.elasticsearch.action.trainnaivebayes.TrainNaiveBayesRequestBuilder;
import org.elasticsearch.action.trainnaivebayes.TrainNaiveBayesResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.action.support.RestBuilderListener;

import java.nio.charset.Charset;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;

/**
 *
 */
public class RestTrainNaiveBayesAction extends BaseRestHandler {

    @Inject
    public RestTrainNaiveBayesAction(Settings settings, RestController controller, Client client) {
        super(settings, controller, client);
        controller.registerHandler(POST, "_trainnaivebayes", this);
    }

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        TrainNaiveBayesRequestBuilder trainNaiveBayesRequestBuilder = new TrainNaiveBayesRequestBuilder(client);
        trainNaiveBayesRequestBuilder.setId(request.param("id"));
        trainNaiveBayesRequestBuilder.source(new String(request.content().toBytes(), Charset.defaultCharset()));

        trainNaiveBayesRequestBuilder.execute(new RestBuilderListener<TrainNaiveBayesResponse>(channel) {
            @Override
            public RestResponse buildResponse(TrainNaiveBayesResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                response.toXContent(builder, request);
                builder.endObject();
                return new BytesRestResponse(OK, builder);
            }
        });
    }
}