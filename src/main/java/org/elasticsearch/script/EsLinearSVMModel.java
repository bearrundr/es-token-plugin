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

package org.elasticsearch.script;

import org.dmg.pmml.RegressionModel;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.ESLoggerFactory;

public class EsLinearSVMModel extends EsRegressionModelEvaluator {

    public EsLinearSVMModel(RegressionModel regressionModel) {
        super(regressionModel);
    }

    @Override
    public String evaluate(Tuple<int[], double[]> featureValues) {
        double val = linearFunction(featureValues, intercept, coefficients);
        return val > 0.0 ? classes.v1() : classes.v2();
    }
}
