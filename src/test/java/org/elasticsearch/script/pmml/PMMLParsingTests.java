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

package org.elasticsearch.script.pmml;

import org.dmg.pmml.PMML;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.script.VectorEntriesPMML;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.Matchers;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.transform.Source;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.test.StreamsUtils.copyToStringFromClasspath;
import static org.hamcrest.CoreMatchers.equalTo;

public class PMMLParsingTests extends ESTestCase {

    public void testSimplePipelineParsing() throws IOException {
        final String pmmlString = copyToStringFromClasspath("/org/elasticsearch/script/logistic_regression.xml");
        PMML pmml = AccessController.doPrivileged(new PrivilegedAction<PMML>() {
            public PMML run() {
                try (InputStream is = new ByteArrayInputStream(pmmlString.getBytes(Charset.defaultCharset()))) {
                    Source transformedSource = ImportFilter.apply(new InputSource(is));
                    return JAXBUtil.unmarshalPMML(transformedSource);
                } catch (SAXException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (JAXBException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (IOException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                }
            }
        });

        PMMLModelScriptEngineService.FeaturesAndModel featuresAndModel = PMMLModelScriptEngineService.getFeaturesAndModelFromFullPMMLSpec(pmml, 0);
        assertThat(featuresAndModel.features.getEntries().size(), equalTo(15));
    }

    public void testTwoStepPipelineParsing() throws IOException {
        final String pmmlString = copyToStringFromClasspath("/org/elasticsearch/script/lr_model.xml");
        PMML pmml = AccessController.doPrivileged(new PrivilegedAction<PMML>() {
            public PMML run() {
                try (InputStream is = new ByteArrayInputStream(pmmlString.getBytes(Charset.defaultCharset()))) {
                    Source transformedSource = ImportFilter.apply(new InputSource(is));
                    return JAXBUtil.unmarshalPMML(transformedSource);
                } catch (SAXException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (JAXBException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (IOException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                }
            }
        });
        PMMLModelScriptEngineService.FeaturesAndModel featuresAndModel = PMMLModelScriptEngineService.getFeaturesAndModelFromFullPMMLSpec(pmml, 0);
        VectorEntriesPMML.VectorEntriesPMMLGeneralizedRegression vectorEntries = (VectorEntriesPMML
                .VectorEntriesPMMLGeneralizedRegression) featuresAndModel.features;
        assertThat(vectorEntries.getEntries().size(), equalTo(3));
        final String testData = copyToStringFromClasspath("/org/elasticsearch/script/test.data");
        final String expectedResults = copyToStringFromClasspath("/org/elasticsearch/script/lr_result.txt");
        String testDataLines[] = testData.split("\\r?\\n");
        String expectedResultsLines[] = expectedResults.split("\\r?\\n");
        for (int i = 0; i < testDataLines.length; i++) {
            String[] testDataValues = testDataLines[i].split(",");
            List ageInput = new ArrayList<Double>();
            ;
            if (testDataValues[0].equals("") == false) {
                ageInput.add(Double.parseDouble(testDataValues[0]));
            }
            List workInput = new ArrayList<String>();
            if (testDataValues[1].trim().equals("") == false) {
                workInput.add(testDataValues[1].trim());
            }
            Map<String, List> input = new HashMap<>();
            input.put("age", ageInput);
            input.put("work", workInput);
            Map<String, Object> result = (Map<String, Object>) vectorEntries.vector(input);
            String[] expectedResult = expectedResultsLines[i + 1].split(",");
            double expectedAgeValue = Double.parseDouble(expectedResult[0]);
           // assertThat(Double.parseDouble(expectedResult[0]), Matchers.closeTo(((double[]) result.get("values"))[0], 1.e-7));
            if (workInput.size() == 0) {
                // this might be a problem with the model. not sure. the "other" value does not appear in it.
                assertArrayEquals(((double[]) result.get("values")), new double[]{expectedAgeValue, 1.0d}, 1.e-7);
                assertArrayEquals(((int[]) result.get("indices")), new int[]{0, 4});
            } else if ("Private".equals(workInput.get(0))) {
                assertArrayEquals(((double[]) result.get("values")), new double[]{expectedAgeValue, 1.0d, 1.0d}, 1.e-7);
                assertArrayEquals(((int[]) result.get("indices")), new int[]{0, 1, 4});
            } else if ("Self-emp-inc".equals(workInput.get(0))) {
                assertArrayEquals(((double[]) result.get("values")), new double[]{expectedAgeValue, 1.0d, 1.0d}, 1.e-7);
                assertArrayEquals(((int[]) result.get("indices")), new int[]{0, 2, 4});
            } else if ("State-gov".equals(workInput.get(0))) {
                assertArrayEquals(((double[]) result.get("values")), new double[]{expectedAgeValue, 1.0d, 1.0d}, 1.e-7);
                assertArrayEquals(((int[]) result.get("indices")), new int[]{0, 3, 4});
            } else {
                fail("work input was " + workInput);
            }
        }
    }

    public void testModelAndFeatureParsing() throws IOException {
        final String pmmlString = copyToStringFromClasspath("/org/elasticsearch/script/lr_model.xml");
        PMML pmml = AccessController.doPrivileged(new PrivilegedAction<PMML>() {
            public PMML run() {
                try (InputStream is = new ByteArrayInputStream(pmmlString.getBytes(Charset.defaultCharset()))) {
                    Source transformedSource = ImportFilter.apply(new InputSource(is));
                    return JAXBUtil.unmarshalPMML(transformedSource);
                } catch (SAXException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (JAXBException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                } catch (IOException e) {
                    throw new ElasticsearchException("could not convert xml to pmml model", e);
                }
            }
        });

        PMMLModelScriptEngineService.Factory scriptFactory = new PMMLModelScriptEngineService.Factory(pmmlString);

    }
}