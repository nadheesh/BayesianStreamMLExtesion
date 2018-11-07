/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.execution.bayesianml.streamprocessor;

import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.EventPrinter;
import org.wso2.siddhi.core.util.SiddhiTestHelper;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class BayesianClassificationStreamProcessorExtensionTestCase {

    private static final Logger logger = Logger.getLogger(BayesianClassificationStreamProcessorExtensionTestCase.class);
    private AtomicInteger count;
    private String trainingStream = "@App:name('BayesianClassificationTestApp') " +
            "\ndefine stream StreamTrain (attribute_0 double, " +
            "attribute_1 double, attribute_2  double, attribute_3 double, attribute_4 string );";

    private String trainingQuery = ("@info(name = 'query-train') from " +
            "StreamTrain#streamingml:updateBayesianClassification" + "('ml', 3, attribute_4, 'nadam', 0.01, " +
            "attribute_0, attribute_1, attribute_2, attribute_3) \n" +
            "insert all events into trainOutputStream;\n");


    @BeforeMethod
    public void init() {
        count = new AtomicInteger(0);
    }


    // TODO we can't ensure that this will pass all the time due to the randomness
//    @Test
    public void testBayesianClassificationStreamProcessorExtension1() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase " +
                "- Assert predictions and evolution");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, " +
                "attribute_2 double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " +
                " attribute_0, attribute_1, attribute_2, attribute_3) " +
                "select attribute_0, attribute_1, attribute_2, attribute_3, prediction, confidence " +
                "insert into outputStream;");

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream + inStreamDefinition
                    + trainingQuery + query);
            siddhiAppRuntime.addCallback("query1", new QueryCallback() {

                @Override
                public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                    count.incrementAndGet();
                    EventPrinter.print(inEvents);
                    if (count.get() == 1) {
                        AssertJUnit.assertArrayEquals(new Object[]{5.1, 3.8, 1.6, 0.2, "setosa"},
                                Arrays.copyOf(inEvents[0].getData(), 5));
                    } else if (count.get() == 2) {
                        AssertJUnit.assertArrayEquals(new Object[]{6.8, 3, 5.5, 2.1, "virginica"},
                                Arrays.copyOf(inEvents[0].getData(), 5));
                    } else if (count.get() == 3) {
                        AssertJUnit.assertArrayEquals(new Object[]{5.7, 2.5, 5, 2, "versicolor"},
                                Arrays.copyOf(inEvents[0].getData(), 5));
                    }
                }
            });
            try {
                InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamTrain");
                siddhiAppRuntime.start();

                for (int i = 0; i < 10; i++) {


                    inputHandler.send(new Object[]{5.4, 3.4, 1.7, 0.2, "setosa"});
                    inputHandler.send(new Object[]{6.9, 3.1, 5.4, 2.1, "virginica"});
                    inputHandler.send(new Object[]{4.3, 3, 1.1, 0.1, "setosa"});
                    inputHandler.send(new Object[]{4.3, 3, 1.1, 0.1, "setosa"});
                    inputHandler.send(new Object[]{6, 2.2, 4, 1, "versicolor"});
                    inputHandler.send(new Object[]{6.1, 2.8, 4.7, 1.2, "versicolor"});
                    inputHandler.send(new Object[]{4.9, 3, 1.4, 0.2, "setosa"});
                    inputHandler.send(new Object[]{5.5, 2.5, 4, 1.3, "versicolor"});
                    inputHandler.send(new Object[]{5.4, 3.9, 1.3, 0.4, "setosa"});
                    inputHandler.send(new Object[]{6.8, 2.8, 4.8, 1.4, "versicolor"});
                    inputHandler.send(new Object[]{6.4, 3.1, 5.5, 1.8, "virginica"});
                    inputHandler.send(new Object[]{6.5, 2.8, 4.6, 1.5, "versicolor"});
                    inputHandler.send(new Object[]{4.8, 3.4, 1.9, 0.2, "setosa"});
                }

                Thread.sleep(1100);

                InputHandler inputHandler1 = siddhiAppRuntime.getInputHandler("StreamA");
                // send some unseen data for prediction
                inputHandler1.send(new Object[]{5.1, 3.8, 1.6, 0.2});
                inputHandler1.send(new Object[]{6.8, 3, 5.5, 2.1});
                inputHandler1.send(new Object[]{5.7, 2.5, 5, 2});

                SiddhiTestHelper.waitForEvents(200, 3, count, 60000);
            } catch (Exception e) {
                logger.error(e.getCause().getMessage());
                AssertJUnit.fail("Model fails build");

            } finally {
                siddhiAppRuntime.shutdown();
            }
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model fails build");
        }

    }


    @Test
    public void testBayesianClassificationStreamProcessorExtension2() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - Features are not of type numeric");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 bool );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " + "10," +
                " attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("model.features in 6th parameter is not "
                    + "a numerical type attribute. Found BOOL. Check the input stream definition"));
        }
    }


    @Test
    public void testBayesianClassificationStreamProcessorExtension3() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - " +
                "Number of prediction samples is not int ");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 bool);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " + "0.5, " +
                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for the " +
                    "prediction.samples argument. Expected: INT but found: DOUBLE"));
        }
    }


    @Test
    public void testBayesianClassificationStreamProcessorExtension4() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - Number of samples less than 1");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " + "0," +
                " attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter value found for the " +
                    "prediction.samples argument. Expected a value greater than zero, but found: 0"));
        }
    }

