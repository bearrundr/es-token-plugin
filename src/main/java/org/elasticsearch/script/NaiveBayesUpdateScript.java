package org.elasticsearch.script;

import org.apache.lucene.index.Fields;
import org.apache.spark.mllib.classification.ClassificationModel;
import org.apache.spark.mllib.linalg.Vectors;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.termvectors.ShardTermVectorService;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.lookup.SourceLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Script for predicting class with a Naive Bayes model
 */
public class NaiveBayesUpdateScript extends AbstractSearchScript {

    final static public String SCRIPT_NAME = "naive_bayes_update_script";
    ClassificationModel model = null;
    String field = null;
    ArrayList features = new ArrayList();
    Map<String, Integer> wordMap;
    List<Integer> indices = new ArrayList<>();
    List<Integer> values = new ArrayList<>();
    boolean fieldDataFields = false;
    Map<String, Object> context;
    Client client;

    @Override
    public void setNextVar(String name, Object value) {
        if (name.equals("ctx")) {
            this.context = (Map<String, Object>) value;
        }
    }

    /**
     * Factory that is registered in
     * {@link org.elasticsearch.plugin.TokenPlugin#onModule(org.elasticsearch.script.ScriptModule)}
     * method when the plugin is loaded.
     */
    public static class Factory implements NativeScriptFactory {

        final Node node;

        @Inject
        public Factory(Node node) {
            // Node is not fully initialized here
            // All we can do is save a reference to it for future use
            this.node = node;
        }

        /**
         * This method is called for every search on every shard.
         *
         * @param params list of script parameters passed with the query
         * @return new native script
         */
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) throws ScriptException {
            return new NaiveBayesUpdateScript(params, node.client());
        }
    }

    /**
     * @param params terms that a used for classification and model parameters. Initialize
     *               naive bayes model here.
     * @throws org.elasticsearch.script.ScriptException
     */
    private NaiveBayesUpdateScript(Map<String, Object> params, Client client) throws ScriptException {
        GetResponse parametersDoc = SharedMethods.getStoredParameters(params, client);
        field = (String) params.get("field");
        fieldDataFields = (params.get("fieldDataFields") == null) ? fieldDataFields : (Boolean) params.get("fieldDataFields");
        model = SharedMethods.initializeNaiveBayesModel(features, field, parametersDoc);
        wordMap = new HashMap<>();
        SharedMethods.fillWordIndexMap(features, wordMap);
        this.client = client;
    }

    @Override
    public Object run() {
        final AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze((String) ((Map<String, Object>) (context.get("_source"))).get(field)).get();
        /** here be the vectorizer **/
        Tuple<int[], double[]> indicesAndValues;
        indicesAndValues = SharedMethods.getIndicesAndValuesFromAnalyzedTokens(wordMap, analyzeResponse.getTokens());
        /** until here **/
        double label = model.predict(Vectors.sparse(features.size(), indicesAndValues.v1(), indicesAndValues.v2()));
        ((Map<String, Object>) (context.get("_source"))).put("label", label);
        return label;
    }

}