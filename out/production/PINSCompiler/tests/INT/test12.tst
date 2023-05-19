fun main(_: integer): integer = pozdrav(18);

fun pozdrav(ura: integer): integer = (
	{ if ura >= 12 then
			{ sporocilo = 'Dober dan' }
		else
			{ sporocilo = 'Dobro jutro' }
	},
		print_str(sporocilo),
		0
) { where var sporocilo: string }
