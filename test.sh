TEST_PART="TYP"
RUN_PARAMS="-ea"
DIFF_PARAMS=""
ALL="false"

TEST_RUN="$TEST_PART"
TEST_OUT="$TEST_PART"
TEST_IN="$TEST_PART"
TESTS_PATH="src/tests"
TESTS_OUT_PATH="$TESTS_PATH/$TEST_OUT"
TEST_FILES="$TESTS_PATH/$TEST_IN/*.tst"

RESULT_FILE_EXT="new"

test_failed_counter=0
test_counter=0

if [ "$ALL" = "true" ]
then
	for part in $TESTS_PATH
	do
		echo "**** Testing $part ****"
		for testFile in $TEST_FILES
		do
			# echo $testFile
			echo "----- Testing file: $testFile -----"
			test_counter=$((test_counter+1))
			testNum=${testFile//[^0-9]/}
			java $RUN_PARAMS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS $testFile --dump $part --exec $part &> "$TESTS_PATH/$part/test$testNum.$RESULT_FILE_EXT"
		done

		TEST_RESULTS="$TESTS_PATH/$part/*.out"

		echo "**** Results for $part ****"
		for testFile in $TEST_RESULTS
		do
			echo "----- Results of file: $testFile -----"
			testNum=${testFile//[^0-9]/}
			testNum=${testFile//[^0-9]/}
			diff $DIFF_PARAMS $testFile "$TESTS_OUT_PATH/test$testNum.new"
			if [ $? -ne 0 ] 
			then
				test_failed_counter=$((test_failed_counter+1))
			fi
		done
	done
else
	echo "**** Testing ****"
	for testFile in $TEST_FILES
	do
		# echo $testFile
		echo "----- Testing file: $testFile -----"
		test_counter=$((test_counter+1))
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
		if [ $? -ne 0 ] 
		then
			test_failed_counter=$((test_failed_counter+1))
		fi
	done
fi

tests_passed=$((test_counter-test_failed_counter))
echo "--- Results ---"
echo "$tests_passed/$test_counter passed!"
echo "---------------"
