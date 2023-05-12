fun f(a: integer): integer = g(5)
	{ where fun g(b: integer): integer = h(a)
		{ where fun h(c: integer): integer = a }
	}
