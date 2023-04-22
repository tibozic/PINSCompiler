TEST_PART="FRM"
RUN_PARAMS="-ea"
DIFF_PARAMS=""

TEST_RUN="$TEST_PART"
TEST_OUT="$TEST_PART"
TEST_IN="$TEST_PART"
TESTS_PATH="src/tests"
TESTS_OUT_PATH="$TESTS_PATH/$TEST_OUT"
TEST_FILES="$TESTS_PATH/$TEST_IN/*.tst"

RESULT_FILE_EXT="new"

echo "**** Testing ****"
for testFile in $TEST_FILES
do
	# echo $testFile
	echo "----- Testing file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	java $RUN_PARAMS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS $testFile --dump $TEST_RUN --exec $TEST_RUN &> "$TESTS_OUT_PATH/test$testNum.$RESULT_FILE_EXT"
done

TEST_RESULTS="$TESTS_OUT_PATH/*.out"

echo "**** Results ****"
for testFile in $TEST_RESULTS
do
	echo "----- Results of file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	testNum=${testFile//[^0-9]/}
	diff $DIFF_PARAMS $testFile "$TESTS_OUT_PATH/test$testNum.new"
done
