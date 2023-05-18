fun main(_: integer): integer = isTen(9);

fun isTen(a: integer): integer = (
	{ if a == 10 then
			print_str('Stevilo je enako 10')
		else
			print_str('Stevilo ni enako 10')
	},
	0
)
