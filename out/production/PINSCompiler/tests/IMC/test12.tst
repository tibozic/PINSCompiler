fun f(i: integer, b: logical): integer = f(g(i+1), true)
	{ where fun g(c: integer): integer = c }
