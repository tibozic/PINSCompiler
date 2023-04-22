TEST_NUM="07"
TEST_IN_DIR="AST"
TEST_RUN="$TEST_IN_DIR"
#TEST_RUN="TYP"
# -ea enables assertions
RUN_ARGS="-ea"

java $RUN_ARGS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS src/tests/$TEST_IN_DIR/test$TEST_NUM.tst --dump $TEST_RUN --exec $TEST_RUN
