fun main(_: integer): integer = (
	{ x = 14 },
	print_int(x),
	{ y = dodajTri(x) },
	print_int(y),
	manjseOdDvajset(y),
	0
) { where var x: integer; var y: integer };

fun dodajTri(a: integer): integer = (
	{tri = 3},
	a + tri
) { where var tri: integer };

fun manjseOdDvajset(b: integer): logical = (
	print_str('Zacenjam primerjavo'),
	{ temp = primerjalnik(20) },
	print_str('Koncujem primerjavo'),
	temp
) { where var temp: logical;
		fun primerjalnik(dvajset: integer): logical = (
			{ if b < dvajset then (print_str('Je manjse'), { result = true })
				else (print_str('Ni manjse'), { result = false })
			},
			result
		) { where var result: logical }
	}