//    @Test
//    public void testBayesianClassificationStreamProcessorExtension51() {
//        logger.info("BayesianClassificationStreamProcessorExtension TestCase - model name is not string");
//        SiddhiManager siddhiManager = new SiddhiManager();
//
//        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
//                "double, attribute_3 double);";
//        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification(34," +
//                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
//        try {
//            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
//            AssertJUnit.fail();
//        } catch (Exception e) {
//            logger.error(e.getCause().getMessage());
//            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
//            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for " +
//                    "the model.name argument, required STRING but found INT"));
//        }
//    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension5() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - invalid model name");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double, attribute_4 string );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification(attribute_4, "
                + "1000, attribute_0, attribute_1, attribute_2, attribute_3) \n"
                + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Parameter model.name must be a constant but "
                    + "found org.wso2.siddhi.core.executor.VariableExpressionExecutor"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension6() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - incorrect initialization");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification() \n" + "insert all " +
                "events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters [0] for " +
                    "streamingml:bayesianClassification"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension7() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - Incompatible model");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " + "100, " +
                "attribute_0, attribute_1, attribute_2) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream +
                    inStreamDefinition + trainingQuery + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Model [ml] expects 4 features, but the " +
                    "streamingml:bayesianClassification specifies 3 features"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension8() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - invalid model name type");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification(1000, " +
                "attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail("Model fails build");
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for the " +
                    "model.name argument, required STRING but found INT"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension9() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - incorrect order of parameters");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('m1', " +
                "attribute_0, attribute_1, attribute_2, attribute_3, 2000) \n" +
                "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail("Model fails build");
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("6th parameter is not an attribute "
                    + "(VariableExpressionExecutor) present in the stream definition. Found a "
                    + "org.wso2.siddhi.core.executor.ConstantExpressionExecutor"
            ));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension10() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - more parameters than needed");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('m1', " + "10000, " +
                "attribute_0, attribute_1, attribute_2, attribute_3, 2) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail("Model fails build");
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters for " +
                    "streamingml:bayesianClassification. This Stream Processor requires at most 6 parameters, " +
                    "namely, model.name, prediction.samples[optional], model.features but found 7 parameters"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension11() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - init predict first and then "
                + "update model");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', attribute_0, "
                + "attribute_1, attribute_2, attribute_3) \n" + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(trainingStream
                    + inStreamDefinition + query + trainingQuery);
            AssertJUnit.fail("Model fails build");
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Model [ml.BayesianClassificationTestApp] "
                    + "needs to initialized prior to be used with streamingml:bayesianClassification. Perform "
                    + "streamingml:updateBayesianClassification process first"));
        }
    }

    @Test
    public void testBayesianClassificationStreamProcessorExtension12() {
        logger.info("BayesianClassificationStreamProcessorExtension TestCase - model is visible only within the " +
                "SiddhiApp");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "@App:name('BayesianClassificationTestApp2') \ndefine stream StreamA " +
                "(attribute_0 double, " +
                "attribute_1 double, attribute_2 double, attribute_3 double);";
        String query = ("@info(name = 'query1') from StreamA#streamingml:bayesianClassification('ml', " +
                "1000, attribute_0, attribute_1, attribute_2, attribute_3) \n" +
                "insert all events into " + "outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime1 = siddhiManager.createSiddhiAppRuntime(trainingStream + trainingQuery);
            // should be successful even though both the apps are using the same model name with different feature
            // values
            SiddhiAppRuntime siddhiAppRuntime2 = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail("Model fails build");
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e.getCause().getMessage().contains(
                    "Model [ml.BayesianClassificationTestApp2] needs to initialized prior to be " +
                            "used with streamingml:bayesianClassification. " +
                            "Perform streamingml:updateBayesianClassification process first."));
        }
    }


}
