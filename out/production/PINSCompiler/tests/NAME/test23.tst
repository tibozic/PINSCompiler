####### ultimate test pins 2k69 ######

# inicializacija
# type_definition
typ x: integer;
typ y: string;
typ z: logical;
typ i: blablabla;
typ j: arr[10] arr[15] arr[20] blablabla;
typ blablabla: integer;
typ blabla: blablabla;
typ void: logical;

# variable_definition
var k: string; var l: integer; var q: logical; var m: blabla; var n: arr[69] arr [420] string;

# funkcije
# function_definition
fun firstfunction(x: integer, y: string, z: logical, n: arr[69] arr[420] string): integer =
	( {for i = 1, i <= 100, {i = i + 1}:
		{
			while j > (-10): {
				if j * (-10) + 20 + 30 + 10 - 40 / 50 % 100 -(-(-(-(-(+10))))) != 123 then
				    12
				else
					firstfunction(123, -234, +456)
			}
		# komentar komentar komentar ----------------------------------------------
		}	},

		true & false | 10 | 'danes je lep dan''' & i | firstfunction(i, x + 10)
											>=
		+10 - -20 * !true - !false / (j, 'ha', 'ne', 'da') - {x=10} + {y+x-z = 5 * true - false}
		| i > j+3 | j < j-2 | i >= i*3 |  i <= j/7 | j != j & 'a' & 'b' & 'a' / 'a' == 1 + 10 + 20 + 30 / 40 / 50 / 60
		* 10 * 20 * 30 * 40 - 50 - 60 - 80 - 90  % 4 % 6 % 7 % 8 +++++++10,

		{
			if 'abc' > 'abc' - 10 + 10 * 10 / 10 - 10 + 10 + -10
				then
					firstfunction('danes je lep dan')
		},

		{
		for i =
			{
				while j: (j < 10)
			},
				{
				  if a * 10 >= 100 then {a = a - 10} else 0
				},
					{ for j = 1, j < 10, {j=j*2}: firstfunction('blablabla') }:
						{a[i+10-20*30][j-10-203] = 12}
		}
	)
{ where var i:integer; var a: logical; var j: integer; fun secondfunction(x: integer):integer = 10+20|30-40&123==20 { where
fun thirdfunction(y: string):arr[10] string = ({10*20=xyz}) {where var xyz: integer}}
};

fun partition(stevila: arr[10] integer, begin: integer, end: integer): integer = (
	{pivot = stevila[end]},
	{i = (begin - 1)},

	{ for j = begin, j < end, {j = j + 1}:
		{ if stevila[j] <= pivot then
			({i = i + 1},
			{swaptemp = stevila[j]},
			{stevila[i] = stevila[j]},
			{stevila[j] = swaptemp})
		}
	},

	({swaptemp = stevila[i+1]},
	{stevila[i+1] = stevila[end]},
	{stevila[end] = swaptemp}),

	(i + 1)
) {where
    var swaptemp: integer;
    var pivot: integer;
    var i: integer;
    var j: integer
};

fun izpis(besedilo: string): void =
	print('tvoj program je prestal celoten preizkus!');

fun print(besedilo: string): string = besedilo
