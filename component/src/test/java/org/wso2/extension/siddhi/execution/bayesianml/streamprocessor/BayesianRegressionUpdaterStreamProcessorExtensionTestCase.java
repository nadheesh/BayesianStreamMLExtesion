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
import org.wso2.siddhi.core.exception.SiddhiAppCreationException;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.util.SiddhiTestHelper;

import java.util.concurrent.atomic.AtomicInteger;


public class BayesianRegressionUpdaterStreamProcessorExtensionTestCase {
    private static final Logger logger = Logger.getLogger(
            BayesianRegressionUpdaterStreamProcessorExtensionTestCase.class);

    private AtomicInteger count;


    @BeforeMethod
    public void init() {
        count = new AtomicInteger(0);
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension1() throws InterruptedException {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - All params in");

        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1', "
                + "attribute_4, 1, 'adam', 0.01, attribute_0, attribute_1, attribute_2, attribute_3) \n"
                + "insert all events into outputStream;");

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            try {

                InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamA");
                siddhiAppRuntime.start();
                inputHandler.send(new Object[]{0.1, 0.8, 0.1, 0.2, 0.03});
                inputHandler.send(new Object[]{0.2, 0.95, 0.42, 0.22, 0.1});
                inputHandler.send(new Object[]{0.8, 0.10, 0.8, 0.65, 0.92});
                inputHandler.send(new Object[]{0.75, 0.1, 0.43, 0.58, 0.71});

                SiddhiTestHelper.waitForEvents(200, 4, count, 5000);

            } catch (Exception e) {
                logger.error(e.getCause().getMessage());
                AssertJUnit.fail("Model fails to train with all the params");
            } finally {
                siddhiAppRuntime.shutdown();
            }

        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model fails initialize with all the params");
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension2() throws InterruptedException {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Only learning rate in");

        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1', "
                + "attribute_4, 0.01, attribute_0, attribute_1, attribute_2, attribute_3) \n"
                + "insert all events into outputStream;");

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);

            try {
                InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamA");
                siddhiAppRuntime.start();
                inputHandler.send(new Object[]{0.1, 0.8, 0.1, 0.2, 0.03});
                inputHandler.send(new Object[]{0.2, 0.95, 0.42, 0.22, 0.1});
                inputHandler.send(new Object[]{0.8, 0.1, 0.8, 0.65, 0.92});
                inputHandler.send(new Object[]{0.75, 0.1, 0.4, 0.58, 0.71});

                SiddhiTestHelper.waitForEvents(200, 4, count, 5000);
            } catch (Exception e) {
                logger.error(e.getCause().getMessage());
                AssertJUnit.fail("Model fails to train with all the params");
            } finally {
                siddhiAppRuntime.shutdown();
            }

        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model fails initialize with all the params");
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension21() throws InterruptedException {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - all parameters default");

        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1', "
                + "attribute_4, attribute_0, attribute_1, attribute_2, attribute_3) \n"
                + "insert all events into outputStream;");

        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);

            try {
                InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamA");
                siddhiAppRuntime.start();
                inputHandler.send(new Object[]{0.1, 0.8, 0.1, 0.2, 0.53});
                inputHandler.send(new Object[]{0.2, 0.95, 0.42, 0.22, 0.1});
                inputHandler.send(new Object[]{0.8, 0.1, 0.8, 0.65, 0.92});
                inputHandler.send(new Object[]{0.75, 0.1, 0.4, 0.58, 0.21});

                SiddhiTestHelper.waitForEvents(200, 4, count, 5000);
            } catch (Exception e) {
                logger.error(e.getCause().getMessage());
                AssertJUnit.fail("Model fails to train with all the params");
            } finally {
                siddhiAppRuntime.shutdown();
            }

        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model fails initialize with all the params");
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension5() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Not implemented optimizer given");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1'," +
                "attribute_4, 'adaam', attribute_0, attribute_1, attribute_2, attribute_3) " +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("model.optimizer should be one of " +
                    "[ADAM, RMSPROP, ADAGRAD, SGD, NADAM]. But found adaam"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension8() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Negative learning rate given");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1'," +
                "attribute_4, -0.01, attribute_0, attribute_1, attribute_2, attribute_3) " +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("learning.rate should be greater than zero. " +
                    "But found -0.010000"));
        }
    }


