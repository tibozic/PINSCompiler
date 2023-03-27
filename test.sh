
TEST_PART="NAME"
TESTS_PATH="src/tests/$TEST_PART"
TEST_FILES="$TESTS_PATH/*.tst"

echo "**** Testing ****"
for testFile in $TEST_FILES
do
	# echo $testFile
	echo "----- Testing file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	java -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS $testFile --dump $TEST_PART --exec $TEST_PART &> "$TESTS_PATH/test$testNum.new"
done

TEST_RESULTS="$TESTS_PATH/*.out"

echo "**** Results ****"
for testFile in $TEST_RESULTS
do
	echo "----- Results of file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	testNum=${testFile//[^0-9]/}
	diff $testFile "$TESTS_PATH/test$testNum.new"
done
