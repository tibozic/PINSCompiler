####### ULTIMATE TEST PINS 2k69 ######

# Inicializacija
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

# Funkcije
# function_definition
fun firstfunction(x: integer, y: string, z: logical): integer =
	( {for i = 1, 100, {i = i + 1}:
		{
			while j > (-10): {
				if j * (-10) + 20 + 30 + 10 - 40 / 50 % 100 -(-(-(-(-(+10))))) != 123 then
				    12
				else
					firstfunction(123, 'bruh', true)
			}
		# komentar komentar komentar ----------------------------------------------
		}	},
		true & false | z & true | (1 >= firstfunction(i, 'monke', true)),
		{
			if 16 > (14 - 10 + 10 * 10 / 10 - 10 + 10 + -10)
				then
					firstfunction(i, 'danes je lep dan', false)
		},

		{
		for i = 1, 10, 12 : (
			{
				while j<10: (j < 10)
			},
			{
			    if j * 10 >= 100 then {a = j - 10 == 12} else 0
			},
			{ 
                for j = 1, 12, {j=j*2}: firstfunction(12, 'blablabla', true) 
            },
			{ 
                p[i+10-20*30][j-10-203] = 12
            })
		},
        12
	)
{ where 
    var i:integer; 
    var a: logical; 
    var j: integer;
    var p: arr[69] arr[420] integer;
    fun secondfunction(x: integer):integer = 
        (
            (10+20) >= (30-40) & 12 == 100, 0) 
        { where
            fun thirdfunction(y: string):arr[10] string = 
                (
                    {10*20=xyz}, 
                    ss
                ) 
                { where 
                    var xyz: integer; 
                    var ss : arr[10] string
                }
    };
    fun partition(stevila: arr[10] integer, begin: integer, end: integer): integer = 
        (
	        { pivot = stevila[end] },
	        { i = (begin - 1) },
            { 
                for j = begin, end, {j = j + 1}:
		            { 
                        if stevila[j] <= pivot then
			                (
                                { i = i + 1 },
			                    { swapTemp = stevila[j] },
			                    { stevila[i] = stevila[j] },
			                    { stevila[j] = swapTemp }
                            )         
		            } 
            },
            {swapTemp = stevila[i+1]},
            {stevila[i+1] = stevila[end]},
            {stevila[end] = swapTemp},
            (i + 1)
        )
        { where
            var swapTemp: integer;
            var pivot: integer;
            var i: integer;
            var j: integer
    };
    fun izpis(besedilo: string): void =
        (
            print('Tvoj program je prestal celoten preizkus!'),
            true 
        );

    fun print(besedilo: string): string = besedilo
}
