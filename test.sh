
TESTS="src/tests/*.tst"

echo "**** Testing ****"
for testFile in $TESTS
do
	# echo $testFile
	echo "----- Testing file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	java -classpath /home/betterjimmy/Documents/Projects/software/java/PINSCompiler/out/production/PINSCompiler:/home/betterjimmy/Documents/Projects/software/java/PINSCompiler/lib/ArgPar-0.1.jar Main PINS $testFile --dump LEX > "src/tests/test$testNum.new"
done

TESTS="src/tests/*.out"

echo "**** Results ****"
for testFile in $TESTS
do
	echo "----- Results of file: $testFile -----"
	testNum=${testFile//[^0-9]/}
	testNum=${testFile//[^0-9]/}
	diff $testFile "src/tests/test$testNum.new"
done
