TEST_NUM="33"
TEST_IN_DIR="LEX"
#TEST_RUN="INT"
TEST_RUN="$TEST_IN_DIR"
# -ea enables assertions
RUN_ARGS="-ea"

java $RUN_ARGS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS src/tests/$TEST_IN_DIR/test$TEST_NUM.tst --dump $TEST_RUN --exec $TEST_RUN
