package org.wso2.extension.siddhi.execution.bayesianml.streamprocessor;

import org.apache.log4j.Logger;
import org.wso2.extension.siddhi.execution.bayesianml.model.BaseModel;
import org.wso2.extension.siddhi.execution.bayesianml.model.BayesianModelHolder;
import org.wso2.extension.siddhi.execution.bayesianml.model.LinearRegression;
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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Bayesian regression.
 */

@Extension(
        name = "updateBayesianRegression",
        namespace = "streamingml",
        description = "This extension builds/updates a linear Bayesian regression model.",
        parameters = {
                @Parameter(
                        name = "model.name",
                        description = "The name of the model to be built/updated.",
                        type = {DataType.STRING}
                ),
                @Parameter(
                        name = "model.target",
                        description = "The target attribute (dependant variable) of the dataset",
                        type = {DataType.DOUBLE, DataType.INT}
                ),
                @Parameter(
                        name = "model.samples",
                        description = "Number of samples used to construct the gradients",
                        type = {DataType.INT}, optional = true, defaultValue = "1"
                ),
                @Parameter(
                        name = "model.optimizer",
                        description = "The type of optimization used",
                        type = {DataType.STRING}, optional = true, defaultValue = "ADAM"
                ),
                @Parameter(
                        name = "learning.rate",
                        description = "The learning rate of the updater",
                        type = {DataType.DOUBLE}, optional = true, defaultValue = "0.05"
                ),
                @Parameter(
                        name = "model.features",
                        description = "Features of the model that need to be attributes of the stream.",
                        type = {DataType.DOUBLE} // TODO add int
                )
        },
        returnAttributes = {
                @ReturnAttribute(name = "featureWeight", description = "Weight of the <feature" +
                        ".name> of the " + "model.", type = {DataType.DOUBLE}),
                @ReturnAttribute(name = "featureConfidence", description = "Weight of the <feature" +
                        ".name> of the " + "model.", type = {DataType.DOUBLE})
        },
        examples = {
                @Example(syntax = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 double," +
                        " attribute_3 double, attribute_4 double );\n\n" +
                        "from StreamA#streamingml:updateBayesianRegression('model1', attribute_4, 0.01, " +
                        "attribute_0, attribute_1, attribute_2, attribute_3) \n" +
                        "insert all events into outputStream;",
                        description = "This query builds/updates a Bayesian Linear regression model " +
                                "named `model1` with a `0.01` learning rate using `attribute_0`, `attribute_1`, " +
                                "`attribute_2`, and `attribute_3` as features, and `attribute_4` as the label. " +
                                "Updated weights of the model are emitted to the OutputStream stream.")

        }

)
public class BayesianRegressionUpdaterStreamProcessorExtension extends StreamProcessor {

    private static Logger logger = Logger.getLogger(BayesianRegressionUpdaterStreamProcessorExtension.class);
    private String modelName;
    private int numberOfFeatures;
    private VariableExpressionExecutor targetVariableExpressionExecutor;
    private List<VariableExpressionExecutor> featureVariableExpressionExecutors = new ArrayList<>();


