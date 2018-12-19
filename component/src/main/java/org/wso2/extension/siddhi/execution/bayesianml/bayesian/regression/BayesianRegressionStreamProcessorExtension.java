package org.wso2.extension.siddhi.execution.bayesianml.bayesian.regression;

import org.apache.log4j.Logger;
import org.wso2.extension.siddhi.execution.bayesianml.bayesian.regression.util.LinearRegressionModelHolder;
import org.wso2.extension.siddhi.execution.bayesianml.bayesian.util.LinearRegression;
import org.wso2.extension.siddhi.execution.bayesianml.util.CoreUtils;
import org.wso2.siddhi.annotation.Example;
import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.SiddhiAppContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.core.util.config.ConfigReader;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Predict using a Bayesian regression model built via
 * {@link BayesianRegressionUpdaterStreamProcessorExtension}.
 */


@Extension(
        name = "bayesianRegression",
        namespace = "streamingml",
        description = "This extension predicts using a Bayesian linear regression model.",
        parameters = {
                @Parameter(name = "model.name",
                        description = "The name of the model to be used",
                        type = {DataType.STRING}),
                @Parameter(name = "prediction.samples",
                        description = "The number of samples to be drawn to estimate the prediction",
                        type = {DataType.INT}, optional = true, defaultValue = "1000"),
                @Parameter(name = "model.features",
                        description = "The features of the model that need to be attributes of the stream",
                        type = {DataType.DOUBLE})
        },
        returnAttributes = {
                @ReturnAttribute(name = "prediction",
                        description = "The predicted value (double)",
                        type = {DataType.DOUBLE}),
                @ReturnAttribute(name = "confidence",
                        description = "Standard deviation of the predictive distribution",
                        type = {DataType.DOUBLE}
                )
        },
        examples = {
                @Example(
                        syntax = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 double, " +
                                "attribute_3 double);\n" +
                                "\n" +
                                "from StreamA#streamingml:bayesianRegression('model1', attribute_0, " +
                                "attribute_1, attribute_2, attribute_3) \n" +
                                "insert all events into OutputStream;",
                        description = "This query uses a Bayesian regression model named `model1` to predict " +
                                "the label of the feature vector represented by " +
                                "`attribute_0`, `attribute_1`, `attribute_2`, and `attribute_3`. " +
                                "The predicted value is emitted to the `OutputStream` stream" +
                                "along with the prediction confidence (std of predictive distribution) " +
                                "and the feature vector. As a result, the OutputStream stream is defined as follows: " +
                                "(attribute_0 double, attribute_1 double, attribute_2" +
                                " double, attribute_3 double, prediction double, confidence double)."
                )
        }
)


public class BayesianRegressionStreamProcessorExtension extends StreamProcessor {

    private static Logger logger = Logger.getLogger(BayesianRegressionStreamProcessorExtension.class);
    private String modelName;
    private int numberOfFeatures;
    private List<VariableExpressionExecutor> featureVariableExpressionExecutors = new ArrayList<>();


