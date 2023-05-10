TEST_PART="IMC"
RUN_PARAMS="-ea"
DIFF_PARAMS=""
RUN_ALL_TESTS="false"

TEST_RUN="$TEST_PART"
TEST_OUT="$TEST_PART"
TEST_IN="$TEST_PART"
#TEST_IN="TYP"
TESTS_PATH="src/tests"
TESTS_OUT_PATH="$TESTS_PATH/$TEST_OUT"
TEST_FILES="$TESTS_PATH/$TEST_IN/*.tst"

RESULT_FILE_EXT="new"

test_pass_counter=0
test_counter=0

if [[ "$RUN_ALL_TESTS" == "true" ]]
then
	for part in "$TESTS_PATH"/*;
	do
		# get the part
		arrIN=(${part//// })
		pins_part="${arrIN[2]}"

		if [[ $pins_part == "SYN" ]]
		then
			continue
		fi

		echo "**** Testing $part ****"
		for testFile in "$part"/*.tst;
		do
			echo "----- Testing file: $testFile -----"
			test_counter=$((test_counter+1))
			testNum=${testFile//[^0-9]/}
			java $RUN_PARAMS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS "$testFile" --dump "$pins_part" --exec "$pins_part" &> "$part/test$testNum.$RESULT_FILE_EXT"
		done

		# TEST_RESULTS="$part*.out"

		echo "**** Results for $part ****"
		for testFile in "$part"/*.out;
		do
			echo "----- Results of file: $testFile -----"
			testNum=${testFile//[^0-9]/}
			diff $DIFF_PARAMS "$testFile" "$part/test$testNum.new"
			if [[ $? -eq 0 ]]
			then
				test_pass_counter=$((test_pass_counter+1))
			fi
		done
	done
else
	echo "**** Testing ****"
	for testFile in $TEST_FILES;
	do
		# echo $testFile
		echo "----- Testing file: $testFile -----"
		test_counter=$((test_counter+1))
		testNum=${testFile//[^0-9]/}
		java $RUN_PARAMS -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS $testFile --dump $TEST_RUN --exec $TEST_RUN &> "$TESTS_OUT_PATH/test$testNum.$RESULT_FILE_EXT"
	done

	TEST_RESULTS="$TESTS_OUT_PATH/*.out"

	echo "**** Results ****"
	for testFile in $TEST_RESULTS;
	do
		echo "----- Results of file: $testFile -----"
		testNum=${testFile//[^0-9]/}
		testNum=${testFile//[^0-9]/}
		diff $DIFF_PARAMS $testFile "$TESTS_OUT_PATH/test$testNum.new"
		if [[ $? -eq 0 ]]
		then
			test_pass_counter=$((test_pass_counter+1))
		fi
	done
fi

echo "--- Results ---"
echo "$test_pass_counter/$test_counter passed!"
echo "---------------"