    /**
     * The initialization method for {@link StreamProcessor}, which will be called before other methods and validate
     * the all configuration and getting the initial values.
     *
     * @param attributeExpressionExecutors are the executors of each attributes in the Function
     * @param siddhiAppContext             Siddhi app runtime context
     */
    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition,
                                   ExpressionExecutor[] attributeExpressionExecutors, ConfigReader configReader,
                                   SiddhiAppContext siddhiAppContext) {

        String siddhiAppName = siddhiAppContext.getName();
        BaseModel model;
        String modelPrefix;

        double learningRate = -1;
        int nSamples = -1;
        BaseModel.OptimizerType opimizerName = null;

        // maxNumberOfFeatures = number of attributes - label attribute TODO why model.name is not considered ?
        int maxNumberOfFeatures = inputDefinition.getAttributeList().size() - 1;

        if (attributeExpressionLength >= 3) {
            if (attributeExpressionLength > 3 + maxNumberOfFeatures) {
                throw new SiddhiAppCreationException(String.format("Invalid number of parameters for " +
                        "streamingml:updateBayesianRegression. This Stream Processor requires at most %s " +
                        "parameters, namely, model.name, model.target, model.samples[optional], " +
                        "model.optimizer[optional], " + "learning.rate[optional], model.features but found %s " +
                        "parameters", 3 + maxNumberOfFeatures, attributeExpressionLength));
            }
            if (attributeExpressionExecutors[0] instanceof ConstantExpressionExecutor) {
                if (attributeExpressionExecutors[0].getReturnType() == Attribute.Type.STRING) {
                    modelPrefix = (String) ((ConstantExpressionExecutor) attributeExpressionExecutors[0]).getValue();
                    // model name = user given name + siddhi app name
                    modelName = modelPrefix + "." + siddhiAppName;
                } else {
                    throw new SiddhiAppCreationException("Invalid parameter type found for the model.name argument," +
                            " required " + Attribute.Type.STRING + " but found " + attributeExpressionExecutors[0].
                            getReturnType().toString());
                }
            } else {
                throw new SiddhiAppCreationException("Parameter model.name must be a constant" + " but found " +
                        attributeExpressionExecutors[0].getClass().getCanonicalName());
            }

            if (this.attributeExpressionExecutors[1] instanceof VariableExpressionExecutor) {
                targetVariableExpressionExecutor = (VariableExpressionExecutor) this.attributeExpressionExecutors[1];
                // label attribute should be double or integer
                Attribute.Type targetAttributeType = inputDefinition.getAttributeType(targetVariableExpressionExecutor
                        .getAttribute().getName());
                if (!CoreUtils.isNumeric(targetAttributeType)) {
                    throw new SiddhiAppCreationException(String.format("[model.target] %s in " +
                                    "updateBayesianRegression should be a numeric. But found %s",
                            targetVariableExpressionExecutor.getAttribute().getName(), targetAttributeType.name()));
                }
            } else {
                throw new SiddhiAppCreationException("model.target attribute in updateBayesianRegression should "
                        + "be a variable, but found a " + this.attributeExpressionExecutors[1].getClass()
                        .getCanonicalName());
            }

//            TODO why is the greater than zero check missing?
            int index = 2;
            // setting hyper parameters
            while (attributeExpressionExecutors[index] instanceof ConstantExpressionExecutor) {
                // number of samples from the gradient
                if (attributeExpressionExecutors[index].getReturnType() == Attribute.Type.INT && index == 2) {
                    int val = (int) ((ConstantExpressionExecutor) attributeExpressionExecutors[index])
                            .getValue();
                    if (val <= 0) {
                        throw new SiddhiAppCreationException(String.format("model.sample should be greater" +
                                " than zero." + "But found %d", val));
                    } else {
                        nSamples = val;
                        index += 1;
                    }
                } else if (attributeExpressionExecutors[index].getReturnType() == Attribute.Type.STRING && index <= 3
                        && opimizerName == null) {
                    // optimizer name
                    String val = (String) ((ConstantExpressionExecutor) attributeExpressionExecutors[index]).getValue();
                    try {
                        opimizerName = BaseModel.OptimizerType.valueOf(val.toUpperCase(Locale.ENGLISH));
                        index += 1;
                    } catch (Exception ex) {
                        throw new SiddhiAppCreationException(String.format("model.optimizer should be one of " +
                                "%s. But found %s", Arrays.toString(BaseModel.OptimizerType.values()), val));
                    }
                } else if (attributeExpressionExecutors[index].getReturnType() == Attribute.Type.DOUBLE) {
                    // learning rate
                    double val = (double) ((ConstantExpressionExecutor) attributeExpressionExecutors[index])
                            .getValue();
                    if (val <= 0) {
                        throw new SiddhiAppCreationException(String.format("learning.rate should " +
                                "be greater than zero. " + "But found %f", val));
                    } else {
                        learningRate = val;
                        index += 1;
                        break;
                    }
                } else {
                    throw new SiddhiAppCreationException(String.format("Invalid parameter type found. " +
                                    "Expected: %s or %s or %s. But found %s",
                            Attribute.Type.INT, Attribute.Type.STRING, Attribute.Type.DOUBLE,
                            attributeExpressionExecutors[2].getReturnType().toString()));
                }
                // set number of features
                numberOfFeatures = attributeExpressionLength - index;
                // feature values
                featureVariableExpressionExecutors = CoreUtils.extractAndValidateFeatures(inputDefinition,
                        attributeExpressionExecutors, index, numberOfFeatures);


            }
            if (index == 2) {
                if (attributeExpressionExecutors[2] instanceof VariableExpressionExecutor) {
                    // set number of features
                    numberOfFeatures = attributeExpressionLength - 2;
                    // feature values
                    featureVariableExpressionExecutors = CoreUtils.extractAndValidateFeatures(inputDefinition,
                            attributeExpressionExecutors, 2, numberOfFeatures);
                } else {
                    throw new SiddhiAppCreationException("3rd Parameter must either be a constant (hyperparameter) or "
                            + "an attribute of the stream (model" + ".features), but found a " +
                            attributeExpressionExecutors[2].getClass().getCanonicalName());
                }
            }
        } else {
            throw new SiddhiAppCreationException(String.format("Invalid number of parameters [%s] for " +
                    "streamingml:updateBayesianRegression", attributeExpressionLength));
        }

        // try to load existing model
        try {
            model = BayesianModelHolder.getInstance().getBayesianModel(modelName);
        } catch (Exception ex) {
            model = null;
        }

        // if no model exists, then create a new model
        if (model == null) {
            model = new LinearRegression();
            BayesianModelHolder.getInstance().addBayesianModel(modelName, model);
        }
        if (learningRate != -1) {
            model.setLearningRate(learningRate);
        }
        if (nSamples != -1) {
            model.setNumSamples(nSamples);
        }
        if (opimizerName != null) {
            model.setOptimizerType(opimizerName);
        }

        if (model.getNumFeatures() != -1) {
            // validate the model
            if (numberOfFeatures != model.getNumFeatures()) {
                throw new SiddhiAppCreationException(String.format("Model [%s] expects %s features, but the " +
                        "streamingml:updatePerceptronClassifier specifies %s features", modelPrefix, model
                        .getNumFeatures(), numberOfFeatures));
            }
        } else {
            model.initiateModel();
        }

        List<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < numberOfFeatures; i++) {
            attributes.add(new Attribute(featureVariableExpressionExecutors.get(i).getAttribute().getName() +
                    ".weight", Attribute.Type.DOUBLE));
        }
        return attributes;
    }

    /**
     * Process events received by BayesianRegressionUpdateStreamingProcessorExtension.
     *
     * @param streamEventChunk      the event chunk that need to be processed
     * @param nextProcessor         the next processor to which the success events need to be passed
     * @param streamEventCloner     helps to clone the incoming event for local storage or modification
     * @param complexEventPopulater helps to populate the events with the resultant attributes
     */
    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {

        synchronized (this) {
            while (streamEventChunk.hasNext()) {
                StreamEvent event = streamEventChunk.next();
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Event received; Model name: %s Event:%s", modelName, event));
                }

                double[] target = new double[]{(double) targetVariableExpressionExecutor.execute(event)};
                double[] features = new double[numberOfFeatures];
                for (int i = 0; i < numberOfFeatures; i++) {
                    // attributes cannot ever be any other type than int or double as we've validated the query at init
                    features[i] = (double) featureVariableExpressionExecutors.get(i).execute(event);
                }
                double[] weights = BayesianModelHolder.getInstance().
                        getBayesianModel(modelName).
                        update(features, target)[0];

                // convert weights to object[]
                Object[] data = new Object[weights.length];
                for (int i = 0; i < weights.length; i++) {
                    data[i] = weights[i];
                }

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
        BayesianModelHolder.getInstance().deleteBayesianModel(modelName);
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
        // TODO implements the state
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
