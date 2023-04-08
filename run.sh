TEST_NUM="02"
TEST_PART="TYP"

java -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS src/tests/$TEST_PART/test$TEST_NUM.tst --dump $TEST_PART --exec $TEST_PART
