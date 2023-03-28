#!/bin/python3

profTest="profTest.tst"

def main():
    original_tests = open(profTest, "r")
    test_counter = 50
    writing_test = False
    writing_result = False

    for line in original_tests.readlines():
        if( "!code" in line ):
            writing_test = True
            test_file = open(("test" + str(test_counter) + ".tst"), "w")
            continue

        if( "!expected" in line ):
            writing_test = False
            test_file.close()
            writing_result = True
            result_file = open(("test" + str(test_counter) + ".out"), "w")
            continue

        if( "!end" in line ):
            writing_result = False
            result_file.close()
            test_counter += 1
            continue

        if( writing_test ):
            test_file.write(line)

        if( writing_result ):
            result_file.write(line)

    original_tests.close()
            
    
main()
