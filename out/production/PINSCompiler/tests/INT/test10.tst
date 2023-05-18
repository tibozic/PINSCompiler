fun fib(n:integer) : integer = (
    {
        if n <= 1 then
            { result = n }
        else(
            {result = fib(n-1)},
            {result = result + fib(n-2) }
        )
    },
    result
) { where var result : integer };

# fun main(argc : integer) : integer = print_int(fib(6))
