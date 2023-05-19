fun main( x : integer ) : integer = ({z = 0}, g(12))
	{ where var z : integer;
	fun g ( c : integer) : integer = e(c + 3)
		{ where fun e ( d : integer) : integer = print_int(z) }
	}