    /**
     * The initialization method for {@link BayesianRegressionStreamProcessorExtension},
     * which will be called before other methods and validate
     * the all configuration and getting the initial values.
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the Function
     * @param configReader                 this hold the {@link StreamProcessor} extensions configuration reader.
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition,
                                   ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                                   SiddhiAppContext siddhiAppContext) {

        String siddhiAppName = siddhiAppContext.getName();
        LinearRegression model;
        String modelPrefix;

        int predictionSamples = -1;
        int maxNumberOfFeatures = inputDefinition.getAttributeList().size();
        int minNumberOfAttributes = 2;
        int maxNumberOfHyperParameters = 2;

        if (attributeExpressionLength >= minNumberOfAttributes) {
            if (attributeExpressionLength > maxNumberOfHyperParameters + maxNumberOfFeatures) {
                throw new SiddhiAppCreationException(String.format("Invalid number of parameters for " +
                        "streamingml:bayesianRegression. This Stream Processor requires at most %s " + "parameters," +
                        " namely, model.name, prediction.samples[optional], model.features but found %s " +
                        "parameters", maxNumberOfHyperParameters + maxNumberOfFeatures, attributeExpressionLength));
            }
            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.STRING) {
                    modelPrefix = (String) ((ConstantExpressionExecutor) attributeExpressionExecutors[0]).getValue();
                    // model name = user given name + siddhi app name
                    modelName = modelPrefix + "." + siddhiAppName;
                } else {
                    throw new SiddhiAppCreationException("Invalid parameter type found for the model.name argument," +
                            "" + " required " + Attribute.Type.STRING + " but found " +
                            attributeExpressionExecutors[0].getReturnType().toString());
                }
            } else {
                throw new SiddhiAppCreationException("Parameter model.name must be a constant but found " +
                        attributeExpressionExecutors[0].getClass().getCanonicalName());
            }

            // 2nd param
            if (attributeExpressionExecutors[1] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[1].getReturnType() == Attribute.Type.INT) {
                    int val = (int) ((ConstantExpressionExecutor) attributeExpressionExecutors[1]).getValue();
                    if (val <= 0) {
                        throw new SiddhiAppCreationException(String.format("Invalid parameter value found for the " +
                                "prediction.samples argument. Expected a value greater than zero, but found: %d", val));
                    }
                    predictionSamples = val;
                } else {
                    throw new SiddhiAppCreationException("Invalid parameter type found for the prediction.samples " +
                            "argument. Expected: " + Attribute.Type.INT + " but found: " +
                            attributeExpressionExecutors[1].getReturnType().toString());
                }

                if (attributeExpressionExecutors[2] instanceof VariableExpressionExecutor) {
                    // set number of features
                    numberOfFeatures = attributeExpressionLength - 2;
                    // feature variables
                    featureVariableExpressionExecutors = CoreUtils.extractAndValidateFeatures(inputDefinition,
                            attributeExpressionExecutors, 2, numberOfFeatures);
                } else {
                    throw new SiddhiAppCreationException("3rd Parameter must be an attribute of the " +
                            "stream (model.features), but found a " +
                            attributeExpressionExecutors[2].getClass().getCanonicalName());
                }
            } else if (attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
                // set number of features
                numberOfFeatures = attributeExpressionLength - 1;
                // feature values
                /*  extractAndValidateFeatures(inputDefinition, attributeExpressionExecutors, 1);*/
                featureVariableExpressionExecutors = CoreUtils.extractAndValidateFeatures(inputDefinition,
                        attributeExpressionExecutors, 1, numberOfFeatures);
            } else {
                throw new SiddhiAppCreationException("2nd Parameter must either be a constant " +
                        "(prediction.samples) or an attribute of the stream (model.features), " +
                        "but found a " + attributeExpressionExecutors[1].getClass().getCanonicalName());
            }
        } else {
            throw new SiddhiAppCreationException(String.format("Invalid number of parameters [%s] for " +
                    "streamingml:bayesianRegression", attributeExpressionLength));
        }

        model = LinearRegressionModelHolder.getInstance().getLinearRegressionModel(modelName);

        if (model != null) {
            if (predictionSamples != -1) {
                model.setPredictionSamples(predictionSamples);
            }
            if (model.getNumFeatures() != -1) {
                // validate the model
                if (numberOfFeatures != model.getNumFeatures()) {
                    throw new SiddhiAppCreationException(String.format("Model [%s] expects %s features, but the " +
                                    "streamingml:bayesianRegression specifies %s features",
                            modelPrefix, model.getNumFeatures(), numberOfFeatures));
                }
            }
        } else {
            throw new SiddhiAppCreationException(String.format("Model [%s] needs to initialized "
                    + "prior to be used with streamingml:bayesianRegression. "
                    + "Perform streamingml:updateBayesianRegression process first.", modelName));

        }

        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("prediction", Attribute.Type.DOUBLE));
        attributes.add(new Attribute("confidence", Attribute.Type.DOUBLE));

        return attributes;
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {

        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                StreamEvent event = streamEventChunk.next();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Event received; Model name: %s Event:%s", modelName, event));
                }

                double[] features = new double[numberOfFeatures];
                for (int i = 0; i < numberOfFeatures; i++) {
                    // attributes cannot ever be any other type than double as we've validated the query at init
                    features[i] = ((Number) featureVariableExpressionExecutors.get(i).execute(event)).doubleValue();
                }

                Object[] data = LinearRegressionModelHolder.getInstance().getLinearRegressionModel(modelName)
                        .predictWithStd(features);
                // If output has values, then add those values to output stream
                complexEventPopulater.populateComplexEvent(event, data);
            }
        }
        nextProcessor.process(streamEventChunk);

    }


    /**
     * This will be called only once and this can be used to acquire
     * required resources for the processing element.
     * This will be called after initializing the system and before
     * starting to process the events.
     */
    @Override
    public void start() {

    }

    /**
     * This will be called only once and this can be used to release
     * the acquired resources for processing.
     * This will be called before shutting down the system.
     */
    @Override
    public void stop() {

    }

    /**
     * Used to collect the serializable state of the processing element, that need to be.
     * persisted for reconstructing the element to the same state on a different point of time
     *
     * @return stateful objects of the processing element as an map
     */
    @Override
    public Map<String, Object> currentState() {
        return null;
    }

    /**
     * Used to restore serialized state of the processing element, for reconstructing
     * the element to the same state as if was on a previous point of time.
     *
     * @param state the stateful objects of the processing element as a map.
     *              This is the same map that is created upon calling currentState() method.
     */
    @Override
    public void restoreState(Map<String, Object> state) {

    }
}
