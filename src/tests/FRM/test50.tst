fun main(args:arr[10]int):int = (
    # program, ki vrne sestevek fibonaccijevih stevil na mestih v args tabeli
    { sum = 0 },
    { for i = 0, 10, 1 :
        { sum = sum + fib(args[i]) }
    } { where var i:int },
    sum
) { where
    var sum:int
};

fun fib(n:int):int = (
    # funkcija, ki vrne n-to fibonaccijevo stevilo
    { return = 0 },
    { if n == 1 | n == 2 then
        {return = 1}
    else (
        { return = fib(n-1) + fib(n-2) }
    )
    },
    return
) { where
    var return:int
};

typ int:integer;
typ str:string;
typ bool:logical

# konec
