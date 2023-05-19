fun main(_: integer): integer = (
	{ x = 14 },
	print_int(x),
	{ y = dodajTri(x) },
	print_int(y)
) { where var x: integer; var y: integer };

fun dodajTri(a: integer): integer = (
	{tri = 3},
	a + tri
) { where var tri: integer }
