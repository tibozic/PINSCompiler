typ i:integer;
var j:string;
fun prestejCrke (i:integer, j:string): integer = {
    while (i < j):
        {
            for i = (j + 5), i < 100, i + 1:
                {
                    if i == 69 then print(i)
                }
        }
};

fun print(a: integer): integer = 1