//    //    @Test
//    public void testBayesianRegressionStreamProcessorExtension2() throws InterruptedException, FileNotFoundException,
//            UnsupportedEncodingException {
//        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Assert updated weights");
//
//        SiddhiManager siddhiManager = new SiddhiManager();
//
//        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
//                "double, attribute_3 double, attribute_4 double );";
//
//        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1', " +
//                "attribute_4, 0.05, attribute_0, attribute_1, attribute_2, attribute_3) \n" + "insert all
// events into" +
//                " outputStream;");
//
//        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
//        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
//
//            @Override
//            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
//                count.incrementAndGet();
//                EventPrinter.print(inEvents);
//                if (count.get() == 1) {
//                    AssertJUnit.assertArrayEquals(new Object[]{0.1, 0.8, 0.2, 0.03, "true", 0.001, 0.008, 0.002,
//                            3.0E-4}, inEvents[0].getData());
//                }
//                if (count.get() == 3) {
//                    AssertJUnit.assertArrayEquals(new Object[]{0.8, 0.1, 0.65, 0.92, "false", 0.003, 0.0175,
//                            0.004200000000000001, 0.0013}, inEvents[0].getData());
//                }
//            }
//        });
//
//        Scanner scanner = null;
//        try {
//            File file = new File("src/test/resources/BayesianRegression.csv");
//            InputStream inputStream = new FileInputStream(file);
//            Reader fileReader = new InputStreamReader(inputStream, "UTF-8");
//            BufferedReader bufferedReader = new BufferedReader(fileReader);
//            scanner = new Scanner(bufferedReader);
//
//            InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamA");
//            siddhiAppRuntime.start();
//            while (scanner.hasNext()) {
//                String eventStr = scanner.nextLine();
//                String[] event = eventStr.split(",");
//                inputHandler.send(new Object[]{Double.valueOf(event[0]), Double.valueOf(event[1]), Double.valueOf
//                        (event[2]), Double.valueOf(event[3]), event[4]});
//                try {
//                    Thread.sleep(1);
//                } catch (InterruptedException e) {
//                    logger.error(e.getCause().getMessage());
//                }
//            }
//            SiddhiTestHelper.waitForEvents(200, 8, count, 60000);
//        } finally {
//            if (scanner != null) {
//                scanner.close();
//            }
//            siddhiAppRuntime.shutdown();
//        }
//    }
//
//
//    //    @Test
//    public void testBayesianRegressionStreamProcessorExtension3() throws InterruptedException {
//        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Label in the middle");
//        SiddhiManager siddhiManager = new SiddhiManager();
//
//        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
//                "double, attribute_3 double, attribute_4 double );";
//        String query = ("@info(name = 'query1') " +
//                "from StreamA#streamingml:updateBayesianRegression('model1',attribute_2, " +
//                "0.01, attribute_0, attribute_1, attribute_3, attribute_4) \n" +
//                "insert all events into outputStream;");
//
//        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
//        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
//
//            @Override
//            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
//                count.incrementAndGet();
//                EventPrinter.print(inEvents);
//                if (count.get() == 3) {
//                    AssertJUnit.assertArrayEquals(new Object[]{0.8, 0.1, "false", 0.65, 0.92, 0.003, 0.0175,
//                            0.004200000000000001, 0.0013}, inEvents[0]
//                            .getData());
//                }
//            }
//        });
//        try {
//            InputHandler inputHandler = siddhiAppRuntime.getInputHandler("StreamA");
//            siddhiAppRuntime.start();
//            inputHandler.send(new Object[]{0.1, 0.8, "true", 0.2, 0.03});
//            inputHandler.send(new Object[]{0.2, 0.95, "true", 0.22, 0.1});
//            inputHandler.send(new Object[]{0.8, 0.1, "false", 0.65, 0.92});
//            inputHandler.send(new Object[]{0.75, 0.1, "false", 0.58, 0.71});
//
//            SiddhiTestHelper.waitForEvents(200, 4, count, 60000);
//        } finally {
//            siddhiAppRuntime.shutdown();
//        }
//    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension3() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Features are not of type double");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 bool, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1', "
                + "attribute_4, 0.01, attribute_0, attribute_1, attribute_2, attribute_3) \n"
                + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("model.features in 7th parameter is not"
                    + " a numerical type attribute. Found BOOL. Check the input stream definition"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension4() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Label is not of type double");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double, attribute_4 string );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1'," +
                "attribute_4,0.01, attribute_0, attribute_1, attribute_2, attribute_3)" +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("[model.target] attribute_4 in "
                    + "updateBayesianRegression should be a double. But found STRING"));
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension6() throws InterruptedException {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - Label type is integer");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 double, attribute_4 int );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('model1'," +
                "attribute_4,0.01, attribute_0, attribute_1, attribute_2, attribute_3)" +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("[model.target] attribute_4 in "
                    + "updateBayesianRegression should be a double. But found INT"));
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension9() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - invalid model name");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 int, attribute_4 string );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression(attribute_4,"
                + "attribute_4, attribute_0, attribute_1, attribute_2, attribute_3)"
                + "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Parameter model.name must be a "
                    + "constant but found org.wso2.siddhi.core.executor.VariableExpressionExecutor"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension10() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - incorrect initialization");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 string );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression() \n" +
                "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition +
                    query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters [0] for "
                    + "streamingml:updateBayesianRegression"));
        }
    }


    @Test
    public void testBayesianRegressionStreamProcessorExtension12() {

        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - invalid model name type");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 string );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression(0.2,attribute_4, " +
                "attribute_0, attribute_1, attribute_2, attribute_3)" +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid parameter type found for the "
                    + "model.name argument, required STRING but found DOUBLE"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension13() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - incorrect order of parameters");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('m1',attribute_4,"
                + "1.0, attribute_0, attribute_1, attribute_2, 2)\n"
                + "insert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("7th parameter is not an attribute "
                    + "(VariableExpressionExecutor) present in the stream definition. Found a "
                    + "org.wso2.siddhi.core.executor.ConstantExpressionExecutor"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension14() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - more parameters than needed");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('m1',attribute_4," +
                "10, 'adam', 1.0, attribute_0, attribute_1, attribute_2, attribute_3, 2)" +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("Invalid number of parameters for " +
                    "streamingml:updateBayesianRegression. This Stream Processor requires at most 9 parameters, " +
                    "namely, model.name, model.target, model.samples[optional], model.optimizer[optional], " +
                    "learning.rate[optional], model.features. but found 10 parameters"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension15() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - model.target is not an attribute");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 " +
                "double, attribute_3 int, attribute_4 double );";
        String query = ("@info(name = 'query1') from StreamA#streamingml:updateBayesianRegression('m1',2," +
                "1.0, attribute_0, attribute_1, attribute_2, attribute_3)" +
                "\ninsert all events into outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
            AssertJUnit.fail();
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.assertTrue(e instanceof SiddhiAppCreationException);
            AssertJUnit.assertTrue(e.getCause().getMessage().contains("model.target attribute in "
                    + "updateBayesianRegression should be a variable, but found a "
                    + "org.wso2.siddhi.core.executor.ConstantExpressionExecutor"));
        }
    }

    @Test
    public void testBayesianRegressionStreamProcessorExtension19() {
        logger.info("BayesianRegressionUpdaterStreamProcessorExtension TestCase - model is visible only within the " +
                "SiddhiApp");
        SiddhiManager siddhiManager = new SiddhiManager();

        String trainingStream = "@App:name('BayesianRegressionTestApp1') \n"
                + "define stream StreamTrain (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double, attribute_4 double );";
        String trainingQuery = ("@info(name = 'query-train') from " +
                "StreamTrain#streamingml:updateBayesianRegression" + "('model1', attribute_4, 0.1, attribute_0, " +
                "attribute_1, attribute_2, attribute_3) \n" + "insert all events into trainOutputStream;\n");

        String inStreamDefinition = "@App:name('BayesianRegressionTestApp2') \n"
                + "define stream StreamA (attribute_0 double, attribute_1 double, attribute_2 "
                + "double, attribute_3 double );";
        String query = ("@info(name = 'query1') from "
                + "StreamA#streamingml:updateBayesianRegression('model1', attribute_3, 0.1, attribute_0, " +
                "attribute_1, attribute_2) \n" + "insert all events into " + "outputStream;");
        try {
            SiddhiAppRuntime siddhiAppRuntime1 = siddhiManager.createSiddhiAppRuntime(trainingStream + trainingQuery);
            // should be successful even though both the apps are using the same model name with different feature
            // values
            SiddhiAppRuntime siddhiAppRuntime2 = siddhiManager.createSiddhiAppRuntime(inStreamDefinition + query);
        } catch (Exception e) {
            logger.error(e.getCause().getMessage());
            AssertJUnit.fail("Model is visible across Siddhi Apps which is wrong!");
        }
    }
}
